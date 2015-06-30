package com.iamuv.broid.test;

import java.util.HashMap;

public class Test {

	public static void main(String[] args) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("", "空串");
		map.put(null, "null");

		System.out.println(map.get(""));
		System.out.println(map.get(null));

	}

}