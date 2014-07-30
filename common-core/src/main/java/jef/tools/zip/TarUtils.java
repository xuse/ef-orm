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
package jef.tools.zip;

import java.io.UnsupportedEncodingException;

import jef.tools.ArrayUtils;

public class TarUtils {
	public TarUtils() {}

	public static long parseOctal(byte header[], int offset, int length) {
		long result = 0L;
		boolean stillPadding = true;
		int end = offset + length;
		for (int i = offset; i < end; i++) {
			if (header[i] == 0)
				break;
			if (header[i] == 32 || header[i] == 48) {
				if (stillPadding)
					continue;
				if (header[i] == 32)
					break;
			}
			stillPadding = false;
			result = (result << 3) + (long) (header[i] - 48);
		}
		return result;
	}

	public static CharSequence parseName(byte header[], int offset, int length) {
		int len=ArrayUtils.indexOf(header, (byte)0);
		try {
			return new String(header,offset,Math.min(len, length),TarEntry.DEFAULT_NAME_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static int getNameBytes(CharSequence name,String charset, byte buf[], int offset, int length) throws UnsupportedEncodingException {
		byte[] data=name.toString().getBytes(charset);
		int len= Math.min(data.length, length);
		System.arraycopy(data, 0, buf, offset, len);
		for (int i=len; i < length; i++)
			buf[offset + i] = 0;
		return offset + length;
	}

	public static int getOctalBytes(long value, byte buf[], int offset, int length) {
		int idx = length - 1;
		buf[offset + idx] = 0;
		idx--;
		buf[offset + idx] = 32;
		idx--;
		if (value == 0L) {
			buf[offset + idx] = 48;
			idx--;
		} else {
			for (long val = value; idx >= 0 && val > 0L; idx--) {
				buf[offset + idx] = (byte) (48 + (byte) (int) (val & 7L));
				val >>= 3;
			}
		}
		for (; idx >= 0; idx--)
			buf[offset + idx] = 32;
		return offset + length;
	}

	public static int getLongOctalBytes(long value, byte buf[], int offset, int length) {
		byte temp[] = new byte[length + 1];
		getOctalBytes(value, temp, 0, length + 1);
		System.arraycopy(temp, 0, buf, offset, length);
		return offset + length;
	}

	public static int getCheckSumOctalBytes(long value, byte buf[], int offset, int length) {
		getOctalBytes(value, buf, offset, length);
		buf[(offset + length) - 1] = 32;
		buf[(offset + length) - 2] = 0;
		return offset + length;
	}

	public static long computeCheckSum(byte buf[]) {
		long sum = 0L;
		for (int i = 0; i < buf.length; i++)
			sum += 255 & buf[i];
		return sum;
	}
}
