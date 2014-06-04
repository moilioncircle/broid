package com.iamuv.broid;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.iamuv.broid.http.Http;
import com.iamuv.broid.http.HttpAsynTask;
import com.iamuv.broid.http.HttpCallback;
import com.iamuv.broid.storage.PreferencesStorage;
import com.iamuv.broid.storage.SQLiteStorage;
import com.iamuv.broid.storage.StorageFactory;

public class Broid {

	public static final String TAG = "Broid";

	private static Application mAppInstance;
	private static int mCPUCount;
	private static boolean mDebugMode;
	private static StorageFactory mStorageFactory;
	private static HttpAsynTask mHttpAsynTask;
	private static long mHttpTimeLimit;

	private Broid() {
	}

	public static final void onCreate(Application application) {
		mAppInstance = application;
		mCPUCount = Runtime.getRuntime().availableProcessors();
		try {
			ApplicationInfo info = mAppInstance.getPackageManager().getApplicationInfo(mAppInstance.getPackageName(),
					PackageManager.GET_META_DATA);
			if (info != null) {
				mDebugMode = info.metaData.getBoolean("debug", true);
				mHttpTimeLimit = info.metaData.getLong("http", 60);
			} else
				throw new Exception();
		} catch (Exception e) {
			mDebugMode = true;
			mHttpTimeLimit = 60;
		}
		if (mDebugMode) {
			android.util.Log.d(TAG, "Broid debug mode is true\r\n" + "if you do not want to log the debug message\r\n"
					+ "you can add the following code in your manifest\r\n"
					+ "<meta-data android:name=\"debug\" android:value=\"false\"/>");
		} else
			android.util.Log.d(TAG, "Broid debug mode is false");
	}

	public static Application getApplication() {
		if (mAppInstance == null) {
			throw new IllegalArgumentException(
					"Broid Framework cannot find Application, please add the code 'Broid.onCreate(this)' in Application onCreate method");
		}
		return mAppInstance;
	}

	public static final SharedPreferences getConfig() {
		return getApplication().getSharedPreferences("broid_framework_config", 0);
	}

	public static final int getCPUCount() {
		return mCPUCount;
	}

	public static final boolean getDebugMode() {
		return mDebugMode;
	}
	
	public static final long getHttpTimeLimit() {
		return mHttpTimeLimit;
	}

	/**
	 * 获取SQLite的实体类
	 * 
	 * @param type
	 *            数据库中表的映射类 此类需带有注解
	 *            {@linkplain com.iamuv.broid.storage.StorageEntry.SQLiteTable
	 *            SQLiteTable}
	 * @return {@linkplain com.iamuv.broid.storage.SQLiteStorage SQLiteStorage}
	 */
	public static final <T> SQLiteStorage<T> getSLDao(Class<T> type) {
		if (mStorageFactory == null)
			mStorageFactory = new StorageFactory();
		return mStorageFactory.getSQLiteStorage(type);
	}

	/**
	 * 获取SharedPreferences的实体类
	 * 
	 * @param type
	 *            配置文件的映射类 此类需带有注解
	 *            {@linkplain com.iamuv.broid.storage.StorageEntry.Preferences
	 *            Preference}
	 * @return {@linkplain com.iamuv.broid.storage.PreferencesStorage
	 *         PreferencesStorage}
	 */
	public static final <T> PreferencesStorage<T> getSPDao(Class<T> type) {
		if (mStorageFactory == null)
			mStorageFactory = new StorageFactory();
		return mStorageFactory.getPreferencesStorage(type);
	}

	/**
	 * http短时异步请求 如果该请求执行时间超过60秒 调度将会自动终止该线程
	 * 
	 * @param httpRequest
	 *            http请求映射类 此类需带有注解
	 *            {@linkplain com.iamuv.broid.http.RequestEntry.HttpRequest
	 *            HttpRequest}
	 * @param callback
	 *            extends {@linkplain com.iamuv.broid.http.HttpCallback
	 *            HttpCallback} 回调
	 * @return {@linkplain com.iamuv.broid.http.Http Http} 请求实例
	 */
	public static final <T> Http<T> http(T httpRequest, HttpCallback<?> callback) {
		if (mHttpAsynTask == null)
			mHttpAsynTask = new HttpAsynTask();
		return mHttpAsynTask.submit(httpRequest, callback);
	}

	/**
	 * http短时异步请求 如果该请求执行时间超过60秒 调度将会自动终止该线程
	 * 
	 * @param httpRequest
	 *            http请求映射类 此类需带有注解
	 *            {@linkplain com.iamuv.broid.http.RequestEntry.HttpRequest
	 *            HttpRequest}
	 * @return {@linkplain com.iamuv.broid.http.Http Http} 请求实例
	 */
	public static final <T> Http<T> http(T httpRequest) {
		if (mHttpAsynTask == null)
			mHttpAsynTask = new HttpAsynTask();
		return mHttpAsynTask.submit(httpRequest, null);
	}

}
