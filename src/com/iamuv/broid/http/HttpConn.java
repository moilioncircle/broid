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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.http.HttpRequest.RequestMethod;
import com.iamuv.broid.utils.EmptyUtils;

public class HttpConn {

    private final HttpRequest mEntry;

    private BufferedReader mBufferedReader;
    private InputStreamReader mInputStreamReader;
    private StringBuffer mStringBuffer;
    private String mResult;
    private HttpClient mHttpClient;

    public HttpConn(final HttpRequest entry) {
	mEntry = entry;
	mHttpClient = new DefaultHttpClient();
	Log.d(Broid.TAG, "connection timeout is " + mEntry.getConnTimeOut(), null);
	mHttpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, mEntry.getConnTimeOut());
	Log.d(Broid.TAG, "so timeout is " + mEntry.getSoTimeOut(), null);
	mHttpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, mEntry.getSoTimeOut());
	Log.d(Broid.TAG, "charset is " + mEntry.getCharset(), null);
	mHttpClient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, mEntry.getCharset());
    }

    public String exec() throws IOException {
	if (mEntry.getMode() == RequestMethod.GET) {
	    return get();
	} else {
	    return post();
	}
    }

    private String get() throws IOException {
	Log.i(Broid.TAG, "http get start", null);
	mResult = null;
	try {
	    final String uri = mEntry.getUrl() + mEntry.getParamsStr();
	    Log.d(Broid.TAG, "request url is\r\n" + uri, null);
	    HttpGet httpGet = new HttpGet();
	    httpGet.setURI(new URI(uri));
	    HttpResponse httpResponse = mHttpClient.execute(httpGet);
	    mInputStreamReader = new InputStreamReader(httpResponse.getEntity().getContent());
	    mBufferedReader = new BufferedReader(mInputStreamReader);
	    mStringBuffer = new StringBuffer("");
	    String line = null;
	    while ((line = mBufferedReader.readLine()) != null && !mEntry.isInterrupt()) {
		mStringBuffer.append(line);
	    }
	    if (!mEntry.isInterrupt())
		mResult = mStringBuffer.toString();
	} catch (URISyntaxException e) {
	    Log.w(Broid.TAG, null, e);
	} catch (IOException e) {
	    throw e;
	} finally {
	    if (mBufferedReader != null) {
		try {
		    mBufferedReader.close();
		} catch (IOException e) {}
	    }
	    if (mInputStreamReader != null) {
		try {
		    mInputStreamReader.close();
		} catch (IOException e) {}
	    }
	}
	return mResult;
    }

    private String post() throws IOException {
	Log.i(Broid.TAG, "http post start", null);
	mResult = null;
	try {
	    HttpPost request = new HttpPost();
	    Log.d(Broid.TAG, "request url is\r\n" + mEntry.getUrl(), null);
	    request.setURI(new URI(mEntry.getUrl()));
	    if (!EmptyUtils.isEmpty(mEntry.getParams())) {
		request.setEntity(new UrlEncodedFormEntity(mEntry.getParams(), mEntry.getCharset()));
		if (Broid.getDebugMode()) {
		    Log.d(Broid.TAG, "request entity is\r\n", null);
		    for (BasicNameValuePair param : mEntry.getParams()) {
			Log.d(Broid.TAG, param.getName() + " : " + param.getValue(), null);
		    }
		}
	    }
	    HttpResponse response = mHttpClient.execute(request);
	    if (response.getStatusLine().getStatusCode() == 200) {
		if (!mEntry.isInterrupt())
		    mResult = EntityUtils.toString(response.getEntity());
	    }
	} catch (URISyntaxException e) {
	    Log.w(Broid.TAG, null, e);
	} catch (IOException e) {
	    throw e;
	} finally {
	    if (mBufferedReader != null) {
		try {
		    mBufferedReader.close();
		} catch (IOException e) {}
	    }
	    if (mInputStreamReader != null) {
		try {
		    mInputStreamReader.close();
		} catch (IOException e) {}
	    }
	}
	return mResult;
    }
}
