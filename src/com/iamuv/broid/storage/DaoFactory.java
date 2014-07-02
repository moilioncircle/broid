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

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储实体类工厂
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about DaoFactory.java">uv@iamuv.com</a> <br>
 *         2014-6-5
 * 
 */
public class DaoFactory {

    private static ConcurrentHashMap<String, SQLiteDao<?>> SQLITE_DAO_CACHE;
    private static ConcurrentHashMap<String, PreferencesDao<?>> PREFERENCES_DAO_CACHE;

    public DaoFactory() {
	SQLITE_DAO_CACHE = new ConcurrentHashMap<String, SQLiteDao<?>>();
	PREFERENCES_DAO_CACHE = new ConcurrentHashMap<String, PreferencesDao<?>>();
    }

    @SuppressWarnings("unchecked")
    public final <T> SQLiteDao<T> getSQLiteDao(Class<T> type) {
	SQLiteDao<T> dao = (SQLiteDao<T>) SQLITE_DAO_CACHE.get(type.getName());
	if (dao == null) {
	    dao = new SQLiteDao<T>(type);
	    SQLITE_DAO_CACHE.put(type.getName(), dao);
	}
	return dao;
    }

    @SuppressWarnings("unchecked")
    public final <T> PreferencesDao<T> getPreferencesDao(Class<T> type) {
	PreferencesDao<T> dao = (PreferencesDao<T>) PREFERENCES_DAO_CACHE.get(type);
	if (dao == null) {
	    dao = new PreferencesDao<T>(type);
	    PREFERENCES_DAO_CACHE.put(type.getName(), dao);
	}
	return dao;
    }
}
