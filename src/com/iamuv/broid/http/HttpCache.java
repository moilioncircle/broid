package com.iamuv.broid.http;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.Utils;

public class HttpCache {

	private static Object lock = new Object();

	private static SQLiteOpenHelper mSqLiteOpenHelper;

	public HttpCache() {
		init();
	}

	private static void init() {

		if (mSqLiteOpenHelper == null) {
			mSqLiteOpenHelper = new SQLiteOpenHelper(Broid.getApplication(), "broid_cache.sqlite", null, 1) {

				@Override
				public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				}

				@Override
				public void onCreate(SQLiteDatabase db) {
					db.execSQL("CREATE TABLE IF NOT EXISTS http_cache ( id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, params TEXT, cache TEXT, md5 TEXT, time long )");
					db.execSQL("CREATE INDEX IF NOT EXISTS http_cache_index ON http_cache ( url,params, cache, time)");
				}
			};
		}

	}

	public static void clear() {
		init();
		synchronized (lock) {
			SQLiteDatabase database = mSqLiteOpenHelper.getWritableDatabase();
			try {
				database.beginTransaction();
				Cursor cursor = database.query("http_cache", new String[] { "id", "cache" }, null, null, null, null, null);
				try {
					while (cursor.moveToNext()) {
						int id = cursor.getInt(0);
						String cache = cursor.getString(1);
						if (!TextUtils.isEmpty(cache)) {
							File cacheFile = new File(Broid.getApplication().getCacheDir(), cache);
							if (cacheFile.exists())
								cacheFile.delete();
						}
						database.delete("http_cache", "id = ?", new String[] { String.valueOf(id) });
					}
					database.setTransactionSuccessful();
				} catch (Exception e) {
					Log.w(Broid.TAG, null, e);
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
		}

	}

	public String get(RequestEntry request) {
		synchronized (lock) {
			if (request == null) {
				return null;
			}
			SQLiteDatabase database = mSqLiteOpenHelper.getReadableDatabase();
			try {
				database.beginTransaction();
				String[] whereArgs = new String[] { Utils.bytesToHexString(md5Hex(request.getUrl())),
						Utils.bytesToHexString(md5Hex(request.getParamsStr())) };
				Cursor cursor = database.query("http_cache", null, "url = ? and params = ?", whereArgs, null, null, null);
				try {
					if (cursor.moveToNext()) {
						String cache = cursor.getString(cursor.getColumnIndex("cache"));
						if (System.currentTimeMillis() - cursor.getLong(cursor.getColumnIndex("time")) > request.getSession()) {
							database.delete("http_cache", "id=?", new String[] { cursor.getString(cursor.getColumnIndex("id")) });
							return null;
						}
						if (!TextUtils.isEmpty(cache)) {
							File cacheDir = Broid.getApplication().getCacheDir();
							File cacheFile = new File(cacheDir, cache);
							if (cacheFile.exists()) {
								if (cacheFile.canRead()) {
									String result = readFile(cacheFile);
									database.setTransactionSuccessful();
									return result;
								} else
									cacheFile.delete();
							}
						}
						database.delete("http_cache", "url = ? and params = ?", whereArgs);
						database.setTransactionSuccessful();
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

	public boolean save(RequestEntry request, String result) {
		synchronized (lock) {
			if (request == null || TextUtils.isEmpty(result)) {
				return false;
			}
			if (!request.isCache()) {
				return false;
			}
			final String cache = UUID.randomUUID().toString();
			File cacheDir = Broid.getApplication().getCacheDir();
			File cacheFile = new File(cacheDir, cache);
			try {
				if (cacheDir != null && cacheDir.isDirectory()) {
					if (cacheFile.canWrite()) {
						writeFile(cacheFile, result);
					} else
						throw new Exception();
					SQLiteDatabase database = mSqLiteOpenHelper.getWritableDatabase();
					database.beginTransaction();
					try {
						ContentValues values = new ContentValues();
						Cursor cursor = database.query(
								"http_cache",
								null,
								"url = ? and params = ?",
								new String[] { Utils.bytesToHexString(md5Hex(request.getUrl())),
										Utils.bytesToHexString(md5Hex(request.getParamsStr())) }, null, null, null);
						try {
							if (cursor.moveToNext()) {
								File oldCache = new File(cacheDir, cursor.getString(cursor.getColumnIndex("cache")));
								if (oldCache.exists()) {
									oldCache.delete();
								}
								values.put("cache", cache);
								values.put("md5", Utils.bytesToHexString(md5Hex(cacheFile)));
								values.put("time", System.currentTimeMillis());
								if (database.update("http_cache", values, "id = ?",
										new String[] { String.valueOf(cursor.getInt(0)) }) == 1) {
									database.setTransactionSuccessful();
								} else
									throw new Exception();
							} else {
								values.put("url", Utils.bytesToHexString(md5Hex(request.getUrl())));
								values.put("params", Utils.bytesToHexString(md5Hex(request.getParamsStr())));
								values.put("cache", cache);
								values.put("md5", Utils.bytesToHexString(md5Hex(cacheFile)));
								values.put("time", System.currentTimeMillis());
								if (database.insert("http_cache", null, values) != -1) {
									database.setTransactionSuccessful();
								} else
									throw new Exception();
							}
						} catch (Exception e) {
							throw e;
						} finally {
							if (cursor != null && !cursor.isClosed())
								cursor.close();
						}

					} catch (Exception e) {
						throw e;
					} finally {
						database.endTransaction();
						if (database != null && database.isOpen())
							database.close();
					}
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
				if (cacheFile.exists())
					cacheFile.delete();
				return false;
			}
			return true;
		}
	}

	private byte[] md5Hex(String str) throws Exception {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(str.getBytes("UTF-8"));
			return digest.digest();
		} catch (Exception e) {
			Log.w(Broid.TAG, null, e);
			throw e;
		} finally {
			if (digest != null) {
				digest.reset();
			}
		}
	}

	private byte[] md5Hex(File file) throws Exception {
		MessageDigest digest = null;
		InputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] bs = new byte[1024];
			digest = MessageDigest.getInstance("MD5");
			int i = 0;
			while ((i = fis.read(bs)) > 0) {
				digest.update(bs, 0, i);
			}
			return digest.digest();
		} catch (Exception e) {
			Log.w(Broid.TAG, null, e);
			throw e;
		} finally {
			if (digest != null)
				digest.reset();
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					Log.w(Broid.TAG, null, e);
				}
			}
		}
	}

	private static void writeFile(File file, String content) throws Exception {
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			bw.write(content);
		} catch (Exception e) {
			throw e;
		} finally {
			if (bw != null) {
				bw.flush();
				bw.close();
			}
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private static String readFile(File file) throws Exception {
		FileReader fr = null;
		StringBuilder builder = new StringBuilder();
		try {
			fr = new FileReader(file);
			int i;
			while ((i = fr.read()) != -1)
				builder.append(i);
		} catch (Exception e) {
			throw e;
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
				}
			}
		}
		return builder.toString();
	}
}
