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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.iamuv.broid.http.Http;
import com.iamuv.broid.http.HttpAsynTask;
import com.iamuv.broid.http.HttpCallback;
import com.iamuv.broid.http.HttpRequest;
import com.iamuv.broid.storage.DaoFactory;
import com.iamuv.broid.storage.PreferencesDao;
import com.iamuv.broid.storage.SQLiteDao;

/**
 * 
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about Broid.java">uv@iamuv.com</a> <br>
 *         2014-6-10
 *
 */
public class Broid {

	public static final String TAG = "Broid";

	private static Application mAppInstance;
	private static boolean mDebugMode;
	private static DaoFactory mDaoFactory;
	private static HttpAsynTask mHttpAsynTask;
	private static long mHttpTimeLimit;
	private static String mPackageName;
	private static ConcurrentHashMap<String, Integer> mRequestCodeCache;
	private static final AtomicInteger mRequestCode = new AtomicInteger(100);
	private static String mDeviceInfo;
	private static ConnectivityManager mConnectivityManager;

	private Broid() {}

	public static final void onCreate(Application application) {
		mAppInstance = application;
		try {
			PackageManager packageManager = mAppInstance.getPackageManager();
			mPackageName = mAppInstance.getPackageName();

			ApplicationInfo info = packageManager.getApplicationInfo(mPackageName, PackageManager.GET_META_DATA);
			if (info != null && info.metaData != null) {
				mDebugMode = info.metaData.getBoolean("Broid debug", true);
				mHttpTimeLimit = info.metaData.getLong("Broid http time limit", 60 * 1000);
			} else
				throw new Exception();
		} catch (Exception e) {
			mDebugMode = true;
			mHttpTimeLimit = 60 * 1000;
		}
		if (mDebugMode)
			android.util.Log
					.d(TAG, "Broid debug mode is true\r\n" + "if you do not want to log the debug message\r\n"
							+ "you can add the following code in your manifest\r\n"
							+ "<meta-data android:name=\"debug\" android:value=\"false\"/>");
		else
			android.util.Log.d(TAG, "Broid debug mode is false");
		mDeviceInfo = getDeviceInfo(mAppInstance);
		if (!TextUtils.isEmpty(mDeviceInfo))
			android.util.Log.i(TAG, mDeviceInfo);
		mConnectivityManager = (ConnectivityManager) Broid.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public static final Application getApplication() {
		if (mAppInstance == null) {
			throw new IllegalArgumentException(
					"Broid Framework cannot find Application, please add the code 'Broid.onCreate(this)' in Application onCreate method");
		}
		return mAppInstance;
	}

	public static final SharedPreferences getConfig() {
		return getApplication().getSharedPreferences("broid_framework_config", 0);
	}

	public static final boolean getDebugMode() {
		return mDebugMode;
	}

	public static final long getHttpTimeLimit() {
		return mHttpTimeLimit;
	}

	public static final String getPackageName() {
		return mPackageName;
	}

	public static final <T> SQLiteDao<T> getSQLiteDao(Class<T> type) {
		if (mDaoFactory == null)
			mDaoFactory = new DaoFactory();
		return mDaoFactory.getSQLiteDao(type);
	}

	public static final <T> PreferencesDao<T> getPreferencesDao(Class<T> type) {
		if (mDaoFactory == null)
			mDaoFactory = new DaoFactory();
		return mDaoFactory.getPreferencesDao(type);
	}

	public static final Http http(HttpRequest request, HttpCallback<?> callback) {
		if (mHttpAsynTask == null)
			mHttpAsynTask = new HttpAsynTask();
		return mHttpAsynTask.submit(request, callback);
	}

	public static final Http http(HttpRequest request) {
		if (mHttpAsynTask == null)
			mHttpAsynTask = new HttpAsynTask();
		return mHttpAsynTask.submit(request, null);
	}

	public static final int getRequestCode(String className) {
		if (mRequestCodeCache == null)
			mRequestCodeCache = new ConcurrentHashMap<String, Integer>();
		if (mRequestCodeCache.get(className) == null)
			mRequestCodeCache.put(className, mRequestCode.getAndIncrement());
		return mRequestCodeCache.get(className);
	}

	public static final String getRequestClassName(int requestCode) {
		if (mRequestCodeCache == null) {
			mRequestCodeCache = new ConcurrentHashMap<String, Integer>();
			return null;
		}
		Iterator<Entry<String, Integer>> iterator = mRequestCodeCache.entrySet().iterator();
		Entry<String, Integer> entry;
		while (iterator.hasNext()) {
			entry = iterator.next();
			if (entry.getValue() == requestCode) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static ConnectivityManager getConnectivityManager() {
		return mConnectivityManager;
	}

	private static String getDeviceInfo(Context context) {
		try {
			org.json.JSONObject json = new org.json.JSONObject();
			android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String device_id = tm.getDeviceId();
			android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			String mac = wifi.getConnectionInfo().getMacAddress();
			json.put("mac", mac);
			if (TextUtils.isEmpty(device_id)) {
				device_id = mac;
			}
			if (TextUtils.isEmpty(device_id)) {
				device_id = android.provider.Settings.Secure.getString(context.getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);
			}
			json.put("device_id", device_id);
			return json.toString();
		} catch (Exception e) {
			android.util.Log.w(TAG, "get device info fail", e);
		}
		return null;
	}
}
