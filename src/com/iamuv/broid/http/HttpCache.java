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

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;

public class HttpCache {

	private static Object lock = new Object();

	private static SQLiteOpenHelper mSqLiteOpenHelper;

	private static ConcurrentHashMap<String, WeakReference<Cache>> MEMORY_CACHE;

	private static MemoryInfo mMemoryInfo;

	private static ActivityManager mActivityManager;

	private volatile Cache mCache;

	public HttpCache() {
		init();
	}

	private static void init() {
		if (mSqLiteOpenHelper == null) {
			mSqLiteOpenHelper = new SQLiteOpenHelper(Broid.getApplication(), "broid_cache.sqlite", null, 1) {

				@Override
				public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

				@Override
				public void onCreate(SQLiteDatabase db) {
					db.execSQL("CREATE TABLE IF NOT EXISTS broid_http_cache ( id TEXT PRIMARY KEY, result BLOB, cookie BLOB, time long, size long )");
					db.execSQL("CREATE INDEX IF NOT EXISTS broid_http_cache_index ON broid_http_cache ( id, time, size )");
				}
			};
		}

		if (MEMORY_CACHE == null) {
			MEMORY_CACHE = new ConcurrentHashMap<String, WeakReference<Cache>>();
		}

		if (mMemoryInfo == null) {
			mMemoryInfo = new MemoryInfo();
			mActivityManager = (ActivityManager) Broid.getApplication().getSystemService("activity");
		}

	}

	public static void clear() {
		init();
		MEMORY_CACHE.clear();
		synchronized (lock) {
			SQLiteDatabase database = mSqLiteOpenHelper.getWritableDatabase();
			try {
				database.beginTransaction();
				database.rawQuery("delete from broid_http_cache", null);
				database.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				database.endTransaction();
				if (database != null && database.isOpen())
					database.close();
			}
		}
	}

	public HttpResult get(HttpRequestEntry entry) {
		if (entry == null)
			return null;
		if (!entry.isCache())
			return null;
		WeakReference<Cache> cacheRef = MEMORY_CACHE.get(entry.getId());
		if (cacheRef != null) {
			mCache = cacheRef.get();
			if (mCache != null) {
				if (mCache.time < System.currentTimeMillis()) {
					MEMORY_CACHE.remove(entry.getId());
				} else {
					return new HttpResult(mCache.result, mCache.cookie);
				}
			} else
				MEMORY_CACHE.remove(entry.getId());
		}
		synchronized (lock) {
			SQLiteDatabase database = mSqLiteOpenHelper.getReadableDatabase();
			try {
				database.beginTransaction();
				database.delete("broid_http_cache", "time < ?", new String[] { String.valueOf(System.currentTimeMillis()) });
				Cursor cursor = database.query("broid_http_cache", null, "id = ?", new String[] { String.valueOf(entry.getId()) }, null,
						null, null);
				try {
					if (cursor.moveToFirst()) {
						byte[] result = cursor.getBlob(cursor.getColumnIndex("result"));
						byte[] cookie = cursor.getBlob(cursor.getColumnIndex("cookie"));
						database.setTransactionSuccessful();
						return new HttpResult(result, cookie);
					}
				} catch (Exception e) {
					throw e;
				} finally {
					if (cursor != null && !cursor.isClosed())
						cursor.close();
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				database.endTransaction();
				if (database != null && database.isOpen())
					database.close();
			}
			return null;
		}
	}

	public void save(HttpRequestEntry entry, HttpResult result) {
		if (entry == null)
			return;
		mActivityManager.getMemoryInfo(mMemoryInfo);
		if (mMemoryInfo.lowMemory) {
			MEMORY_CACHE.clear();
			Log.w(Broid.TAG, "the system considers itself to currently be in a low memory situation, http memory cache clear", null);
		} else {
			mCache = new Cache();
			try {
				mCache.id = entry.getId();
				mCache.cookie = result.getCookieByte();
				mCache.result = result.getResultByte();
				if (entry.getSession() < 1) {
					throw new Exception("session is invalid");
				}
				mCache.time = System.currentTimeMillis() + entry.getSession() * 1000;
				mCache.size = mCache.cookie.length + mCache.result.length;
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
				return;
			}
			MEMORY_CACHE.put(mCache.id, new WeakReference<Cache>(mCache));
			Log.i(Broid.TAG, "http memory cache num is " + MEMORY_CACHE.size(), null);
		}
		synchronized (lock) {
			SQLiteDatabase database = mSqLiteOpenHelper.getWritableDatabase();
			try {
				database.beginTransaction();
				database.delete("broid_http_cache", "time < ?", new String[] { String.valueOf(System.currentTimeMillis()) });
				ContentValues values = new ContentValues();
				values.put("id", mCache.id);
				values.put("result", mCache.result);
				values.put("cookie", mCache.cookie);
				values.put("time", mCache.time);
				values.put("size", mCache.size);
				if (database.update("broid_http_cache", values, " id = ? ", new String[] { mCache.id }) < 1)
					database.insert("broid_http_cache", null, values);
				database.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				database.endTransaction();
				if (database != null && database.isOpen())
					database.close();
			}
		}
	}

	class Cache {
		String id;
		byte[] result;
		byte[] cookie;
		long time;
		long size;
	}

}
