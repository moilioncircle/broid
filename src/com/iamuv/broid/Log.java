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
package com.iamuv.broid;

import android.text.TextUtils;

/**
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about Log.java">uv@iamuv.com</a> <br>
 *         2014-6-5
 * 
 */
public class Log {

    private Log() {}

    public static void w(String tag, String msg, Throwable tr) {
	if (TextUtils.isEmpty(msg)) {
	    android.util.Log.w(tag, android.util.Log.getStackTraceString(tr));
	} else if (tr == null) {
	    android.util.Log.w(tag, msg);
	} else
	    android.util.Log.w(tag, msg, tr);
    }

    public static void w(Object msg) {
	if (msg instanceof Throwable) {
	    w(Broid.getPackageName(), null, (Throwable) msg);
	} else
	    w(Broid.getPackageName(), String.valueOf(msg), null);
    }

    public static void e(String tag, String msg, Throwable tr) {
	if (TextUtils.isEmpty(msg)) {
	    android.util.Log.e(tag, android.util.Log.getStackTraceString(tr));
	} else if (tr == null) {
	    android.util.Log.e(tag, msg);
	} else
	    android.util.Log.e(tag, msg, tr);
    }

    public static void e(Object msg) {
	if (msg instanceof Throwable) {
	    e(Broid.getPackageName(), null, (Throwable) msg);
	} else
	    e(Broid.getPackageName(), String.valueOf(msg), null);
    }

    public static void i(String tag, String msg, Throwable tr) {
	if (TextUtils.isEmpty(msg)) {
	    android.util.Log.i(tag, android.util.Log.getStackTraceString(tr));
	} else if (tr == null) {
	    android.util.Log.i(tag, msg);
	} else
	    android.util.Log.i(tag, msg, tr);
    }

    public static void i(Object msg) {
	if (msg instanceof Throwable) {
	    i(Broid.getPackageName(), null, (Throwable) msg);
	} else
	    i(Broid.getPackageName(), String.valueOf(msg), null);
    }

    public static void d(String tag, String msg, Throwable tr) {
	if (Broid.getDebugMode()) {
	    if (TextUtils.isEmpty(msg)) {
		android.util.Log.d(tag, android.util.Log.getStackTraceString(tr));
	    } else if (tr == null) {
		android.util.Log.d(tag, msg);
	    } else
		android.util.Log.d(tag, msg, tr);
	}
    }

    public static void d(Object msg) {
	if (msg instanceof Throwable) {
	    d(getTag(), null, (Throwable) msg);
	} else
	    d(getTag(), String.valueOf(msg), null);
    }

    private static String getTag() {
	String tag = null;
	for (StackTraceElement st : Thread.currentThread().getStackTrace()) {
	    try {
		if (!st.isNativeMethod() && !st.getClassName().equals(Log.class.getName())
		    && !st.getClassName().equals(Thread.class.getName()) && !TextUtils.isEmpty(st.getClassName())
		    && !TextUtils.isEmpty(st.getMethodName())) {
		    tag = new StringBuilder(st.getClassName()).append('.').append(st.getMethodName()).append("(at ")
			.append(st.getFileName()).append(':').append(st.getLineNumber()).append(')').toString();
		    break;
		}
	    } catch (Exception e) {}
	}
	if (TextUtils.isEmpty(tag))
	    tag = Broid.getPackageName();
	return tag;
    }
}
