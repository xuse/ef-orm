package jef.tools;

public class Strings {
	public static String upper(String input) {
		// 半角转全角：
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			int x=c[i];
			if (x>='a' && x<='z') {
				c[i]=(char)(x-32);
			}
		}
		return new String(c);

	}

	public static String lower(String input) {
		// 半角转全角：
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			int x=c[i];
			if (x>='A' && x<='Z') {
				System.out.println((char)x);
				c[i]=(char)(x-32);
			}
		}
		return new String(c);
	}

}
