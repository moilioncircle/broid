package com.iamuv.broid.storage;

import java.util.HashMap;

/**
 * 存储实体类工厂
 * 
 * @author Uv
 * @date 2014-6-3
 * @email uv@iamuv.com
 * @blog http://www.iamuv.com
 */
public class StorageFactory {

	private static HashMap<String, SQLiteStorage<?>> SQLITE_STORAGE_CACHE;
	private static HashMap<String, PreferencesStorage<?>> PREFERENCES_STORAGE_CACHE;

	public StorageFactory() {
		SQLITE_STORAGE_CACHE = new HashMap<String, SQLiteStorage<?>>();
		PREFERENCES_STORAGE_CACHE = new HashMap<String, PreferencesStorage<?>>();
	}

	@SuppressWarnings("unchecked")
	public final <T> SQLiteStorage<T> getSQLiteStorage(Class<T> type) {
		SQLiteStorage<T> storage = (SQLiteStorage<T>) SQLITE_STORAGE_CACHE.get(type.getName());
		if (storage == null) {
			synchronized (SQLITE_STORAGE_CACHE) {
				storage = new SQLiteStorage<T>(type);
				SQLITE_STORAGE_CACHE.put(type.getName(), storage);
			}
		}
		return storage;
	}

	@SuppressWarnings("unchecked")
	public final <T> PreferencesStorage<T> getPreferencesStorage(Class<T> type) {
		PreferencesStorage<T> storage = (PreferencesStorage<T>) PREFERENCES_STORAGE_CACHE.get(type);
		if (storage == null) {
			synchronized (PREFERENCES_STORAGE_CACHE) {
				storage = new PreferencesStorage<T>(type);
				PREFERENCES_STORAGE_CACHE.put(type.getName(), storage);
			}
		}
		return storage;
	}
}
