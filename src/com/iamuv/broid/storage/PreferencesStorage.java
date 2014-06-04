package com.iamuv.broid.storage;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.storage.StorageEntry.PreferenceField;
import com.iamuv.broid.storage.StorageEntry.Preferences;

/**
 * SharedPreferences存储
 * 
 * @author Uv
 * @date 2014-6-3
 * @email uv@iamuv.com
 * @blog http://www.iamuv.com
 * @param <T>
 */
public class PreferencesStorage<T> {

	private SharedPreferences mSharedPreferences;

	private Class<T> mType;

	private Field[] mFields;

	public PreferencesStorage(Class<T> c) {
		mType = c;
		mFields = mType.getDeclaredFields();
		Preferences prefrernces = mType.getAnnotation(Preferences.class);
		if (prefrernces != null) {
			String m = prefrernces.mode().toUpperCase(Locale.getDefault());
			int mode = 0x0000;
			if (m.indexOf("APPEND") > 0) {
				mode = 0x8000;
			} else if (m.indexOf("MULTI") > 0) {
				mode = 0x0004;
			} else if (m.indexOf("ENABLE") > 0) {
				mode = 0x0008;
			}
			mSharedPreferences = Broid.getApplication().getSharedPreferences(c.getSimpleName(), mode);
		}
	}

	public final T get() {
		T t = null;
		try {
			t = mType.newInstance();
			final Map<String, ?> all = mSharedPreferences.getAll();
			Object value;
			int length = mFields.length;
			PreferenceField preferenceField;
			for (int i = 0; i < length; i++) {
				mFields[i].setAccessible(true);
				preferenceField = mFields[i].getAnnotation(PreferenceField.class);
				value = all.get(mFields[i].getName());
				if (boolean.class == mFields[i].getType()) {
					if (value == null) {
						if (preferenceField != null && !TextUtils.isEmpty(preferenceField.value())) {
							value = Boolean.parseBoolean(preferenceField.value());
						} else
							value = false;
					}
					mFields[i].setBoolean(t, Boolean.parseBoolean(String.valueOf(value)));
				} else if (long.class == mFields[i].getType()) {
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
					mFields[i].setLong(t, Long.parseLong(String.valueOf(value)));
				} else if (float.class == mFields[i].getType()) {
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
					mFields[i].setFloat(t, Float.parseFloat(String.valueOf(value)));
				} else if (int.class == mFields[i].getType()) {
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
					mFields[i].setInt(t, Integer.parseInt(String.valueOf(value)));
				} else if (String.class == mFields[i].getType()) {
					if (value == null) {
						if (preferenceField != null && preferenceField.value() != null) {
							value = preferenceField.value();
						} else
							value = "";
					}
					mFields[i].set(t, String.valueOf(value));
				}
			}
		} catch (InstantiationException e) {
			Log.w(Broid.TAG, null, e);
		} catch (IllegalAccessException e) {
			Log.w(Broid.TAG, null, e);
		}

		return t;
	}

	public final void save(Class<T> c) {
		Editor editor = mSharedPreferences.edit();
		int length = mFields.length;
		for (int i = 0; i < length; i++) {
			try {
				mFields[i].setAccessible(true);
				if (boolean.class == mFields[i].getType()) {
					editor.putBoolean(mFields[i].getName(), mFields[i].getBoolean(c));
				} else if (long.class == mFields[i].getType()) {
					editor.putLong(mFields[i].getName(), mFields[i].getLong(c));
				} else if (float.class == mFields[i].getType()) {
					editor.putFloat(mFields[i].getName(), mFields[i].getFloat(c));
				} else if (int.class == mFields[i].getType()) {
					editor.putInt(mFields[i].getName(), mFields[i].getInt(c));
				} else if (String.class == mFields[i].getType()) {
					editor.putString(mFields[i].getName(), String.valueOf(mFields[i].get(c)));
				}
			} catch (IllegalAccessException e) {
				Log.w(Broid.TAG, null, e);
			}
		}
		editor.commit();

	}
}
