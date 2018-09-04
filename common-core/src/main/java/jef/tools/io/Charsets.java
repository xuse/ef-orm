package jef.tools.io;

import java.nio.charset.Charset;

public final class Charsets {
	private Charsets() {
	}

	public static final Charset GB18030 = Charset.forName("GB18030");

	public static final Charset UTF8 = Charset.forName("UTF8");
	
	/**
	 * Null-safe charset lookup.
	 * @param charset
	 * @return
	 */
	public static Charset forName(String charset) {
		return charset == null ? null : Charset.forName(charset);
	}

}
