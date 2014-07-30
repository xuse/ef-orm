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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TarInputStream extends FilterInputStream {
	public TarInputStream(InputStream is) {
		this(is, 10240, 512);
	}

	public TarInputStream(InputStream is, int blockSize) {
		this(is, blockSize, 512);
	}

	public TarInputStream(InputStream is, int blockSize, int recordSize) {
		super(is);
		buffer = new TarBuffer(is, blockSize, recordSize);
		readBuf = null;
		oneBuf = new byte[1];
		debug = false;
		hasHitEOF = false;
		v7Format = false;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
		buffer.setDebug(debug);
	}

	public void close() throws IOException {
		buffer.close();
	}

	public int getRecordSize() {
		return buffer.getRecordSize();
	}

	public int available() throws IOException {
		return entrySize - entryOffset;
	}

	public long skip(long numToSkip) throws IOException {
		byte skipBuf[] = new byte[8192];
		long skip;
		int numRead;
		for (skip = numToSkip; skip > 0L; skip -= numRead) {
			int realSkip = (int) (skip <= (long) skipBuf.length ? skip : skipBuf.length);
			numRead = read(skipBuf, 0, realSkip);
			if (numRead == -1)
				break;
		}
		return numToSkip - skip;
	}

	public boolean markSupported() {
		return false;
	}

	public void mark(int i) {}

	public void reset() {}

	public TarEntry getNextEntry() throws IOException {
		if (hasHitEOF)
			return null;
		if (currEntry != null) {
			int numToSkip = entrySize - entryOffset;
			if (debug)
				System.err.println("TarInputStream: SKIP currENTRY '" + currEntry.getName() + "' SZ " + entrySize + " OFF " + entryOffset + "  skipping " + numToSkip + " bytes");
			if (numToSkip > 0)
				skip(numToSkip);
			readBuf = null;
		}
		byte headerBuf[] = buffer.readRecord();
		if (headerBuf == null) {
			if (debug)
				System.err.println("READ NULL RECORD");
			hasHitEOF = true;
		} else if (buffer.isEOFRecord(headerBuf)) {
			if (debug)
				System.err.println("READ EOF RECORD");
			hasHitEOF = true;
		}
		if (hasHitEOF) {
			currEntry = null;
		} else {
			currEntry = new TarEntry(headerBuf);
			if (headerBuf[257] != 117 || headerBuf[258] != 115 || headerBuf[259] != 116 || headerBuf[260] != 97 || headerBuf[261] != 114)
				v7Format = true;
			if (debug)
				System.err.println("TarInputStream: SET CURRENTRY '" + currEntry.getName() + "' size = " + currEntry.getSize());
			entryOffset = 0;
			entrySize = (int) currEntry.getSize();
		}
		if (currEntry != null && currEntry.isGNULongNameEntry()) {
			StringBuffer longName = new StringBuffer();
			byte buf[] = new byte[256];
			for (int length = 0; (length = read(buf)) >= 0;)
				longName.append(new String(buf, 0, length,TarEntry.DEFAULT_NAME_ENCODING));
			getNextEntry();
			if (longName.length() > 0 && longName.charAt(longName.length() - 1) == 0)
				longName.deleteCharAt(longName.length() - 1);
			currEntry.setName(longName.toString());
		}
		return currEntry;
	}

	public int read() throws IOException {
		int num = read(oneBuf, 0, 1);
		return num != -1 ? oneBuf[0] & 255 : -1;
	}

	public int read(byte buf[], int offset, int numToRead) throws IOException {
		int totalRead = 0;
		if (entryOffset >= entrySize)
			return -1;
		if (numToRead + entryOffset > entrySize)
			numToRead = entrySize - entryOffset;
		if (readBuf != null) {
			int sz = numToRead <= readBuf.length ? numToRead : readBuf.length;
			System.arraycopy(readBuf, 0, buf, offset, sz);
			if (sz >= readBuf.length) {
				readBuf = null;
			} else {
				int newLen = readBuf.length - sz;
				byte newBuf[] = new byte[newLen];
				System.arraycopy(readBuf, sz, newBuf, 0, newLen);
				readBuf = newBuf;
			}
			totalRead += sz;
			numToRead -= sz;
			offset += sz;
		}
		while (numToRead > 0) {
			byte rec[] = buffer.readRecord();
			if (rec == null)
				throw new IOException("unexpected EOF with " + numToRead + " bytes unread");
			int sz = numToRead;
			int recLen = rec.length;
			if (recLen > sz) {
				System.arraycopy(rec, 0, buf, offset, sz);
				readBuf = new byte[recLen - sz];
				System.arraycopy(rec, sz, readBuf, 0, recLen - sz);
			} else {
				sz = recLen;
				System.arraycopy(rec, 0, buf, offset, recLen);
			}
			totalRead += sz;
			numToRead -= sz;
			offset += sz;
		}
		entryOffset += totalRead;
		return totalRead;
	}

	public void copyEntryContents(OutputStream out) throws IOException {
		byte buf[] = new byte[32768];
		do {
			int numRead = read(buf, 0, buf.length);
			if (numRead != -1)
				out.write(buf, 0, numRead);
			else
				return;
		} while (true);
	}
	protected boolean debug;
	protected boolean hasHitEOF;
	protected int entrySize;
	protected int entryOffset;
	protected byte readBuf[];
	protected TarBuffer buffer;
	protected TarEntry currEntry;
	protected boolean v7Format;
	protected byte oneBuf[];
}
