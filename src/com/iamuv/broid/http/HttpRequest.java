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
import java.util.Locale;
import java.util.UUID;

import org.apache.http.message.BasicNameValuePair;

import android.text.TextUtils;

import com.iamuv.broid.annotation.HttpRequestEntry;

public class HttpRequest {

    private String urlFieldName;

    private Class<?> mType;

    private RequestMethod mode;
    private String url;
    private int connTimeOut;
    private int soTimeOut;
    private boolean cache;
    private long session;
    private String charset;

    private ArrayList<BasicNameValuePair> mParams;
    private String mParamsStr;

    private UUID id;

    private volatile boolean interrupt;

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

    public HttpRequest(Object c) {
	interrupt = false;
	StringBuilder builder = null;
	try {
	    mType = c.getClass();
	    HttpRequestEntry entry = mType.getAnnotation(HttpRequestEntry.class);
	    if (entry != null) {
		urlFieldName = entry.url();
		session = entry.session();
		if (TextUtils.isEmpty(urlFieldName))
		    throw new Exception("do not set the url field name");
		if (entry.mode().toUpperCase(Locale.getDefault()).indexOf("GET") == 0) {
		    mode = RequestMethod.GET;
		} else if (entry.mode().toUpperCase(Locale.getDefault()).indexOf("POST") == 0) {
		    mode = RequestMethod.POST;
		} else
		    throw new Exception("can not read the mode '" + entry.mode() + "'");
		if (TextUtils.isEmpty(entry.charset())) {
		    charset = "UTF-8";
		} else
		    charset = entry.charset();
		connTimeOut = entry.connectionTimeout() > HttpRequest.MAX_CONNECTION_TIMEOUT ? HttpRequest.MAX_CONNECTION_TIMEOUT
		    : entry.connectionTimeout();
		soTimeOut = entry.socketTimeout() > HttpRequest.MAX_SOCKET_TIMEOUT ? HttpRequest.MAX_SOCKET_TIMEOUT
		    : entry.connectionTimeout();
		cache = entry.cache();
		builder = new StringBuilder("");
		mParams = new ArrayList<BasicNameValuePair>();
		BasicNameValuePair pair = null;
		Object obj = null;
		String name = null;
		String value = null;
		for (Field field : mType.getDeclaredFields()) {
		    if (Modifier.isStatic(field.getModifiers()))
			continue;
		    if (field.getName().equals(urlFieldName)) {
			obj = field.get(c);
			if (obj != null) {
			    url = String.valueOf(obj);
			} else
			    new Exception("http url is null");
		    } else {
			name = field.getName();
			obj = field.get(c);
			value = obj == null ? "" : new String(String.valueOf(obj).getBytes(), entry.charset());
			pair = new BasicNameValuePair(name, value);
			mParams.add(pair);
			builder.append('&').append(name).append('=').append(value);
		    }
		}
		if (builder.length() > 0)
		    builder.setCharAt(0, '?');
		mParamsStr = builder.toString();
	    } else
		throw new Exception("can not find the annotation 'HttpRequestEntry'");
	    if (TextUtils.isEmpty(url))
		throw new Exception("http url is null");
	    builder = new StringBuilder();
	    builder.append(getUrl()).append(getParamsStr()).append(getMode()).append(getConnTimeOut())
		.append(getSoTimeOut()).append(getCharset()).append(getSession());
	    id = UUID.nameUUIDFromBytes(builder.toString().getBytes("UTF-8"));
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}

    }

    @Override
    public boolean equals(Object o) {
	if (o instanceof HttpRequest) {
	    HttpRequest entry = (HttpRequest) o;
	    return (entry.getId().equals(getId()));
	} else
	    return false;
    }

}
