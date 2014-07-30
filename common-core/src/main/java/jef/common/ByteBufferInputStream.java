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
package jef.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {

	ByteBuffer bb;
	
	public ByteBufferInputStream(ByteBuffer bytebuffer) {
		bb = bytebuffer;
	}

	public int read() throws IOException {
		if (bb == null)
			throw new IOException("read on a closed InputStream");
		if (bb.remaining() == 0)
			return -1;
		else
			return bb.get();
	}

	public int read(byte abyte0[]) throws IOException {
		if (bb == null)
			throw new IOException("read on a closed InputStream");
		else
			return read(abyte0, 0, abyte0.length);
	}

	public int read(byte abyte0[], int i, int j) throws IOException {
		if (bb == null)
			throw new IOException("read on a closed InputStream");
		if (abyte0 == null)
			throw new NullPointerException();
		if (i < 0 || i > abyte0.length || j < 0 || i + j > abyte0.length || i + j < 0)
			throw new IndexOutOfBoundsException();
		if (j == 0)
			return 0;
		int k = Math.min(bb.remaining(), j);
		if (k == 0) {
			return -1;
		} else {
			bb.get(abyte0, i, k);
			return k;
		}
	}

	public long skip(long l) throws IOException {
		if (bb == null)
			throw new IOException("skip on a closed InputStream");
		if (l <= 0L) {
			return 0L;
		} else {
			int i = (int) l;
			int j = Math.min(bb.remaining(), i);
			bb.position(bb.position() + j);
			return (long) i;
		}
	}

	public int available() throws IOException {
		if (bb == null)
			throw new IOException("available on a closed InputStream");
		else
			return bb.remaining();
	}

	public void close() throws IOException {
		bb = null;
	}

	public synchronized void mark(int i) {
	}

	public synchronized void reset() throws IOException {
		throw new IOException("mark/reset not supported");
	}

	public boolean markSupported() {
		return false;
	}

}
