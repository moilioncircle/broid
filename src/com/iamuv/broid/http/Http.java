package com.iamuv.broid.http;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.Process;

import com.iamuv.broid.Broid;
import com.iamuv.broid.http.HttpWorker.Status;

/**
 * Http短时异步请求实例
 * 
 * @author Uv
 * @date 2014-6-3
 * @email uv@iamuv.com
 * @blog http://www.iamuv.com
 */
public final class Http<T> {

	private final HttpFutureTask mHttpFutureTask;

	private UUID mID;

	private Class<T> mType;

	@SuppressWarnings("unchecked")
	Http(T httpRequest, HttpCallback<?> callback) {
		final HttpWorker worker = new HttpWorker(new RequestEntry(httpRequest), callback);
		mHttpFutureTask = new HttpFutureTask(worker);
		mID = UUID.randomUUID();
		mType = ((Class<T>) httpRequest.getClass());
	}

	public void cancel() {
		mHttpFutureTask.cancel();
		mHttpFutureTask.getHttpWorker().getHttpCallback().cancel();
	}

	public void interrupt() {
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

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof Http) {
			return ((Http<T>) o).getID().equals(this.getID());
		} else
			return false;
	}

	public final Status getStatus() {
		return mHttpFutureTask.getHttpWorker().getStatus();
	}

	public Class<T> getType() {
		return mType;
	}

}

class HttpFutureTask extends FutureTask<String> {

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

}

class HttpWorker implements Callable<String> {

	private HttpConn mConn;

	private RequestEntry mEntry;

	private String mResult;

	private boolean isCancelled;

	private HttpCache mHttpCache;

	private HttpCallback<?> mCallback;

	private Status mStatus;

	private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;

	private Scheduling mScheduling;

	HttpWorker(RequestEntry entry, HttpCallback<?> httpCallback) {
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

	@Override
	public String call() {
		mStatus = Status.RUNNING;
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		mScheduledThreadPoolExecutor.scheduleAtFixedRate(mScheduling, Broid.getHttpTimeLimit(), 1L, TimeUnit.SECONDS);
		try {
			if (!isCancelled) {
				String cache = mHttpCache.get(mEntry);
				if (cache == null) {
					if (!isCancelled)
						mConn = new HttpConn(mEntry);
					if (!isCancelled)
						mResult = mConn.exec();
					if (isCancelled)
						mHttpCache.save(mEntry, mResult);
				} else {
					if (!isCancelled)
						mResult = cache;
				}
			}
		} catch (Exception e) {
			if (!isCancelled && mCallback != null)
				mCallback.error(e);
			mStatus = Status.FINISHED;
			return mResult;
		}
		if (!isCancelled && mCallback != null)
			mCallback.complete(mResult);
		mStatus = Status.FINISHED;
		return mResult;
	}

	public enum Status {
		PENDING, RUNNING, FINISHED
	}

	class Scheduling implements Runnable {
		@Override
		public void run() {
			if (mStatus == Status.RUNNING) {
				mCallback.error(new TimeoutException());
				cancel();
			}
		}
	}

}
