package com.iamuv.broid;

import android.text.TextUtils;

public class Log {

	private static final String TAG = "broid";

	private Log() {
	}

	public static void w(String tag, String msg, Throwable tr) {
		if (TextUtils.isEmpty(msg)) {
			android.util.Log.w(tag, android.util.Log.getStackTraceString(tr));
		} else if (tr == null) {
			android.util.Log.w(tag, msg);
		} else
			android.util.Log.w(tag, msg, tr);
	}

	public static void w(Throwable tr) {
		w(getTag(), null, tr);
	}

	public static void w(String msg) {
		w(getTag(), msg, null);
	}

	public static void e(String tag, String msg, Throwable tr) {
		if (TextUtils.isEmpty(msg)) {
			android.util.Log.e(tag, android.util.Log.getStackTraceString(tr));
		} else if (tr == null) {
			android.util.Log.e(tag, msg);
		} else
			android.util.Log.e(tag, msg, tr);
	}

	public static void e(Throwable tr) {
		e(getTag(), null, tr);
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
		i(getTag(), String.valueOf(msg), null);
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
		d(getTag(), String.valueOf(msg), null);
	}

	private static String getTag() {
		String tag = null;
		for (StackTraceElement st : Thread.currentThread().getStackTrace()) {
			try {
				if (!st.isNativeMethod() && !st.getClassName().equals(Log.class.getName())
						&& !st.getClassName().equals(Thread.class.getName()) && !TextUtils.isEmpty(st.getClassName())
						&& !TextUtils.isEmpty(st.getMethodName())) {
					tag = st.getClassName() + "." + st.getMethodName() + "(at " + st.getFileName() + ":" + st.getLineNumber()
							+ ")";
					break;
				}
			} catch (Exception e) {
			}
		}
		if (TextUtils.isEmpty(tag))
			tag = TAG;
		return tag;
	}
}
