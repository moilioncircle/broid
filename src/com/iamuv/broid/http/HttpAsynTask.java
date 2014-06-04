package com.iamuv.broid.http;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;

public class HttpAsynTask {

	private final ThreadFactory mThreadFactory;

	private final ExecutorService mExecutorService;

	private static final int CORE_POOL_SIZE = (int) Math.pow(Broid.getCPUCount() + 1, 2);

	public HttpAsynTask() {
		mThreadFactory = new HttpThreadFactory();
		Log.i(Broid.TAG, "the http core pool size is " + CORE_POOL_SIZE, null);
		mExecutorService = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(), mThreadFactory);

	}

	public final <T> Http<T> submit(T httpRequest, HttpCallback<?> callback) {
		final Http<T> http = new Http<T>(httpRequest, callback);
		mExecutorService.execute(http.getTask());
		return http;
	}

	class HttpThreadFactory implements ThreadFactory {

		private final AtomicInteger mCount = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "http thread #" + mCount.getAndIncrement());
			Log.d(Broid.TAG, thread.getName() + " create", null);
			return thread;
		}
	}

}
