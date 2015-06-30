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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.message.BasicNameValuePair;

import android.text.TextUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.annotation.Ignore;
import com.iamuv.broid.http.HttpRequest.Method;
import com.iamuv.broid.utils.EmptyUtils;

public class HttpRequestEntry {

	private RequestMethod mode;
	private String url;
	private int connTimeOut;
	private int soTimeOut;
	private boolean cache;
	private long session = 10 * 60;
	private String charset;

	private ArrayList<BasicNameValuePair> mParams;
	private String mParamsStr;

	private UUID id;

	private volatile boolean interrupt;

	private Ignore mIgnore;

	private BasicCookieStore mCookieStore;

	public RequestMethod getMode() {
		return mode;
	}

	public String getUrl() {
		return url;
	}

	public ArrayList<BasicNameValuePair> getParams() {
		return mParams;
	}

	public String getParamsStr() {
		return mParamsStr;
	}

	public int getConnTimeOut() {
		return connTimeOut;
	}

	public int getSoTimeOut() {
		return soTimeOut;
	}

	public String getCharset() {
		return charset;
	}

	public boolean isCache() {
		return cache;
	}

	public long getSession() {
		return session;
	}

	public String getId() {
		return id.toString();
	}

	public enum RequestMethod {
		GET, POST;
	}

	public void interrupt() {
		interrupt = true;
	}

	public boolean isInterrupt() {
		return interrupt;
	}

	public static final int MAX_CONNECTION_TIMEOUT = 30 * 1000;
	public static final int MAX_SOCKET_TIMEOUT = 30 * 1000;

	@SuppressWarnings("unchecked")
	public HttpRequestEntry(HttpRequest request) {
		interrupt = false;
		StringBuilder builder = null;
		try {
			if (request != null) {
				url = request.getUrl();
				if (request.getHttpCacheSession() > 0)
					session = request.getHttpCacheSession();
				if (request.getMethod().equals(Method.GET)) {
					mode = RequestMethod.GET;
				} else if (request.getMethod().equals(Method.POST)) {
					mode = RequestMethod.POST;
				} else
					throw new Exception("can not read the mode '" + request.getMethod().name() + "'");
				if (TextUtils.isEmpty(request.getCharset())) {
					charset = "UTF-8";
				} else
					charset = request.getCharset();
				connTimeOut = request.getConnTimeOut() > HttpRequestEntry.MAX_CONNECTION_TIMEOUT ? HttpRequestEntry.MAX_CONNECTION_TIMEOUT
						: request.getConnTimeOut();
				soTimeOut = request.getSoTimeOut() > HttpRequestEntry.MAX_SOCKET_TIMEOUT ? HttpRequestEntry.MAX_SOCKET_TIMEOUT : request
						.getSoTimeOut();
				cache = request.isCache();

				builder = new StringBuilder("");
				mParams = new ArrayList<BasicNameValuePair>();
				BasicNameValuePair pair = null;
				Object obj = null;
				String name = null;
				String value = null;
				for (Field field : request.getClass().getDeclaredFields()) {
					field.setAccessible(true);
					if (Modifier.isStatic(field.getModifiers()))
						continue;
					mIgnore = field.getAnnotation(Ignore.class);
					if (mIgnore != null)
						continue;
					name = field.getName();
					obj = field.get(request);
					if (field.getType().isAssignableFrom(List.class)) {
						List list = (List) obj;
						for (Object o : list) {
							value = new String(String.valueOf(o).getBytes(), request.getCharset());
							pair = new BasicNameValuePair(name, value);
							mParams.add(pair);
							builder.append('&').append(name).append('=').append(value);
						}
					} else if (field.getType().isAssignableFrom(ArrayList.class)) {
						ArrayList list = (ArrayList) obj;
						if (!EmptyUtils.isEmpty(list)) {
							for (Object o : list) {
								value = new String(String.valueOf(o).getBytes(), request.getCharset());
								pair = new BasicNameValuePair(name, value);
								mParams.add(pair);
								builder.append('&').append(name).append('=').append(value);
							}
						}
					} else {
						if (obj != null) {
							value = new String(String.valueOf(obj).getBytes(), request.getCharset());
							pair = new BasicNameValuePair(name, value);
							mParams.add(pair);
							builder.append('&').append(name).append('=').append(value);
						}
					}
				}
				if (builder.length() > 0)
					builder.setCharAt(0, '?');
				mParamsStr = builder.toString();

				if (!EmptyUtils.isEmpty(request.getCookie())) {
					mCookieStore = new BasicCookieStore();
					BasicClientCookie2 cookie = null;
					String cookieValue = null;
					final HashMap<String, String> cookies = request.getCookie();
					for (String key : cookies.keySet()) {
						value = cookies.get(key);
						if (!TextUtils.isEmpty(value))
							cookie = new BasicClientCookie2(key, cookieValue);
						mCookieStore.addCookie(cookie);
					}
				}
			} else
				throw new Exception("can not find the annotation 'HttpRequestEntry'");
			if (TextUtils.isEmpty(url))
				throw new Exception("http url is null");
			builder = new StringBuilder();
			builder.append(getUrl()).append(getParamsStr()).append(getMode()).append(getConnTimeOut()).append(getSoTimeOut())
					.append(getCharset()).append(getSession());
			id = UUID.nameUUIDFromBytes(builder.toString().getBytes("UTF-8"));
		} catch (Exception e) {
			Log.e(Broid.TAG, null, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof HttpRequestEntry) {
			HttpRequestEntry entry = (HttpRequestEntry) o;
			return (entry.getId().equals(getId()));
		} else
			return false;
	}

	public CookieStore getCookieStore() {
		return mCookieStore;
	}

}
