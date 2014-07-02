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

import java.util.Locale;

import android.text.TextUtils;

/**
 * 16进制工具类
 * 
 * @author <a href="http://www.iamuv.com" target="_blank">Uv</a> <br>
 *         <a
 *         href="mailto:uv@iamuv.com?subject=about HexUtils.java">uv@iamuv.com
 *         </a> <br>
 *         2014-6-5
 * 
 */
public class HexUtils {

    /**
     * 将字节数组转化为16进制字符串
     * 
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src) {
	StringBuilder stringBuilder = new StringBuilder();
	if (src != null && src.length > 0) {
	    for (int i = 0; i < src.length; i++) {
		int hex = src[i] & 0xFF;
		String hexStr = Integer.toHexString(hex);
		if (hexStr.length() < 2) {
		    stringBuilder.append(0);
		}
		stringBuilder.append(hexStr);
	    }
	    return stringBuilder.toString();
	} else
	    return "";
    }

    /**
     * 将16进制字符串转化为字节数组
     * 
     * @param hexString
     * @return
     */
    public static byte[] hexStringToBytes(String hexString) {
	byte[] bs = null;
	if (!TextUtils.isEmpty(hexString)) {
	    bs = new byte[hexString.length() / 2];
	    char[] hexChars = hexString.toUpperCase(Locale.getDefault())
		.toCharArray();
	    for (int i = 0; i < bs.length; i++) {
		bs[i] = (byte) ((byte) "0123456789ABCDEF"
		    .indexOf(hexChars[i * 2]) << 4 | (byte) "0123456789ABCDEF"
		    .indexOf(hexChars[i * 2 + 1]));
	    }
	}
	return bs;
    }
}