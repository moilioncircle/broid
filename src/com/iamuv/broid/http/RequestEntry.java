package com.iamuv.broid.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import com.iamuv.broid.Broid;
import com.iamuv.broid.Log;

import android.text.TextUtils;

public class RequestEntry {

	private String urlFieldName;

	private RequestMethod mode;
	private String url;
	private ArrayList<Param> params;
	private int connTimeOut;
	private int soTimeOut;
	private boolean cache;
	private String encode;
	private long session;

	public RequestMethod getMode() {
		return mode;
	}

	public String getUrl() {
		return url;
	}

	public ArrayList<Param> getParams() {
		return params;
	}

	public String getParamsStr() {
		StringBuffer stringBuffer = new StringBuffer("");
		if (getParams().size() > 0) {
			for (Param param : getParams()) {
				stringBuffer.append('&');
				stringBuffer.append(param.getName());
				stringBuffer.append('=');
				stringBuffer.append(param.getValue());
			}
			if (stringBuffer.length() > 0) {
				stringBuffer.setCharAt(0, '?');
			}
		}
		return stringBuffer.toString();
	}

	public int getConnTimeOut() {
		return connTimeOut;
	}

	public int getSoTimeOut() {
		return soTimeOut;
	}

	public boolean isCache() {
		return cache;
	}

	public String getEncode() {
		return encode;
	}

	public long getSession() {
		return session;
	}

	public class Param {
		private String name;
		private String value;

		void setName(String name) {
			this.name = name;
		}

		void setValue(String value) {
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}

	public enum RequestMethod {
		GET, POST;
	}

	public static final int MAX_CONNECTION_TIMEOUT = 30 * 1000;
	public static final int MAX_SO_TIMEOUT = 30 * 1000;

	public RequestEntry(Object c) {

		Object value = null;
		try {
			HttpRequest httpRequest = c.getClass().getAnnotation(HttpRequest.class);
			if (httpRequest != null) {
				urlFieldName = httpRequest.url();
				session = httpRequest.session();
				if (TextUtils.isEmpty(urlFieldName))
					throw new Exception("do not set the url field name");
				if (httpRequest.mode().toUpperCase(Locale.getDefault()).indexOf("GET") == 0) {
					mode = RequestMethod.GET;
				} else if (httpRequest.mode().toUpperCase(Locale.getDefault()).indexOf("POST") == 0) {
					mode = RequestMethod.POST;
				} else
					throw new Exception("can not read the mode '" + httpRequest.mode() + "'");
				if (TextUtils.isEmpty(httpRequest.charset())) {
					encode = "UTF-8";
				} else
					encode = httpRequest.charset();
				connTimeOut = httpRequest.conn_timeout() > RequestEntry.MAX_CONNECTION_TIMEOUT ? RequestEntry.MAX_CONNECTION_TIMEOUT
						: httpRequest.conn_timeout();
				soTimeOut = httpRequest.so_timeout() > RequestEntry.MAX_SO_TIMEOUT ? RequestEntry.MAX_SO_TIMEOUT : httpRequest
						.conn_timeout();
				cache = httpRequest.cache();
				params = new ArrayList<Param>();
				Param param = null;
				for (Field field : c.getClass().getDeclaredFields()) {
					if (field.getName().equals(urlFieldName)) {
						value = field.get(c);
						if (c != null) {
							url = String.valueOf(field.get(c));
						} else
							new Exception("http url is null");
					} else {
						param = new Param();
						param.setName(field.getName());
						value = field.get(c);
						param.setValue(value == null ? "" : String.valueOf(value));
						params.add(param);
					}
				}
			} else
				throw new Exception("can not find the annotation 'HttpRequest'");
			if (TextUtils.isEmpty(url))
				throw new Exception("http url is null");
		} catch (Exception e) {
			throw new RuntimeException(e.getLocalizedMessage());
		}

	}

	/**
	 * Http请求参数映射类注解
	 * 
	 * @author Uv
	 * @date 2014-6-3
	 * @email uv@iamuv.com
	 * @blog http://www.iamuv.com
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface HttpRequest {

		public String mode() default "GET";

		public boolean cache() default true;

		public long session() default 60 * 1000;

		public int conn_timeout() default MAX_CONNECTION_TIMEOUT - 20 * 1000;

		public int so_timeout() default MAX_SO_TIMEOUT - 20 * 1000;

		public String url() default "url";

		public String charset() default "UTF-8";

	}

}
