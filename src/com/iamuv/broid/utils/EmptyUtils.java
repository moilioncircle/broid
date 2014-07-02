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
package com.iamuv.broid.utils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;

/**
 * 校验是否为空工具类
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a href="mailto:uv@iamuv.com?subject=about EmptyUtils.java">uv@iamuv.
 *         com</a> <br>
 *         2014-6-5
 * 
 */
public class EmptyUtils {

    /**
     * 校验数据是否为空
     * 
     * @param sourceArray
     * @return
     */
    public static <V> boolean isEmpty(V[] sourceArray) {
	return (sourceArray == null || sourceArray.length == 0);
    }

    /**
     * 校验List是否为空
     * 
     * @param sourceList
     * @return
     */
    public static <V> boolean isEmpty(List<V> sourceList) {
	return (sourceList == null || sourceList.size() == 0);
    }

    /**
     * 校验Map是否为空
     * 
     * @param sourceMap
     * @return
     */
    public static <K, V> boolean isEmpty(Map<K, V> sourceMap) {
	return (sourceMap == null || sourceMap.size() == 0);
    }

    /**
     * 校验String是否为空
     * 
     * @param sourceString
     * @return
     */
    public static boolean isEmpty(String sourceString) {
	return (sourceString == null || sourceString.length() == 0);
    }

    /**
     * 校验String是由空格组成
     * 
     * @param sourceString
     * @return
     */
    public static boolean isBlank(String str) {
	return (str == null || str.trim().length() == 0);
    }

    /**
     * 校验JSONArray是否为空
     * 
     * @param sourceJSONArray
     * @return
     */
    public static boolean isEmpty(JSONArray sourceJSONArray) {
	return (sourceJSONArray == null || sourceJSONArray.length() == 0);
    }

    /**
     * 获取非空,非空格,非"null"字符串
     * 
     * @param str 源字符串
     * @return trim后的字符串或者空串
     */
    public static String getUnNullString(String str) {
	if (isBlank(str)) {
	    return "";
	} else if (str.trim().toLowerCase(Locale.ENGLISH).equals("null")) {
	    return "";
	} else
	    return str.trim();
    }

}