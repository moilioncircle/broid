package com.iamuv.broid.storage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.Utils;
import com.iamuv.broid.storage.StorageEntry.SQLiteField;
import com.iamuv.broid.storage.StorageEntry.SQLiteTable;

/**
 * SQLite存储
 * 
 * @author Uv
 * @date 2014-6-3
 * @email uv@iamuv.com
 * @blog http://www.iamuv.com
 * @param <T>
 */
public class SQLiteStorage<T> {

	Class<T> mType;

	private SQLiteHelper mSqLiteHelper;

	private SQLiteDatabase mDatabase;

	String mTableName;

	Field[] mFields;

	ArrayList<Field> mAlterFields;

	private Object lock = new Object();

	private Cursor mCursor;

	int mVersion;

	Field mAutoKeyField;

	public SQLiteStorage(Class<T> type) {
		mType = type;
		mFields = mType.getDeclaredFields();
		SQLiteTable table = mType.getAnnotation(SQLiteTable.class);
		if (table != null) {
			mVersion = getVersion(table.database());
			mTableName = type.getSimpleName();
			mSqLiteHelper = new SQLiteHelper(table.database());
			if (existTable()) {
				mAlterFields = getAlterFields(getTableFields(mTableName));
				if (mAlterFields != null) {
					setVersion(table.database(), ++mVersion);
					mSqLiteHelper = new SQLiteHelper(table.database());
				}
			} else {
				setVersion(table.database(), ++mVersion);
				mSqLiteHelper = new SQLiteHelper(table.database());
			}

		} else
			throw new SQLiteStorageException("can not find the class with the annotation 'SQLiteTable'");
	}

	private int getVersion(String databaseName) {
		return Broid.getConfig().getInt(databaseName + "_version", 1);
	}

	private void setVersion(String databaseName, int version) {
		Broid.getConfig().edit().putInt(databaseName + "_version", version).commit();
	}

	private ArrayList<Field> getAlterFields(HashMap<String, Integer> tableFields) {
		if (mFields.length > tableFields.size()) {
			ArrayList<Field> results = new ArrayList<Field>();
			int length = mFields.length;
			for (int i = 0; i < length; i++) {
				mFields[i].setAccessible(true);
				if (tableFields.get(mFields[i].getName()) == null) {
					results.add(mFields[i]);
				}
			}
			return results.size() > 0 ? results : null;
		}
		return null;

	}

	private HashMap<String, Integer> getTableFields(String tableName) {
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getReadableDatabase();
				try {
					mDatabase.beginTransaction();
					mCursor = mDatabase.rawQuery("PRAGMA table_info(" + tableName + ')', null);
					if (mCursor != null && mCursor.getColumnCount() > 0) {
						HashMap<String, Integer> results = new HashMap<String, Integer>(mCursor.getColumnCount());
						while (mCursor.moveToNext()) {
							String name = mCursor.getString(mCursor.getColumnIndex("name"));
							results.put(name, mCursor.getPosition());
							if (mCursor.getInt(mCursor.getColumnIndex("pk")) == 1) {
								int length = mFields.length;
								for (int i = 0; i < length; i++) {
									mFields[i].setAccessible(true);
									if (mFields[i].getName().equals(name)) {
										mAutoKeyField = mFields[i];
									}
								}
							}
						}
						return results;
					}
					mDatabase.setTransactionSuccessful();
				} catch (Exception e) {
					Log.w(Broid.TAG, null, e);
				} finally {
					closeCursor();
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();
			}
		}
		return null;
	}

	private boolean existTable() {
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getReadableDatabase();
				try {
					mDatabase.beginTransaction();
					mCursor = mDatabase.rawQuery("SELECT * FROM SQLITE_MASTER WHERE NAME = ?", buildSelectionArgs(mTableName));
					if (mCursor != null && mCursor.moveToNext()) {
						return true;
					}
					mDatabase.setTransactionSuccessful();
				} catch (Exception e) {
					Log.w(Broid.TAG, null, e);
				} finally {
					closeCursor();
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();

			}
			return false;
		}
	}

	private void closeDatabase() {
		if (mDatabase != null) {
			mDatabase.close();
		}
	}

	private void closeCursor() {
		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}
	}

	private ContentValues toValue(T c) {
		ContentValues values = new ContentValues();
		int length = mFields.length;
		SQLiteField sqLiteField;
		for (int i = 0; i < length; i++) {
			sqLiteField = mFields[i].getAnnotation(SQLiteField.class);
			if (sqLiteField != null && sqLiteField.isAutoKey()) {
				continue;
			}
			try {
				if (int.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), Integer.valueOf(mFields[i].getInt(c)));
				} else if (byte[].class == mFields[i].getType()) {
					values.put(mFields[i].getName(), Utils.bytesToHexString((byte[]) mFields[i].get(c)));
				} else if (String.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), String.valueOf(mFields[i].get(c)));
				} else if (char.class == mFields[i].getType() || Character.class.isAssignableFrom(mFields[i].getType())) {
					values.put(mFields[i].getName(), String.valueOf(mFields[i].get(c)));
				} else if (short.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), mFields[i].getShort(c));
				} else if (long.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), mFields[i].getLong(c));
				} else if (float.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), mFields[i].getFloat(c));
				} else if (double.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), mFields[i].getDouble(c));
				} else if (boolean.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), mFields[i].getBoolean(c));
				} else if (Date.class == mFields[i].getType()) {
					values.put(mFields[i].getName(), ((Date) mFields[i].get(c)).getTime());
				} else if (Integer.class.isAssignableFrom(mFields[i].getType())
						|| Short.class.isAssignableFrom(mFields[i].getType())
						|| Long.class.isAssignableFrom(mFields[i].getType())
						|| Float.class.isAssignableFrom(mFields[i].getType())
						|| Double.class.isAssignableFrom(mFields[i].getType())
						|| Boolean.class.isAssignableFrom(mFields[i].getType())) {
					if (mFields[i].get(c) != null) {
						values.put(mFields[i].getName(), String.valueOf(mFields[i].get(c)));
					}
				} else {
					Log.w(Broid.TAG, "can't read the type'" + mFields[i].getType() + "' , and value will be null", null);
					values.putNull(mFields[i].getName());
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, "get " + mFields[i].getName() + " value throw exception", e);
			}
		}
		return values;
	}

	private void toField(Cursor cursor, T c) {
		int length = mFields.length;
		for (int i = 0; i < length; i++) {
			try {
				mFields[i].setAccessible(true);
				int index = cursor.getColumnIndex(mFields[i].getName());
				if (mCursor.getString(index) != null) {
					if (int.class == mFields[i].getType()) {
						mFields[i].setInt(c, mCursor.getInt(index));
					} else if (byte[].class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Utils.hexStringToBytes(mCursor.getString(index)));
					} else if (String.class == mFields[i].getType()) {
						mFields[i].set(c, mCursor.getString(index));
					} else if (short.class == mFields[i].getType()) {
						mFields[i].setShort(c, cursor.getShort(index));
					} else if (long.class == mFields[i].getType()) {
						mFields[i].setLong(c, cursor.getLong(index));
					} else if (float.class == mFields[i].getType()) {
						mFields[i].setFloat(c, cursor.getFloat(index));
					} else if (double.class == mFields[i].getType()) {
						mFields[i].setDouble(c, cursor.getDouble(index));
					} else if (boolean.class == mFields[i].getType()) {
						mFields[i].setBoolean(c, Boolean.getBoolean(cursor.getString(index)));
					} else if (Date.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, new Date(cursor.getLong(index)));
					} else if (char.class == mFields[i].getType()) {
						if (cursor.getString(index).length() > 0) {
							mFields[i].setChar(c, cursor.getString(index).charAt(0));
						} else
							mFields[i].setChar(c, (char) 0);
					} else if (Integer.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Integer.valueOf(mCursor.getInt(index)));
					} else if (Short.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Short.valueOf(cursor.getString(index)));
					} else if (Long.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Long.valueOf(cursor.getString(index)));
					} else if (Float.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Float.valueOf(cursor.getString(index)));
					} else if (Double.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Double.valueOf(cursor.getString(index)));
					} else if (Boolean.class.isAssignableFrom(mFields[i].getType())) {
						mFields[i].set(c, Boolean.valueOf(cursor.getString(index)));
					} else if (Character.class.isAssignableFrom(mFields[i].getType())) {
						if (cursor.getString(index).length() > 0) {
							mFields[i].set(c, Character.valueOf((cursor.getString(index).charAt(0))));
						} else
							mFields[i].set(c, Character.valueOf((char) 0));
					} else {
						Log.w(Broid.TAG, "can't read the type'" + mFields[i].getType() + "' , and value will be null", null);
					}
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, "set " + mFields[i].getName() + " value throw exception", e);
			}
		}

	}

	private String[] buildSelectionArgs(Object... args) {
		String[] selectionArgs = null;
		if (args != null) {
			int length = args.length;
			if (length > 0) {
				selectionArgs = new String[length];
				for (int i = 0; i < length; i++) {
					if (byte[].class.isAssignableFrom(args[i].getClass())) {
						selectionArgs[i] = Utils.bytesToHexString((byte[]) args[i]);
					} else {
						selectionArgs[i] = String.valueOf(args[i]);
					}
				}
			}
		}
		return selectionArgs;
	}

	private String buildSelection(String... selections) {
		StringBuilder selectionString = new StringBuilder();
		if (selections != null) {
			int length = selections.length;
			for (int i = 0; i < length; i++) {
				selectionString.append(" AND ");
				selectionString.append(selections[i]);
				selectionString.append("=?");
			}
		}
		return selectionString.substring(5);
	}

	public long save(T c) {
		long rowid = -1;
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getWritableDatabase();
				mDatabase.beginTransaction();
				ContentValues values = toValue(c);
				rowid = mDatabase.insert(mTableName, null, values);
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				mDatabase.close();
			}
		}
		return rowid;
	}

	public int save(List<T> list) {
		int result = 0;
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getWritableDatabase();
				mDatabase.beginTransaction();
				ContentValues values;
				int size = list.size();
				for (int i = 0; i < size; i++) {
					values = toValue(list.get(i));
					if (mDatabase.insert(mTableName, null, values) != -1) {
						result++;
					}
				}
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				mDatabase.close();
			}
		}
		return result;
	}

	public ArrayList<T> get(String selection, Object[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		ArrayList<T> results = new ArrayList<T>();
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getReadableDatabase();
				try {
					mDatabase.beginTransaction();
					mCursor = mDatabase.query(mTableName, null, selection, buildSelectionArgs(selectionArgs), groupBy, having,
							orderBy, limit);
					T result;
					while (mCursor.moveToNext()) {
						result = mType.newInstance();
						toField(mCursor, result);
						results.add(result);
					}
					mDatabase.setTransactionSuccessful();
				} catch (Exception e) {
					Log.w(Broid.TAG, null, e);
				} finally {
					closeCursor();
				}
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();
			}
		}
		return results;
	}

	public ArrayList<T> get(String selection, Object[] selectionArgs) {
		return get(selection, selectionArgs, null, null, null, null);
	}

	public ArrayList<T> get() {
		return get(null, null, null, null, null, null);
	}

	public T getByKey(int keyValue) {
		if (mAutoKeyField == null) {
			Log.w(Broid.TAG, "table " + mTableName + " do not have the primary key", null);
			return null;
		}
		ArrayList<T> list = get(buildSelection(mAutoKeyField.getName()), new Object[] { keyValue });
		return list.size() == 1 ? list.get(0) : null;
	}

	public T getByRow(long rowid) {
		ArrayList<T> list = get("ROWID = ?", new Object[] { rowid });
		return list.size() == 1 ? list.get(0) : null;
	}

	public int upd(ContentValues values, String whereClause, String[] whereArgs) {
		int result = 0;
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getReadableDatabase();
				mDatabase.beginTransaction();
				result = mDatabase.update(mTableName, values, whereClause, whereArgs);
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();
			}
		}
		return result;
	}

	public int updByKeys(T c, int... keyValues) {
		int result = 0;
		synchronized (lock) {
			try {
				mDatabase = mSqLiteHelper.getReadableDatabase();
				mDatabase.beginTransaction();
				ContentValues values = toValue(c);
				StringBuilder stringBuilder = new StringBuilder();
				int length = keyValues.length;
				for (int i = 0; i < length; i++) {
					stringBuilder.append(',').append(String.valueOf(keyValues[i]));
				}
				result = mDatabase.update(mTableName, values, mAutoKeyField.getName() + " IN ("
						+ stringBuilder.substring(1).toString() + ')', null);
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();
			}
		}
		return result;

	}

	public int delByKeys(int... keyValues) {
		int result = 0;
		if (mAutoKeyField == null) {
			Log.w(Broid.TAG, "table " + mTableName + " do not have the primary key", null);
			return result;
		}
		synchronized (lock) {
			StringBuilder stringBuilder = new StringBuilder();
			int length = keyValues.length;
			for (int i = 0; i < length; i++) {
				stringBuilder.append(',').append(String.valueOf(keyValues[i]));
			}
			try {
				mDatabase = mSqLiteHelper.getWritableDatabase();
				mDatabase.beginTransaction();
				result = mDatabase.delete(mTableName, mAutoKeyField.getName() + " IN (" + stringBuilder.substring(1).toString()
						+ ')', null);
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();
			}
		}
		return result;
	}

	public int delByRows(long... rowids) {
		int result = 0;
		synchronized (lock) {
			StringBuilder stringBuilder = new StringBuilder();
			int length = rowids.length;
			for (int i = 0; i < length; i++) {
				stringBuilder.append(',').append(String.valueOf(rowids[i]));
			}
			try {
				mDatabase = mSqLiteHelper.getWritableDatabase();
				mDatabase.beginTransaction();
				result = mDatabase.delete(mTableName, "ROWID IN (" + stringBuilder.substring(1).toString() + ')', null);
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				Log.w(Broid.TAG, null, e);
			} finally {
				mDatabase.endTransaction();
				closeDatabase();
			}
		}
		return result;
	}

	public int del() {
		int result = 0;
		try {
			mDatabase = mSqLiteHelper.getWritableDatabase();
			mDatabase.beginTransaction();
			result = mDatabase.delete(mTableName, null, null);
			mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
			Log.w(Broid.TAG, null, e);
		} finally {
			mDatabase.endTransaction();
			closeDatabase();
		}
		return result;
	}

	public int del(String whereClause, String[] whereArgs) {
		int result = 0;
		try {
			mDatabase = mSqLiteHelper.getWritableDatabase();
			mDatabase.beginTransaction();
			result = mDatabase.delete(mTableName, whereClause, whereArgs);
			mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
			Log.w(Broid.TAG, null, e);
		} finally {
			mDatabase.endTransaction();
			closeDatabase();
		}
		return result;

	}

	class SQLiteHelper extends SQLiteOpenHelper {

		SQLiteHelper(String name) {
			super(Broid.getApplication(), name, null, mVersion);
			Log.d(Broid.TAG, name + " version is " + mVersion, null);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			String sql;
			if (mAlterFields != null) {
				int size = mAlterFields.size();
				for (int i = 0; i < size; i++) {
					sql = getAlterSQL(mAlterFields.get(i));
					if (sql != null) {
						Log.d(Broid.TAG, "alter a new column, alter sql is\r\n" + sql, null);
						db.execSQL(sql);
					}
				}
			} else {
				sql = getCreateSQL();
				if (sql != null) {
					Log.d(Broid.TAG, "add a new table, create sql is\r\n" + sql, null);
					db.execSQL(sql);
				}
			}
		}

		private String getCreateSQL() {
			StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(mTableName).append("( ");
			StringBuilder s = new StringBuilder();
			SQLiteField f;
			int length = mFields.length;
			for (int i = 0; i < length; i++) {
				mFields[i].setAccessible(true);
				f = mFields[i].getAnnotation(SQLiteField.class);
				if (f != null) {
					s.append(mFields[i].getName());
					initField(s, mFields[i]);
					if (f.isAutoKey()) {
						s.append("PRIMARY KEY AUTOINCREMENT");
						mAutoKeyField = mFields[i];
					} else if (!TextUtils.isEmpty(f.value())) {
						s.append("DEFAULT ").append(f.value());
					}
				} else {
					s.append(mFields[i].getName());
					initField(s, mFields[i]);
				}
				s.append(" ,");
			}
			if (s.length() > 0) {
				s.setCharAt(s.length() - 1, ')');
			} else {
				Log.w(Broid.TAG, "class  '" + mType.getSimpleName() + "' have no fields", null);
				return null;
			}
			return sql.append(s).toString();
		}

		private String getAlterSQL(Field alterField) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ").append(mTableName).append(" ADD COLUMN ");
			sql.append(alterField.getName());
			initField(sql, alterField);
			SQLiteField f = alterField.getAnnotation(SQLiteField.class);
			if (f != null && !TextUtils.isEmpty(f.value())) {
				sql.append("DEFAULT ").append(f.value());
			}
			return sql.toString();
		}

		private void initField(StringBuilder s, Field field) {
			if (field == mAutoKeyField || int.class == field.getType() || Integer.class.isAssignableFrom(field.getType())) {
				s.append(" INTEGER ");
			} else if (byte[].class == field.getType()) {
				s.append(" TEXT ");
			} else if (String.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (char.class == field.getType() || Character.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (short.class == field.getType() || Short.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (long.class == field.getType() || Long.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (float.class == field.getType() || Float.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (double.class == field.getType() || Double.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (boolean.class == field.getType() || Boolean.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else if (Date.class.isAssignableFrom(field.getType())) {
				s.append(" TEXT ");
			} else {
				Log.w(Broid.TAG, "can't read the type'" + field.getType() + "' , and the column type will be TEXT", null);
				s.append(" TEXT ");
			}
		}
	}

}

class SQLiteStorageException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SQLiteStorageException(String msg) {
		super(msg);
	}

}