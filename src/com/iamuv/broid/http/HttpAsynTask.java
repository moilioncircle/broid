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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.utils.SystemUtils;

public class HttpAsynTask {

	private final ThreadFactory mThreadFactory;

	private final ExecutorService mExecutorService;

	public HttpAsynTask() {
		mThreadFactory = new HttpThreadFactory();
		int size = SystemUtils.getDefaultThreadPoolSize(8);
		Log.i(Broid.TAG, "the http core pool size is " + size, null);
		mExecutorService = new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
				mThreadFactory);

	}

	public final  Http submit(HttpRequest request, HttpCallback<?> callback) {
		final Http http = new Http(request, callback);
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
