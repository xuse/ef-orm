/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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
package jef.jre5support;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Enumeration;

/**
 * JRE 6中，Properties支持reader参数构造，因此可以支持各种非标准编码的properties文件。
 * 开发者将不再需要将properties文件用native2ascii转换为\\uxxxx格式的编码。
 * 而是可以直接以utf-8格式来存储properties并在其中使用各种语言。
 * 
 * 然而悲催的是，当时还有很多应用是使用JRE5的，因此当时我可以想到的办法就是，将JDK 6 的Properties拿出来，
 * 统一使用这个包下的来代替JDK中的。
 * 
 * 目前jef的主要支持对象逐渐从JDK 5-6转向JDK6-7。
 * 因此这个类的使用将逐渐减少，最终废弃。
 */
public class Properties extends java.util.Properties {
	private static final long serialVersionUID = 1L;
	/**
	 * Creates an empty property list with no default values.
	 */
	public Properties() {
		this(null);
	}

	/**
	 * Creates an empty property list with the specified defaults.
	 * 
	 * @param defaults
	 *            the defaults.
	 */
	public Properties(Properties defaults) {
		this.defaults = defaults;
	}

	public synchronized void load(Reader reader) throws IOException {
		load0(new LineReader(reader));
	}

	private void load0(LineReader lr) throws IOException {
		char[] convtBuf = new char[1024];
		int limit;
		int keyLen;
		int valueStart;
		char c;
		boolean hasSep;
		boolean precedingBackslash;

		while ((limit = lr.readLine()) >= 0) {
			c = 0;
			keyLen = 0;
			valueStart = limit;
			hasSep = false;

			precedingBackslash = false;
			while (keyLen < limit) {
				c = lr.lineBuf[keyLen];
				// need check if escaped.
				if ((c == '=' || c == ':') && !precedingBackslash) {
					valueStart = keyLen + 1;
					hasSep = true;
					break;
				} else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
					valueStart = keyLen + 1;
					break;
				}
				if (c == '\\') {
					precedingBackslash = !precedingBackslash;
				} else {
					precedingBackslash = false;
				}
				keyLen++;
			}
			while (valueStart < limit) {
				c = lr.lineBuf[valueStart];
				if (c != ' ' && c != '\t' && c != '\f') {
					if (!hasSep && (c == '=' || c == ':')) {
						hasSep = true;
					} else {
						break;
					}
				}
				valueStart++;
			}
			String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
			String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
			put(key, value);
		}
	}

	/*
	 * Read in a "logical line" from an InputStream/Reader, skip all comment and
	 * blank lines and filter out those leading whitespace characters ( , and )
	 * from the beginning of a "natural line". Method returns the char length of
	 * the "logical line" and stores the line in "lineBuf".
	 */
	class LineReader {
		public LineReader(InputStream inStream) {
			this.inStream = inStream;
			inByteBuf = new byte[8192];
		}

		public LineReader(Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}

		byte[] inByteBuf;
		char[] inCharBuf;
		char[] lineBuf = new char[1024];
		int inLimit = 0;
		int inOff = 0;
		InputStream inStream;
		Reader reader;

		int readLine() throws IOException {
			int len = 0;
			char c = 0;

			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;

			while (true) {
				if (inOff >= inLimit) {
					inLimit = (inStream == null) ? reader.read(inCharBuf) : inStream.read(inByteBuf);
					inOff = 0;
					if (inLimit <= 0) {
						if (len == 0 || isCommentLine) {
							return -1;
						}
						return len;
					}
				}
				if (inStream != null) {
					// The line below is equivalent to calling a
					// ISO8859-1 decoder.
					c = (char) (0xff & inByteBuf[inOff++]);
				} else {
					c = inCharBuf[inOff++];
				}
				if (skipLF) {
					skipLF = false;
					if (c == '\n') {
						continue;
					}
				}
				if (skipWhiteSpace) {
					if (c == ' ' || c == '\t' || c == '\f') {
						continue;
					}
					if (!appendedLineBegin && (c == '\r' || c == '\n')) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				if (isNewLine) {
					isNewLine = false;
					if (c == '#' || c == '!') {
						isCommentLine = true;
						continue;
					}
				}

				if (c != '\n' && c != '\r') {
					lineBuf[len++] = c;
					if (len == lineBuf.length) {
						int newLength = lineBuf.length * 2;
						if (newLength < 0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					// flip the preceding backslash flag
					if (c == '\\') {
						precedingBackslash = !precedingBackslash;
					} else {
						precedingBackslash = false;
					}
				} else {
					// reached EOL
					if (isCommentLine || len == 0) {
						isCommentLine = false;
						isNewLine = true;
						skipWhiteSpace = true;
						len = 0;
						continue;
					}
					if (inOff >= inLimit) {
						inLimit = (inStream == null) ? reader.read(inCharBuf) : inStream.read(inByteBuf);
						inOff = 0;
						if (inLimit <= 0) {
							return len;
						}
					}
					if (precedingBackslash) {
						len -= 1;
						// skip the leading whitespace characters in following
						// line
						skipWhiteSpace = true;
						appendedLineBegin = true;
						precedingBackslash = false;
						if (c == '\r') {
							skipLF = true;
						}
					} else {
						return len;
					}
				}
			}
		}
	}

	/*
	 * Converts encoded &#92;uxxxx to unicode chars and changes special saved
	 * chars to their original forms
	 */
	private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
		if (convtBuf.length < len) {
			int newLen = len * 2;
			if (newLen < 0) {
				newLen = Integer.MAX_VALUE;
			}
			convtBuf = new char[newLen];
		}
		char aChar;
		char[] out = convtBuf;
		int outLen = 0;
		int end = off + len;

		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = in[off++];
						switch (aChar) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							value = (value << 4) + aChar - '0';
							break;
						case 'a':
						case 'b':
						case 'c':
						case 'd':
						case 'e':
						case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A':
						case 'B':
						case 'C':
						case 'D':
						case 'E':
						case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
							throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar == 't')
						aChar = '\t';
					else if (aChar == 'r')
						aChar = '\r';
					else if (aChar == 'n')
						aChar = '\n';
					else if (aChar == 'f')
						aChar = '\f';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = (char) aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	/*
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters
	 * with a preceding slash
	 */
	private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}
	/**
     * Convert a nibble to a hex character
     * @param	nibble	the nibble to convert.
     */
    private static char toHex(int nibble) {
	return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
	'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
	private static void writeComments(BufferedWriter bw, String comments) throws IOException {
		bw.write("#");
		int len = comments.length();
		int current = 0;
		int last = 0;
		char[] uu = new char[6];
		uu[0] = '\\';
		uu[1] = 'u';
		while (current < len) {
			char c = comments.charAt(current);
			if (c > '\u00ff' || c == '\n' || c == '\r') {
				if (last != current)
					bw.write(comments.substring(last, current));
				if (c > '\u00ff') {
					uu[2] = toHex((c >> 12) & 0xf);
					uu[3] = toHex((c >> 8) & 0xf);
					uu[4] = toHex((c >> 4) & 0xf);
					uu[5] = toHex(c & 0xf);
					bw.write(new String(uu));
				} else {
					bw.newLine();
					if (c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n') {
						current++;
					}
					if (current == len - 1 || (comments.charAt(current + 1) != '#' && comments.charAt(current + 1) != '!'))
						bw.write("#");
				}
				last = current + 1;
			}
			current++;
		}
		if (last != current)
			bw.write(comments.substring(last, current));
		bw.newLine();
	}

	/**
	 * Writes this property list (key and element pairs) in this
	 * <code>Properties</code> table to the output character stream in a format
	 * suitable for using the {@link #load(java.io.Reader) load(Reader)} method.
	 * <p>
	 * Properties from the defaults table of this <code>Properties</code> table
	 * (if any) are <i>not</i> written out by this method.
	 * <p>
	 * If the comments argument is not null, then an ASCII <code>#</code>
	 * character, the comments string, and a line separator are first written to
	 * the output stream. Thus, the <code>comments</code> can serve as an
	 * identifying comment. Any one of a line feed ('\n'), a carriage return
	 * ('\r'), or a carriage return followed immediately by a line feed in
	 * comments is replaced by a line separator generated by the
	 * <code>Writer</code> and if the next character in comments is not
	 * character <code>#</code> or character <code>!</code> then an ASCII
	 * <code>#</code> is written out after that line separator.
	 * <p>
	 * Next, a comment line is always written, consisting of an ASCII
	 * <code>#</code> character, the current date and time (as if produced by
	 * the <code>toString</code> method of <code>Date</code> for the current
	 * time), and a line separator as generated by the <code>Writer</code>.
	 * <p>
	 * Then every entry in this <code>Properties</code> table is written out,
	 * one per line. For each entry the key string is written, then an ASCII
	 * <code>=</code>, then the associated element string. For the key, all
	 * space characters are written with a preceding <code>\</code> character.
	 * For the element, leading space characters, but not embedded or trailing
	 * space characters, are written with a preceding <code>\</code> character.
	 * The key and element characters <code>#</code>, <code>!</code>,
	 * <code>=</code>, and <code>:</code> are written with a preceding backslash
	 * to ensure that they are properly loaded.
	 * <p>
	 * After the entries have been written, the output stream is flushed. The
	 * output stream remains open after this method returns.
	 * <p>
	 * 
	 * @param writer
	 *            an output character stream writer.
	 * @param comments
	 *            a description of the property list.
	 * @exception IOException
	 *                if writing this property list to the specified output
	 *                stream throws an <tt>IOException</tt>.
	 * @exception ClassCastException
	 *                if this <code>Properties</code> object contains any keys
	 *                or values that are not <code>Strings</code>.
	 * @exception NullPointerException
	 *                if <code>writer</code> is null.
	 * @since 1.6
	 */
	public void store(Writer writer, String comments) throws IOException {
		store0((writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer), comments, false);
	}
	
	private void store0(BufferedWriter bw, String comments, boolean escUnicode) throws IOException {
		if (comments != null) {
			writeComments(bw, comments);
		}
		bw.write("#" + new Date().toString());
		bw.newLine();
		synchronized (this) {
			for (Enumeration<?> e = keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				String val = (String) get(key);
				key = saveConvert(key, true, escUnicode);
				/*
				 * No need to escape embedded and trailing spaces for value,
				 * hence pass false to flag.
				 */
				val = saveConvert(val, false, escUnicode);
				bw.write(key + "=" + val);
				bw.newLine();
			}
		}
		bw.flush();
	}
}
