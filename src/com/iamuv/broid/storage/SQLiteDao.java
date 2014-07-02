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
package com.iamuv.broid.storage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.annotation.Column;
import com.iamuv.broid.annotation.Table;
import com.iamuv.broid.utils.HexUtils;

/**
 * SQLite存储 <br>
 * <ul>
 * <li>自动创建表</li>
 * <li>自动升级表，只限增加字段</li>
 * <li>支持数据类型如下:<br>
 * int, float, double, long, short, boolean, char<br>
 * Interger, Float, Double, Long, Short, Boolean<br>
 * Character, Date, String, byte[]</li>
 * <li>封装类型支持null</li>
 * <li>byte[]将以16进制方式存入数据库</li>
 * </ul>
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about SQLiteDaoImpl.java">uv@iamuv.com</a> <br>
 *         2014-6-5
 * 
 * @param <T>
 *            表的映射类
 */
public class SQLiteDao<T> {

    static volatile int mVersion;

    Class<T> mType;

    private SQLiteHelper mSqLiteHelper;

    private SQLiteDatabase mDatabase;

    String mTableName;

    ArrayList<Field> mFields = new ArrayList<Field>();

    ArrayList<Field> mAlterFields;

    private static Object LOCK = new Object();

    private Cursor mCursor;

    Field mAutoKeyField;

    Table mTable;

    Column mColumn;

    final int mSize;

    public SQLiteDao(Class<T> cla) {
	mType = cla;
	initFields();
	mSize = mFields.size();
	if (mSize == 0) {
	    Log.w(Broid.TAG, "can not find any fields in the class " + mType.getSimpleName(), null);
	}
	mTable = mType.getAnnotation(Table.class);
	if (mTable != null) {
	    synchronized (LOCK) {
		mVersion = getVersion(mTable.database());
		mTableName = "table_" + mType.getSimpleName().toLowerCase(Locale.getDefault());
		mSqLiteHelper = new SQLiteHelper();
		if (existTable()) {
		    Log.d("the table " + mTableName + " exist");
		    mAlterFields = getAlterFields(getTableFields(mTableName));
		    if (mAlterFields != null) {
			Log.d("the table " + mTableName + " need to alter");
			setVersion(mTable.database(), ++mVersion);
			mSqLiteHelper = new SQLiteHelper();
		    } else
			return;
		} else {
		    Log.d("the table " + mTableName + " do not exist");
		    setVersion(mTable.database(), ++mVersion);
		    mSqLiteHelper = new SQLiteHelper();
		}
		try {
		    mDatabase = mSqLiteHelper.getWritableDatabase();
		} finally {
		    mDatabase.close();
		}
	    }
	} else
	    throw new DaoException("can not find the class with the annotation 'Table'");
    }

    private int getVersion(String databaseName) {
	return Broid.getConfig().getInt(databaseName + "_version", 1);
    }

    private void setVersion(String databaseName, int version) {
	Broid.getConfig().edit().putInt(databaseName + "_version", version).commit();
    }

    private void initFields() {
	Field[] fields = mType.getDeclaredFields();
	final int length = fields.length;
	for (int i = 0; i < length; i++) {
	    if (Modifier.isStatic(fields[i].getModifiers()))
		continue;
	    mColumn = fields[i].getAnnotation(Column.class);
	    if (mColumn != null && mColumn.ignore())
		continue;
	    fields[i].setAccessible(true);
	    mFields.add(fields[i]);
	}
    }

    private ArrayList<Field> getAlterFields(HashMap<String, Integer> tableFields) {
	if (mFields.size() > tableFields.size()) {
	    ArrayList<Field> results = new ArrayList<Field>();
	    for (int i = 0; i < mSize; i++) {
		if (tableFields.get(mFields.get(i).getName()) == null) {
		    results.add(mFields.get(i));
		}
	    }
	    return results.size() > 0 ? results : null;
	}
	return null;

    }

    private HashMap<String, Integer> getTableFields(String tableName) {
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
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
				for (int i = 0; i < mSize; i++) {
				    if (mFields.get(i).getName().equals(name)) {
					mAutoKeyField = mFields.get(i);
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
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return null;
    }

    private boolean existTable() {
	try {
	    mSqLiteHelper = new SQLiteHelper();
	    mDatabase = mSqLiteHelper.getReadableDatabase();
	    try {
		mDatabase.beginTransaction();
		mCursor = mDatabase.rawQuery("SELECT * FROM SQLITE_MASTER WHERE NAME = ?",
		    buildSelectionArgs(mTableName));
		if (mCursor != null && mCursor.moveToNext()) {
		    return true;
		}
		mDatabase.setTransactionSuccessful();
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		closeCursor();
		endTransaction();
	    }
	} catch (Exception e) {
	    Log.w(Broid.TAG, null, e);
	} finally {
	    mDatabase.close();

	}
	return false;
    }

    private void endTransaction() {
	if (mDatabase.inTransaction())
	    mDatabase.endTransaction();
    }

    private void closeCursor() {
	if (mCursor != null && !mCursor.isClosed())
	    mCursor.close();
    }

    private ContentValues toValue(T c) {
	ContentValues values = new ContentValues();
	for (int i = 0; i < mSize; i++) {
	    mColumn = mFields.get(i).getAnnotation(Column.class);
	    if (mColumn != null) {
		if (mColumn.isAutoKey() || mColumn.ignore()) {
		    continue;
		}
	    }
	    try {
		if (int.class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), Integer.valueOf(mFields.get(i).getInt(c)));
		} else if (byte[].class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), HexUtils.bytesToHexString((byte[]) mFields.get(i).get(c)));
		} else if (String.class == mFields.get(i).getType()) {
		    if (mFields.get(i).get(c) != null)
			values.put(mFields.get(i).getName(), String.valueOf(mFields.get(i).get(c)));
		} else if (char.class == mFields.get(i).getType()
		    || Character.class.isAssignableFrom(mFields.get(i).getType())) {
		    if (mFields.get(i).get(c) != null)
			values.put(mFields.get(i).getName(), String.valueOf(mFields.get(i).get(c)));
		} else if (short.class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), mFields.get(i).getShort(c));
		} else if (long.class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), mFields.get(i).getLong(c));
		} else if (float.class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), mFields.get(i).getFloat(c));
		} else if (double.class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), mFields.get(i).getDouble(c));
		} else if (boolean.class == mFields.get(i).getType()) {
		    values.put(mFields.get(i).getName(), mFields.get(i).getBoolean(c));
		} else if (Date.class == mFields.get(i).getType()) {
		    if (mFields.get(i).get(c) != null)
			values.put(mFields.get(i).getName(), ((Date) mFields.get(i).get(c)).getTime());
		} else if (Integer.class.isAssignableFrom(mFields.get(i).getType())
		    || Short.class.isAssignableFrom(mFields.get(i).getType())
		    || Long.class.isAssignableFrom(mFields.get(i).getType())
		    || Float.class.isAssignableFrom(mFields.get(i).getType())
		    || Double.class.isAssignableFrom(mFields.get(i).getType())
		    || Boolean.class.isAssignableFrom(mFields.get(i).getType())) {
		    if (mFields.get(i).get(c) != null)
			values.put(mFields.get(i).getName(), String.valueOf(mFields.get(i).get(c)));
		} else {
		    Log.w(Broid.TAG, "can't read the type'" + mFields.get(i).getType() + "' , and value will be null",
			null);
		    values.putNull(mFields.get(i).getName());
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, "get " + mFields.get(i).getName() + " value throw exception", e);
	    }
	}
	return values;
    }

    private void toField(Cursor cursor, T c) {
	for (int i = 0; i < mSize; i++) {
	    try {
		mFields.get(i).setAccessible(true);
		int index = cursor.getColumnIndex(mFields.get(i).getName());
		if (index != -1 && mCursor.getString(index) != null) {
		    if (int.class == mFields.get(i).getType()) {
			mFields.get(i).setInt(c, mCursor.getInt(index));
		    } else if (byte[].class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, HexUtils.hexStringToBytes(mCursor.getString(index)));
		    } else if (String.class == mFields.get(i).getType()) {
			mFields.get(i).set(c, mCursor.getString(index));
		    } else if (short.class == mFields.get(i).getType()) {
			mFields.get(i).setShort(c, cursor.getShort(index));
		    } else if (long.class == mFields.get(i).getType()) {
			mFields.get(i).setLong(c, cursor.getLong(index));
		    } else if (float.class == mFields.get(i).getType()) {
			mFields.get(i).setFloat(c, cursor.getFloat(index));
		    } else if (double.class == mFields.get(i).getType()) {
			mFields.get(i).setDouble(c, cursor.getDouble(index));
		    } else if (boolean.class == mFields.get(i).getType()) {
			mFields.get(i).setBoolean(c, Boolean.getBoolean(cursor.getString(index)));
		    } else if (Date.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, new Date(cursor.getLong(index)));
		    } else if (char.class == mFields.get(i).getType()) {
			if (cursor.getString(index).length() > 0) {
			    mFields.get(i).setChar(c, cursor.getString(index).charAt(0));
			} else
			    mFields.get(i).setChar(c, (char) 0);
		    } else if (Integer.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, Integer.valueOf(mCursor.getInt(index)));
		    } else if (Short.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, Short.valueOf(cursor.getString(index)));
		    } else if (Long.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, Long.valueOf(cursor.getString(index)));
		    } else if (Float.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, Float.valueOf(cursor.getString(index)));
		    } else if (Double.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, Double.valueOf(cursor.getString(index)));
		    } else if (Boolean.class.isAssignableFrom(mFields.get(i).getType())) {
			mFields.get(i).set(c, Boolean.valueOf(cursor.getString(index)));
		    } else if (Character.class.isAssignableFrom(mFields.get(i).getType())) {
			if (cursor.getString(index).length() > 0) {
			    mFields.get(i).set(c, Character.valueOf((cursor.getString(index).charAt(0))));
			} else
			    mFields.get(i).set(c, Character.valueOf((char) 0));
		    } else {
			Log.w(Broid.TAG, "can't read the type'" + mFields.get(i).getType()
			    + "' , and value will be null", null);
		    }
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, "set " + mFields.get(i).getName() + " value throw exception", e);
	    }
	}

    }

    private String[] buildSelectionArgs(Object... args) {
	String[] selectionArgs = null;
	if (args != null) {
	    final int length = args.length;
	    if (length > 0) {
		selectionArgs = new String[length];
		for (int i = 0; i < length; i++) {
		    if (byte[].class.isAssignableFrom(args[i].getClass())) {
			selectionArgs[i] = HexUtils.bytesToHexString((byte[]) args[i]);
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
	    final int length = selections.length;
	    for (int i = 0; i < length; i++) {
		selectionString.append(" AND ");
		selectionString.append(selections[i]);
		selectionString.append("=?");
	    }
	}
	return selectionString.substring(5);
    }

    /**
     * insert单条数据
     * 
     * @param c
     *            数据实体类
     * @return insert的数据在表中的rowid, 若insert失败则返回-1
     */
    public long save(T c) {
	long rowid = -1;
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    ContentValues values = toValue(c);
		    rowid = mDatabase.insert(mTableName, null, values);
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return rowid;
    }

    /**
     * insert多条数据
     * 
     * @param list
     *            数据实体类list
     * @return insert数据的数量 此返回值不会低于0
     */
    public int save(List<T> list) {
	int result = 0;
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    ContentValues values;
		    final int size = list.size();
		    for (int i = 0; i < size; i++) {
			values = toValue(list.get(i));
			if (mDatabase.insert(mTableName, null, values) != -1) {
			    result++;
			}
		    }
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    /**
     * 根据条件获取数据
     * 
     * @param selection
     *            条件语句
     * @param selectionArgs
     *            条件值
     * @param groupBy
     * @param having
     * @param orderBy
     * @param limit
     * @return 数据list
     */
    public ArrayList<T> get(String selection, Object[] selectionArgs, String groupBy, String having, String orderBy,
	String limit) {
	ArrayList<T> results = new ArrayList<T>();
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getReadableDatabase();
		try {
		    mDatabase.beginTransaction();
		    mCursor = mDatabase.query(mTableName, null, selection, buildSelectionArgs(selectionArgs), groupBy,
			having, orderBy, limit);
		    T result;
		    while (mCursor.moveToNext()) {
			result = mType.newInstance();
			toField(mCursor, result);
			results.add(result);
		    }
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    closeCursor();
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return results;
    }

    /**
     * 根据条件获取数据
     * 
     * @param selection
     * @param selectionArgs
     * @return 数据list
     */
    public ArrayList<T> get(String selection, Object[] selectionArgs) {
	return get(selection, selectionArgs, null, null, null, null);
    }

    /**
     * 获取所有数据
     * 
     * @return 数据list
     */
    public ArrayList<T> get() {
	return get(null, null, null, null, null, null);
    }

    /**
     * 根据主键获取数据
     * 
     * @param keyValue
     *            主键
     * @return 数据实体类
     */
    public T getByKey(int keyValue) {
	if (mAutoKeyField == null) {
	    Log.w(Broid.TAG, "table " + mTableName + " do not have the primary key", null);
	    return null;
	}
	ArrayList<T> list = get(buildSelection(mAutoKeyField.getName()), new Object[] { keyValue });
	return list.size() == 1 ? list.get(0) : null;
    }

    /**
     * 根据Row ID获取数据
     * 
     * @param rowid
     * @return 数据实体类 如果不存在数据则返回为null
     */
    public T getByRow(long rowid) {
	ArrayList<T> list = get("ROWID = ?", new Object[] { rowid });
	return list.size() == 1 ? list.get(0) : null;
    }

    /**
     * 根据条件更新数据
     * 
     * @param values
     * @param whereClause
     * @param whereArgs
     * @return
     */
    public int upd(ContentValues values, String whereClause, Object[] whereArgs) {
	int result = 0;
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    result = mDatabase.update(mTableName, values, whereClause, buildSelectionArgs(whereArgs));
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    public int updByKeys(List<T> list) {
	int result = 0;
	if (mAutoKeyField == null) {
	    Log.w(Broid.TAG, "table " + mTableName + " do not have the primary key", null);
	    return result;
	}
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    ContentValues values = null;
		    final int size = list.size();
		    StringBuilder whereClause = new StringBuilder(mAutoKeyField.getName());
		    whereClause.append("=?");
		    String[] whereArgs = new String[1];
		    for (int i = 0; i < size; i++) {
			values = toValue(list.get(i));
			whereArgs[0] = values.getAsString(mAutoKeyField.getName());
			if (mDatabase.update(mTableName, values, whereClause.toString(), whereArgs) != -1)
			    result++;
		    }
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;

    }

    public int updByKey(T c) {
	int result = 0;
	if (mAutoKeyField == null) {
	    Log.w(Broid.TAG, "table " + mTableName + " do not have the primary key", null);
	    return result;
	}
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    ContentValues values = null;
		    StringBuilder whereClause = new StringBuilder(mAutoKeyField.getName());
		    whereClause.append("=?");
		    values = toValue(c);
		    String[] whereArgs = new String[] { values.getAsString(mAutoKeyField.getName()) };
		    result = mDatabase.update(mTableName, values, whereClause.toString(), whereArgs);
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    /**
     * 根据主键 删除表中数据
     * 
     * @param keyValues
     *            主键 支持多参
     * @return 删除数据的数量 此返回值不会低于0 若表中不存在自增主键则会返回0
     */
    public int delByKeys(int... keyValues) {
	int result = 0;
	if (mAutoKeyField == null) {
	    Log.w(Broid.TAG, "table " + mTableName + " do not have the primary key", null);
	    return result;
	}
	synchronized (LOCK) {
	    StringBuilder stringBuilder = new StringBuilder();
	    final int length = keyValues.length;
	    for (int i = 0; i < length; i++) {
		stringBuilder.append(',').append(String.valueOf(keyValues[i]));
	    }
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    result = mDatabase.delete(mTableName, mAutoKeyField.getName() + " IN ("
			+ stringBuilder.substring(1).toString() + ')', null);
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    /**
     * 根据RowID 删除表中数据
     * 
     * @param rowids
     *            支持多参
     * @return 删除数据的数量 此返回值不会低于0
     */
    public int delByRows(long... rowids) {
	int result = 0;
	synchronized (LOCK) {
	    StringBuilder stringBuilder = new StringBuilder();
	    final int length = rowids.length;
	    for (int i = 0; i < length; i++) {
		stringBuilder.append(',').append(String.valueOf(rowids[i]));
	    }
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    result = mDatabase.delete(mTableName, "ROWID IN (" + stringBuilder.substring(1).toString() + ')',
			null);
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    /**
     * 删除表中所有数据
     * 
     * @return 删除数据的数量 此返回值不会低于0
     */
    public int del() {
	int result = 0;
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    result = mDatabase.delete(mTableName, null, null);
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    /**
     * 根据条件删除表中数据
     * 
     * @param whereClause
     * @param whereArgs
     * @return 删除数据的数量 此返回值不会低于0
     */
    public int del(String whereClause, Object[] whereArgs) {
	int result = 0;
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    result = mDatabase.delete(mTableName, whereClause, buildSelectionArgs(whereArgs));
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
	return result;
    }

    public void rawQuery(String sql, Object[] selectionArgs, rawQueryCallback callback) {
	synchronized (LOCK) {
	    try {
		mSqLiteHelper = new SQLiteHelper();
		mDatabase = mSqLiteHelper.getWritableDatabase();
		try {
		    mDatabase.beginTransaction();
		    mCursor = mDatabase.rawQuery(sql, buildSelectionArgs(selectionArgs));
		    if (callback != null)
			callback.processCursor(mCursor);
		    mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
		    throw e;
		} finally {
		    closeCursor();
		    endTransaction();
		}
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
	    } finally {
		mDatabase.close();
	    }
	}
    }

    public interface rawQueryCallback {
	public void processCursor(Cursor cursor);
    }

    class SQLiteHelper extends SQLiteOpenHelper {

	SQLiteHelper() {
	    super(Broid.getApplication(), mTable.database(), null, mVersion);
	    Log.d(Broid.TAG, mTable.database() + " version is " + mVersion, null);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    String sql;
	    if (mAlterFields != null) {
		final int size = mAlterFields.size();
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
	    for (int i = 0; i < mSize; i++) {
		mFields.get(i).setAccessible(true);
		mColumn = mFields.get(i).getAnnotation(Column.class);
		if (mColumn != null) {
		    if (mColumn.ignore()) {
			continue;
		    }
		    s.append(mFields.get(i).getName());
		    initField(s, mFields.get(i));
		    if (mColumn.isAutoKey()) {
			s.append("PRIMARY KEY AUTOINCREMENT");
			mAutoKeyField = mFields.get(i);
		    } else if (!TextUtils.isEmpty(mColumn.value())) {
			s.append("DEFAULT ").append(mColumn.value());
		    }
		} else {
		    s.append(mFields.get(i).getName());
		    initField(s, mFields.get(i));
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
	    mColumn = alterField.getAnnotation(Column.class);
	    if (mColumn != null && !TextUtils.isEmpty(mColumn.value())) {
		sql.append("DEFAULT ").append(mColumn.value());
	    }
	    return sql.toString();
	}

	private void initField(StringBuilder s, Field field) {
	    if (field == mAutoKeyField || int.class == field.getType()
		|| Integer.class.isAssignableFrom(field.getType())) {
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
		Log.w(Broid.TAG, "can't read the type'" + field.getType() + "' , and the column type will be TEXT",
		    null);
		s.append(" TEXT ");
	    }
	}
    }

}
