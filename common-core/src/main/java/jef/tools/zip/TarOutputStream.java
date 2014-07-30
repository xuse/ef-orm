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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// Referenced classes of package org.apache.tools.tar:
//            TarBuffer, TarEntry
public class TarOutputStream extends FilterOutputStream {
	public TarOutputStream(OutputStream os) {
		this(os, 10240, 512);
	}

	public TarOutputStream(OutputStream os, int blockSize) {
		this(os, blockSize, 512);
	}

	public TarOutputStream(OutputStream os, int blockSize, int recordSize) {
		super(os);
		longFileMode = 0;
		closed = false;
		buffer = new TarBuffer(os, blockSize, recordSize);
		debug = false;
		assemLen = 0;
		assemBuf = new byte[recordSize];
		recordBuf = new byte[recordSize];
		oneBuf = new byte[1];
	}

	public void setLongFileMode(int longFileMode) {
		this.longFileMode = longFileMode;
	}

	public void setDebug(boolean debugF) {
		debug = debugF;
	}

	public void setBufferDebug(boolean debug) {
		buffer.setDebug(debug);
	}

	public void finish() throws IOException {
		writeEOFRecord();
		writeEOFRecord();
	}

	public void close() throws IOException {
		if (!closed) {
			finish();
			buffer.close();
			out.close();
			closed = true;
		}
	}

	public int getRecordSize() {
		return buffer.getRecordSize();
	}

	public void putNextEntry(TarEntry entry) throws IOException {
		if (entry.getName().length() >= 100)
			if (longFileMode == 2) {
				TarEntry longLinkEntry = new TarEntry("././@LongLink", (byte) 76);
				longLinkEntry.setSize(entry.getName().length() + 1);
				putNextEntry(longLinkEntry);
				write(entry.getName().getBytes());
				write(0);
				closeEntry();
			} else if (longFileMode != 1)
				throw new RuntimeException("file name '" + entry.getName() + "' is too long ( > " + 100 + " bytes)");
		entry.writeEntryHeader(recordBuf);
		buffer.writeRecord(recordBuf);
		currBytes = 0L;
		if (entry.isDirectory())
			currSize = 0L;
		else
			currSize = entry.getSize();
		currName = entry.getName();
	}

	public void closeEntry() throws IOException {
		if (assemLen > 0) {
			for (int i = assemLen; i < assemBuf.length; i++)
				assemBuf[i] = 0;
			buffer.writeRecord(assemBuf);
			currBytes += assemLen;
			assemLen = 0;
		}
		if (currBytes < currSize)
			throw new IOException("entry '" + currName + "' closed at '" + currBytes + "' before the '" + currSize + "' bytes specified in the header were written");
		else
			return;
	}

	public void write(int b) throws IOException {
		oneBuf[0] = (byte) b;
		write(oneBuf, 0, 1);
	}

	public void write(byte wBuf[]) throws IOException {
		write(wBuf, 0, wBuf.length);
	}

	public void write(byte wBuf[], int wOffset, int numToWrite) throws IOException {
		if (currBytes + (long) numToWrite > currSize)
			throw new IOException("request to write '" + numToWrite + "' bytes exceeds size in header of '" + currSize + "' bytes for entry '" + currName + "'");
		if (assemLen > 0)
			if (assemLen + numToWrite >= recordBuf.length) {
				int aLen = recordBuf.length - assemLen;
				System.arraycopy(assemBuf, 0, recordBuf, 0, assemLen);
				System.arraycopy(wBuf, wOffset, recordBuf, assemLen, aLen);
				buffer.writeRecord(recordBuf);
				currBytes += recordBuf.length;
				wOffset += aLen;
				numToWrite -= aLen;
				assemLen = 0;
			} else {
				System.arraycopy(wBuf, wOffset, assemBuf, assemLen, numToWrite);
				wOffset += numToWrite;
				assemLen += numToWrite;
				return;
			}
		do {
			if (numToWrite <= 0)
				break;
			if (numToWrite < recordBuf.length) {
				System.arraycopy(wBuf, wOffset, assemBuf, assemLen, numToWrite);
				assemLen += numToWrite;
				break;
			}
			buffer.writeRecord(wBuf, wOffset);
			int num = recordBuf.length;
			currBytes += num;
			numToWrite -= num;
			wOffset += num;
		} while (true);
	}

	private void writeEOFRecord() throws IOException {
		for (int i = 0; i < recordBuf.length; i++)
			recordBuf[i] = 0;
		buffer.writeRecord(recordBuf);
	}
	public static final int LONGFILE_ERROR = 0;
	public static final int LONGFILE_TRUNCATE = 1;
	public static final int LONGFILE_GNU = 2;
	protected boolean debug;
	protected long currSize;
	protected String currName;
	protected long currBytes;
	protected byte oneBuf[];
	protected byte recordBuf[];
	protected int assemLen;
	protected byte assemBuf[];
	protected TarBuffer buffer;
	protected int longFileMode;
	private boolean closed;
}
/*
 * DECOMPILATION REPORT
 * 
 * Decompiled from: D:\MyWork\workspace\JEF\lib\ant.jar Total time: 47 ms Jad
 * reported messages/errors: Exit status: 0 Caught exceptions:
 */
