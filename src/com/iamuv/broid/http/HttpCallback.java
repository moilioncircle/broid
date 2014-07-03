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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;
import com.iamuv.broid.utils.EmptyUtils;

/**
 * Http短时异步请求回调 此类的回调方法与初始化方法为同一线程
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about HttpCallback.java">uv@iamuv.com</a> <br>
 *         2014-6-5
 * 
 * @param <Result>
 */
public abstract class HttpCallback<Result> {

    private Class<?> mType;
    private Handler mHandler;
    private Result mResult;
    private Object mTag;
    private boolean flag;

    @SuppressWarnings("unchecked")
    public HttpCallback() {
	mHandler = currentHandler();
	flag = false;
	try {
	    mType = (Class<Result>) ((ParameterizedType) this.getClass().getGenericSuperclass())
		.getActualTypeArguments()[0];
	} catch (Exception e) {
	    mType = null;
	}
	if (mType == null) {
	    try {
		Type type = this.getClass().getGenericSuperclass();
		String typeName = type.toString();
		int i = typeName.indexOf("java.util.List");
		if (i != -1) {
		    typeName = typeName.substring(i + 15, typeName.length() - 2);
		    if (!EmptyUtils.isEmpty(EmptyUtils.getUnNullString(typeName))) {
			mType = Class.forName(typeName);
			flag = true;
		    } else
			throw new Exception();
		} else
		    throw new Exception();
	    } catch (Exception e) {
		Log.w(Broid.TAG, null, e);
		mType = null;
	    }
	}

    }

    protected abstract void onComplete(Result result);

    protected abstract void onError(Part part, Throwable tr);

    protected void onCancel() {}

    public final Class<?> getType() {
	return mType;
    }

    @SuppressWarnings("unchecked")
    protected Result processResult(String resultStr) throws Exception {
	Log.d("result is\r\n" + resultStr);
	Result result = null;
	try {
	    if (mType == null)
		throw new Exception("can not find the generic class type");
	    if (resultStr == null)
		throw new Exception("result is null");
	    if (String.class == mType) {
		result = (Result) resultStr;
	    } else if (flag) {
		result = (Result) JSON.parseArray(resultStr, mType);
	    } else
		result = (Result) JSON.parseObject(resultStr, mType);
	    if (result == null)
		throw new Exception("can not format result");
	} catch (Exception e) {
	    Log.w(Broid.TAG, null, e);
	    throw e;
	}
	return result;

    }

    protected final void complete(String result) {
	try {
	    mResult = processResult(result);
	    if (mHandler != null) {
		mHandler.post(new Runnable() {

		    @Override
		    public void run() {
			onComplete(mResult);
		    }
		});
	    } else
		onComplete(mResult);
	} catch (Exception e) {
	    Log.w(Broid.TAG, null, e);
	    error(Part.CALLBACK, e);
	}
    }

    protected final void cancel() {
	try {
	    if (mHandler != null) {
		mHandler.post(new Runnable() {

		    @Override
		    public void run() {
			onCancel();
		    }
		});
	    } else
		onCancel();
	} catch (Exception e) {
	    Log.w(Broid.TAG, null, e);
	    error(Part.CALLBACK, e);
	}
    }

    protected final void error(final Part part, final Throwable tr) {
	if (mHandler != null) {
	    mHandler.post(new Runnable() {

		@Override
		public void run() {
		    onError(part, tr);
		}
	    });
	} else
	    onError(part, tr);
    }

    protected final static Handler currentHandler() {
	try {
	    Looper loop = Looper.myLooper();
	    return loop == null ? null : new Handler(loop);
	} catch (Exception e) {
	    return null;
	}
    }

    public enum Part {
	PREPARE, REQUEST, CALLBACK;
    }

    public void setTag(final Object tag) {
	mTag = tag;
    }

    public Object getTag() {
	return mTag;
    }

}