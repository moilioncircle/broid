package com.iamuv.broid.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.http.RequestEntry.Param;
import com.iamuv.broid.http.RequestEntry.RequestMethod;

public class HttpConn {

	private final RequestEntry mEntry;

	private BufferedReader mBufferedReader;
	private InputStreamReader mInputStreamReader;
	private StringBuffer mStringBuffer;
	private String mResult;
	private HttpClient mHttpClient;

	public HttpConn(final RequestEntry entry) {
		mEntry = entry;
		mHttpClient = new DefaultHttpClient();
		Log.d(Broid.TAG, "connection timeout is " + mEntry.getConnTimeOut(), null);
		mHttpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, mEntry.getConnTimeOut());
		Log.d(Broid.TAG, "so timeout is " + mEntry.getSoTimeOut(), null);
		mHttpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, mEntry.getSoTimeOut());
		Log.d(Broid.TAG, "encode is " + mEntry.getEncode(), null);
		mHttpClient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, mEntry.getEncode());
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
			HttpGet httpGet = new HttpGet();
			final String uri = mEntry.getUrl() + mEntry.getParamsStr();
			Log.d(Broid.TAG, "request url is\r\n" + uri, null);
			httpGet.setURI(new URI(uri));
			HttpResponse httpResponse = mHttpClient.execute(httpGet);
			mInputStreamReader = new InputStreamReader(httpResponse.getEntity().getContent());
			mBufferedReader = new BufferedReader(mInputStreamReader);
			mStringBuffer = new StringBuffer("");
			String line = null;
			while ((line = mBufferedReader.readLine()) != null) {
				mStringBuffer.append(line);
			}
			mResult = mStringBuffer.toString();
		} catch (URISyntaxException e) {
			Log.w(Broid.TAG, null, e);
		} catch (IOException e) {
			throw e;
		} finally {
			if (mBufferedReader != null) {
				try {
					mBufferedReader.close();
				} catch (IOException e) {
				}
			}
			if (mInputStreamReader != null) {
				try {
					mInputStreamReader.close();
				} catch (IOException e) {
				}
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
			if (mEntry.getParams().size() > 0) {
				List<NameValuePair> list = new ArrayList<NameValuePair>();
				Log.d(Broid.TAG, "request params is ", null);
				for (Param param : mEntry.getParams()) {
					Log.d(Broid.TAG, param.getName() + " " + param.getValue(), null);
					list.add(new BasicNameValuePair(param.getName(), param.getValue()));
				}
				request.setEntity(new UrlEncodedFormEntity(list));
			}

			HttpResponse response = mHttpClient.execute(request);
			mInputStreamReader = new InputStreamReader(response.getEntity().getContent());
			mBufferedReader = new BufferedReader(mInputStreamReader);
			mStringBuffer = new StringBuffer("");
			String line = null;
			while ((line = mBufferedReader.readLine()) != null) {
				mStringBuffer.append(line);
			}
			mResult = mStringBuffer.toString();

		} catch (URISyntaxException e) {
			Log.w(Broid.TAG, null, e);
		} catch (UnsupportedEncodingException e) {
			Log.w(Broid.TAG, null, e);
		} catch (IOException e) {
			throw e;
		} finally {
			if (mBufferedReader != null) {
				try {
					mBufferedReader.close();
				} catch (IOException e) {
				}
			}
			if (mInputStreamReader != null) {
				try {
					mInputStreamReader.close();
				} catch (IOException e) {
				}
			}
		}
		return mResult;
	}
}
