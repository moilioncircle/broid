package com.iamuv.broid;

import java.util.Locale;

import android.text.TextUtils;

public class Utils {
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
			return null;
	}

	public static byte[] hexStringToBytes(String hexString) {
		byte[] bs = null;
		if (!TextUtils.isEmpty(hexString)) {
			bs = new byte[hexString.length() / 2];
			char[] hexChars = hexString.toUpperCase(Locale.getDefault()).toCharArray();
			for (int i = 0; i < bs.length; i++) {
				bs[i] = (byte) ((byte) "0123456789ABCDEF".indexOf(hexChars[i * 2]) << 4 | (byte) "0123456789ABCDEF"
						.indexOf(hexChars[i * 2 + 1]));
			}
		}
		return bs;
	}
}