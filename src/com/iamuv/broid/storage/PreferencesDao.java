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
import java.util.Locale;
import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.annotation.Preferences;
import com.iamuv.broid.annotation.PreferencesPair;

/**
 * SharedPreferences存储
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about PreferencesDao.java">uv@iamuv.com</a> <br>
 *         2014-6-5
 * 
 * @param <T>
 */
public class PreferencesDao<T> {

    private SharedPreferences mSharedPreferences;

    private Class<T> mType;

    private ArrayList<Field> mFields = new ArrayList<Field>();

    private Preferences mPreferences;

    private PreferencesPair mPair;

    private final int mSize;

    public PreferencesDao(Class<T> cla) {
	mType = cla;
	initFields();
	mSize = mFields.size();
	if (mSize == 0) {
	    Log.w(Broid.TAG, "can not find any fields in the class " + mType.getSimpleName(), null);
	}
	mPreferences = mType.getAnnotation(Preferences.class);
	if (mPreferences != null) {
	    String m = mPreferences.mode().toUpperCase(Locale.getDefault());
	    int mode = 0x0000;
	    if (m.indexOf("APPEND") > 0) {
		mode = 0x8000;
	    } else if (m.indexOf("MULTI") > 0) {
		mode = 0x0004;
	    } else if (m.indexOf("ENABLE") > 0) {
		mode = 0x0008;
	    }
	    mSharedPreferences = Broid.getApplication().getSharedPreferences(mType.getSimpleName(), mode);
	} else
	    throw new DaoException("can not find the class with the annotation 'Preferences'");
    }

    private void initFields() {
	Field[] fields = mType.getDeclaredFields();
	final int length = fields.length;
	for (int i = 0; i < length; i++) {
	    if (Modifier.isStatic(fields[i].getModifiers()))
		continue;
	    mPair = fields[i].getAnnotation(PreferencesPair.class);
	    if (mPair != null && mPair.ignore())
		continue;
	    fields[i].setAccessible(true);
	    mFields.add(fields[i]);
	}
    }

    public final T get() {
	T t = null;
	try {
	    t = mType.newInstance();
	    final Map<String, ?> all = mSharedPreferences.getAll();
	    Object value;
	    PreferencesPair preferenceField;
	    for (int i = 0; i < mSize; i++) {
		mFields.get(i).setAccessible(true);
		preferenceField = mFields.get(i).getAnnotation(PreferencesPair.class);
		value = all.get(mFields.get(i).getName());
		if (boolean.class == mFields.get(i).getType()) {
		    if (value == null) {
			if (preferenceField != null && !TextUtils.isEmpty(preferenceField.value())) {
			    value = Boolean.parseBoolean(preferenceField.value());
			} else
			    value = false;
		    }
		    mFields.get(i).setBoolean(t, Boolean.parseBoolean(String.valueOf(value)));
		} else if (long.class == mFields.get(i).getType()) {
		    if (value == null) {
			if (preferenceField != null && !TextUtils.isEmpty(preferenceField.value())) {
			    try {
				value = Long.parseLong(preferenceField.value());
			    } catch (Exception e) {
				value = 0;
			    }
			} else
			    value = 0;
		    }
		    mFields.get(i).setLong(t, Long.parseLong(String.valueOf(value)));
		} else if (float.class == mFields.get(i).getType()) {
		    if (value == null) {
			if (preferenceField != null && !TextUtils.isEmpty(preferenceField.value())) {
			    try {
				value = Float.parseFloat(preferenceField.value());
			    } catch (Exception e) {
				value = 0;
			    }
			} else
			    value = 0;
		    }
		    mFields.get(i).setFloat(t, Float.parseFloat(String.valueOf(value)));
		} else if (int.class == mFields.get(i).getType()) {
		    if (value == null) {
			if (preferenceField != null && !TextUtils.isEmpty(preferenceField.value())) {
			    try {
				value = Integer.parseInt(preferenceField.value());
			    } catch (Exception e) {
				value = 0;
			    }
			} else
			    value = 0;
		    }
		    mFields.get(i).setInt(t, Integer.parseInt(String.valueOf(value)));
		} else if (String.class == mFields.get(i).getType()) {
		    if (value == null) {
			if (preferenceField != null && preferenceField.value() != null) {
			    value = preferenceField.value();
			} else
			    value = "";
		    }
		    mFields.get(i).set(t, String.valueOf(value));
		}
	    }
	} catch (InstantiationException e) {
	    Log.w(Broid.TAG, null, e);
	} catch (IllegalAccessException e) {
	    Log.w(Broid.TAG, null, e);
	}

	return t;
    }

    public final void save(T c) {
	Editor editor = mSharedPreferences.edit();
	for (int i = 0; i < mSize; i++) {
	    try {
		mFields.get(i).setAccessible(true);
		if (boolean.class == mFields.get(i).getType()) {
		    editor.putBoolean(mFields.get(i).getName(), mFields.get(i).getBoolean(c));
		} else if (long.class == mFields.get(i).getType()) {
		    editor.putLong(mFields.get(i).getName(), mFields.get(i).getLong(c));
		} else if (float.class == mFields.get(i).getType()) {
		    editor.putFloat(mFields.get(i).getName(), mFields.get(i).getFloat(c));
		} else if (int.class == mFields.get(i).getType()) {
		    editor.putInt(mFields.get(i).getName(), mFields.get(i).getInt(c));
		} else if (String.class == mFields.get(i).getType()) {
		    editor.putString(mFields.get(i).getName(), String.valueOf(mFields.get(i).get(c)));
		}
	    } catch (IllegalAccessException e) {
		Log.w(Broid.TAG, null, e);
	    }
	}
	editor.commit();

    }
}
