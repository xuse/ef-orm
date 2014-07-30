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
package jef.tools.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * 将Reader重新包装为InputStream
 * @author Administrator
 *
 */
public class ReaderInputStream extends InputStream {

	public ReaderInputStream(Reader reader) {
		this.reader = reader;
		byteArrayOut = new ByteArrayOutputStream();
		writer = new OutputStreamWriter(byteArrayOut);
		chars = new char[1024];
	}

	public ReaderInputStream(Reader reader, String encoding) throws UnsupportedEncodingException {
		this.reader = reader;
		byteArrayOut = new ByteArrayOutputStream();
		writer = new OutputStreamWriter(byteArrayOut, encoding);
		chars = new char[1024];
	}

	public int read() throws IOException {
		if (index >= length)
			fillBuffer();
		if (index >= length)
			return -1;
		else
			return 255 & buffer[index++];
	}

	protected void fillBuffer() throws IOException {
		if (length < 0)
			return;
		int numChars = reader.read(chars);
		if (numChars < 0) {
			length = -1;
		} else {
			byteArrayOut.reset();
			writer.write(chars, 0, numChars);
			writer.flush();
			buffer = byteArrayOut.toByteArray();
			length = buffer.length;
			index = 0;
		}
	}

	public int read(byte data[], int off, int len) throws IOException {
		if (index >= length)
			fillBuffer();
		if (index >= length) {
			return -1;
		} else {
			int amount = Math.min(len, length - index);
			System.arraycopy(buffer, index, data, off, amount);
			index += amount;
			return amount;
		}
	}

	public int available() throws IOException {
		return index >= length ? ((int) (length < 0 || !reader.ready() ? 0 : 1)) : length - index;
	}

	public void close() throws IOException {
		reader.close();
	}

	protected Reader reader;
	protected ByteArrayOutputStream byteArrayOut;
	protected Writer writer;
	protected char chars[];
	protected byte buffer[];
	protected int index;
	protected int length;
}

/*
 * DECOMPILATION REPORT
 * 
 * Decompiled from: D:\MyWork\workspace\JEF\lib\ibatis-common-2.jar Total time:
 * 31 ms Jad reported messages/errors: Exit status: 0 Caught exceptions:
 */
