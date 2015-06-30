/*
 * Copyright (C) 2014 The Broid Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iamuv.broid.http;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.http.Http.Status;
import com.iamuv.broid.http.HttpCallback.Part;
import com.umeng.analytics.MobclickAgent;

/**
 * Http短时异步请求实例
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about Http.java">uv@iamuv.com</a> <br>
 *         2014-6-5
 * 
 * @param <T>
 */
public class Http {

	private final HttpFutureTask mHttpFutureTask;

	private UUID mID;

	Http(HttpRequest request, HttpCallback<?> callback) {
		final HttpWorker worker = new HttpWorker(new HttpRequestEntry(request), callback);
		mHttpFutureTask = new HttpFutureTask(worker);
		mID = UUID.randomUUID();
	}

	public void cancel() {
		mHttpFutureTask.cancel();
		mHttpFutureTask.getHttpWorker().getHttpCallback().cancel();
	}

	public void interrupt() {
		mHttpFutureTask.interrupt();
		mHttpFutureTask.cancel(true);
		mHttpFutureTask.getHttpWorker().getHttpCallback().cancel();
	}

	public final String getID() {
		return mID.toString();
	}

	final HttpFutureTask getTask() {
		return mHttpFutureTask;
	}

	public final HttpCallback<?> getCallback() {
		return mHttpFutureTask.getHttpWorker().getHttpCallback();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Http) {
			return ((Http) o).getID().equals(this.getID());
		} else
			return false;
	}

	public final Status getStatus() {
		return mHttpFutureTask.getHttpWorker().getStatus();
	}

	public final boolean isFinished() {
		return getStatus() == Status.FINISHED;
	}

	public enum Status {
		PENDING, RUNNING, FINISHED
	}

}

class HttpFutureTask extends FutureTask<HttpResult> {

	private HttpWorker mWorker;

	public HttpFutureTask(HttpWorker worker) {
		super(worker);
		mWorker = worker;
	}

	HttpWorker getHttpWorker() {
		return mWorker;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		cancel();
		return super.cancel(mayInterruptIfRunning);
	}

	public void cancel() {
		mWorker.cancel();
	}

	public void interrupt() {
		mWorker.interrupt();
	}

}

class HttpWorker implements Callable<HttpResult> {

	private HttpConn mConn;

	private HttpRequestEntry mEntry;

	private HttpResult mResult;

	private boolean isCancelled;

	private HttpCache mHttpCache;

	private HttpCallback<?> mCallback;

	private Status mStatus;

	private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;

	private Scheduling mScheduling;

	private HashMap<String, String> mEventMap = new HashMap<String, String>();

	private long mStartTimeMillis;

	HttpWorker(HttpRequestEntry entry, HttpCallback<?> httpCallback) {
		mStartTimeMillis = System.currentTimeMillis();
		mStatus = Status.PENDING;
		mEntry = entry;
		isCancelled = false;
		mHttpCache = new HttpCache();
		mResult = null;
		mCallback = httpCallback;
		mScheduling = new Scheduling();
		mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		mScheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		mScheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		mEventMap.put("url", mEntry.getUrl());
		mEventMap.put("method", mEntry.getMode().name());
		mEventMap.put("network type", getNetworkType());
	}

	private final String getNetworkType() {
		NetworkInfo info = Broid.getConnectivityManager().getActiveNetworkInfo();
		if (info == null)
			return "no net";
		if (info.getType() == ConnectivityManager.TYPE_MOBILE)
			return "mobile";
		if (info.getType() == ConnectivityManager.TYPE_WIFI)
			return "wifi";
		return "unkown type : " + info.getType();
	}

	final Status getStatus() {
		return mStatus;
	}

	HttpCallback<?> getHttpCallback() {
		return mCallback;
	}

	void cancel() {
		this.isCancelled = true;
		mStatus = Status.FINISHED;
		mScheduledThreadPoolExecutor.shutdown();
	}

	void interrupt() {
		mEntry.interrupt();
		this.isCancelled = true;
		mStatus = Status.FINISHED;
		mScheduledThreadPoolExecutor.shutdown();
	}

	@Override
	public HttpResult call() {
		mStatus = Status.RUNNING;
		try {
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
			mScheduledThreadPoolExecutor.schedule(mScheduling, Broid.getHttpTimeLimit(), TimeUnit.MILLISECONDS);
			ConnectivityManager connectivityManager = (ConnectivityManager) Broid.getApplication().getSystemService(
					Context.CONNECTIVITY_SERVICE);
			if (!connectivityManager.getActiveNetworkInfo().isAvailable())
				throw new NetworkErrorException();
		} catch (Exception e) {
			if (!isCancelled && mCallback != null)
				mCallback.error(Part.PREPARE, e);
			mScheduledThreadPoolExecutor.shutdownNow();
			mStatus = Status.FINISHED;
			return mResult;
		}
		try {
			if (!isCancelled) {
				HttpResult cache = mEntry.isCache() ? mHttpCache.get(mEntry) : null;
				if (cache == null) {
					mEventMap.put("cache", "no");
					Log.i(Broid.TAG, "do not use http cache", null);
					if (!isCancelled)
						mConn = new HttpConn(mEntry);
					if (!isCancelled)
						mResult = mConn.exec();
					mHttpCache.save(mEntry, mResult);
				} else {
					mEventMap.put("cache", "yes");
					Log.i(Broid.TAG, "use http cache", null);
					if (!isCancelled)
						mResult = cache;
				}
				if (!isCancelled && mCallback != null)
					mCallback.complete(mResult);
			}
			mEventMap.put("error", "no");
		} catch (Exception e) {
			mEventMap.put("error", "yes");
			if (!isCancelled && mCallback != null)
				mCallback.error(Part.REQUEST, e);
			return mResult;
		} finally {
			mScheduledThreadPoolExecutor.shutdownNow();
			mStatus = Status.FINISHED;
		}
		MobclickAgent.onEvent(Broid.getApplication(), "broid_http", mEventMap);
		MobclickAgent.onEventValue(Broid.getApplication(), "broid_http_time", mEventMap,
				(int) (System.currentTimeMillis() - mStartTimeMillis));
		return mResult;
	}

	class Scheduling implements Runnable {
		@Override
		public void run() {
			if (mStatus == Status.RUNNING) {
				mEventMap.put("error", "yes");
				Log.w(Broid.TAG, "http thread do not finish in " + Broid.getHttpTimeLimit() / 1000 + " seconds, it will cancel", null);
				interrupt();
				mCallback.error(Part.REQUEST, new TimeoutException());
			}
		}
	}

}
