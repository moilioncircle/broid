package com.iamuv.broid.http;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;

/**
 * Http短时异步请求回调 此类的回调方法与初始化方法为同一线程 即如果初始化在UI线程中 那么回调方法则在UI线程中调用
 * 
 * @author Uv
 * @date 2014-6-3
 * @email uv@iamuv.com
 * @blog http://www.iamuv.com
 * @param <Result>
 */
public abstract class HttpCallback<Result> {

	private Class<Result> mType;
	private Handler mHandler;
	private Result mResult;

	@SuppressWarnings("unchecked")
	public HttpCallback() {
		mHandler = currentHandler();
		ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
		if (type != null) {
			Type[] types = type.getActualTypeArguments();
			if (types.length > 0) {
				mType = (Class<Result>) types[0];
			}
		}
	}

	protected abstract void onComplete(Result result);

	protected abstract void onError(Throwable tr);

	protected void onCancel() {
	}

	public final Class<Result> getType() {
		return mType;
	}

	@SuppressWarnings("unchecked")
	protected final void complete(String result) {
		try {
			if (mType == null) {
				throw new Exception("can not find the generic class type");
			}
			if (result == null)
				throw new Exception("result is null");
			if (String.class == mType) {
				mResult = (Result) result;
			} else
				mResult = JSON.parseObject(result, mType);
			if (mResult == null)
				throw new Exception("can not format result");
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
			error(e);
		}
	}

	protected final void cancel() {
		if (mHandler != null) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					onCancel();
				}
			});
		} else
			onCancel();
	}

	protected final void error(final Throwable tr) {
		if (mHandler != null) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					onError(tr);
				}
			});
		} else
			onError(tr);
	}

	private final static Handler currentHandler() {
		try {
			Looper loop = Looper.myLooper();
			return loop == null ? null : new Handler(loop);
		} catch (Exception e) {
			return null;
		}
	}

}