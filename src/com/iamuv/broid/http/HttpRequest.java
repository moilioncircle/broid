package com.iamuv.broid.http;

import java.util.HashMap;

import com.iamuv.broid.annotation.Ignore;

public abstract class HttpRequest {

	public static enum Method {
		POST, GET;
	}

	@Ignore
	private boolean cache = true;
	@Ignore
	private String charset = "UTF-8";
	@Ignore
	private Method method = Method.GET;
	@Ignore
	private long session;
	@Ignore
	private int connTimeOut;
	@Ignore
	private int soTimeOut;
	@Ignore
	private String url;
	@Ignore
	private HashMap<String, String> cookie = new HashMap<String, String>();

	/**
	 * 是否使用http缓存
	 * @return
	 */
	public boolean isCache() {
		return cache;
	}

	/**
	 * 设置是否使用http缓存
	 * @param cache
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * http请求字符集 默认为UTF-8
	 * @return 
	 */
	public String getCharset() {
		return charset;
	}

	/**
	 * 设置http请求字符集 默认为UTF-8
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * http请求方法
	 * @return
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * 设置Http请求方法
	 * @param method
	 */
	public void setMethod(Method method) {
		this.method = method;
	}

	/**
	 * 获取http缓存留存时间 单位是秒
	 * @return
	 */
	public long getHttpCacheSession() {
		return session;
	}

	/**
	 * 设置http缓存留存时间 单位是秒
	 * @return
	 */
	public void setHttpCacheSession(long session) {
		this.session = session;
	}

	/**
	 * 获取连接超时时间 单位为毫秒
	 * @return
	 */
	public int getConnTimeOut() {
		return connTimeOut;
	}

	/**
	 * 设置连接超时时间 单位为毫秒
	 * @return
	 */
	public void setConnTimeOut(int connTimeOut) {
		this.connTimeOut = connTimeOut;
	}

	/**
	 * 获取读取超时时间 单位为毫秒
	 * @return
	 */
	public int getSoTimeOut() {
		return soTimeOut;
	}

	/**
	 * 设置读取超时时间 单位为毫秒
	 * @return
	 */
	public void setSoTimeOut(int soTimeOut) {
		this.soTimeOut = soTimeOut;
	}

	/**
	 * 请求地址
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 设置请求地址
	 * @return
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	public HashMap<String, String> getCookie() {
		return cookie;
	}

	public void setCookie(HashMap<String, String> cookie) {
		if (cookie != null)
			this.cookie = cookie;
	}

	public void addCookie(String key, String value) {
		this.cookie.put(key, value);
	}

	public void clearCookie() {
		this.cookie.clear();
	}

	public void addCookie(HashMap<String, String> cookie) {
		this.cookie.putAll(cookie);
	}

}
