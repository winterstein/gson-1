package com.winterwell.utilslite;

public class Utils {

	public static RuntimeException runtime(Exception e) {
		if (e instanceof RuntimeException) return (RuntimeException) e;
		return new RuntimeException(e);
	}

}
