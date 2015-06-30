package com.iamuv.broid.http;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class HttpResult implements Serializable {

	private static final long serialVersionUID = 1L;

	public HttpResult() {}

	public HttpResult(byte[] result, byte[] cookie) {
		if (result != null)
			mResult = new String(result);
		if (cookie != null)
			mCookies = genCookieMap(new String(cookie));
	}

	public String mResult;

	public HashMap<String, String> mCookies;

	public byte[] getCookieByte() {
		try {
			return genCookieString().getBytes("UTF-8");
		} catch (Exception e) {
			return new byte[0];
		}
	}

	public byte[] getResultByte() {
		try {
			return mResult.getBytes("UTF-8");
		} catch (Exception e) {
			return new byte[0];
		}
	}

	private HashMap<String, String> genCookieMap(String cookieStr) {
		HashMap<String, String> result = new HashMap<String, String>();
		if (!TextUtils.isEmpty(cookieStr)) {
			JSONObject jsonObject = JSON.parseObject(cookieStr);
			if (jsonObject != null) {
				Set<String> keys = jsonObject.keySet();
				if (keys != null && keys.size() > 0) {
					for (String key : keys) {
						result.put(key, jsonObject.getString(key));
					}
				}
			}
		}
		return result;
	}

	private String genCookieString() {
		return JSON.toJSONString(mCookies);
	}

}