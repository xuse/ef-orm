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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class TarBuffer {
	public TarBuffer(InputStream inStream) {
		this(inStream, 10240);
	}

	public TarBuffer(InputStream inStream, int blockSize) {
		this(inStream, blockSize, 512);
	}

	public TarBuffer(InputStream inStream, int blockSize, int recordSize) {
		this.inStream = inStream;
		outStream = null;
		initialize(blockSize, recordSize);
	}

	public TarBuffer(OutputStream outStream) {
		this(outStream, 10240);
	}

	public TarBuffer(OutputStream outStream, int blockSize) {
		this(outStream, blockSize, 512);
	}

	public TarBuffer(OutputStream outStream, int blockSize, int recordSize) {
		inStream = null;
		this.outStream = outStream;
		initialize(blockSize, recordSize);
	}

	private void initialize(int blockSize, int recordSize) {
		debug = false;
		this.blockSize = blockSize;
		this.recordSize = recordSize;
		recsPerBlock = this.blockSize / this.recordSize;
		blockBuffer = new byte[this.blockSize];
		if (inStream != null) {
			currBlkIdx = -1;
			currRecIdx = recsPerBlock;
		} else {
			currBlkIdx = 0;
			currRecIdx = 0;
		}
	}

	public int getBlockSize() {
		return blockSize;
	}

	public int getRecordSize() {
		return recordSize;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isEOFRecord(byte record[]) {
		int i = 0;
		for (int sz = getRecordSize(); i < sz; i++)
			if (record[i] != 0)
				return false;
		return true;
	}

	public void skipRecord() throws IOException {
		if (debug)
			System.err.println("SkipRecord: recIdx = " + currRecIdx + " blkIdx = " + currBlkIdx);
		if (inStream == null)
			throw new IOException("reading (via skip) from an output buffer");
		if (currRecIdx >= recsPerBlock && !readBlock()) {
			return;
		} else {
			currRecIdx++;
			return;
		}
	}

	public byte[] readRecord() throws IOException {
		if (debug)
			System.err.println("ReadRecord: recIdx = " + currRecIdx + " blkIdx = " + currBlkIdx);
		if (inStream == null)
			throw new IOException("reading from an output buffer");
		if (currRecIdx >= recsPerBlock && !readBlock()) {
			return null;
		} else {
			byte result[] = new byte[recordSize];
			System.arraycopy(blockBuffer, currRecIdx * recordSize, result, 0, recordSize);
			currRecIdx++;
			return result;
		}
	}

	private boolean readBlock() throws IOException {
		if (debug)
			System.err.println("ReadBlock: blkIdx = " + currBlkIdx);
		if (inStream == null)
			throw new IOException("reading from an output buffer");
		currRecIdx = 0;
		int offset = 0;
		int bytesNeeded = blockSize;
		do {
			if (bytesNeeded <= 0)
				break;
			long numBytes = inStream.read(blockBuffer, offset, bytesNeeded);
			if (numBytes == -1L) {
				if (offset == 0)
					return false;
				Arrays.fill(blockBuffer, offset, offset + bytesNeeded, (byte) 0);
				break;
			}
			offset = (int) ((long) offset + numBytes);
			bytesNeeded = (int) ((long) bytesNeeded - numBytes);
			if (numBytes != (long) blockSize && debug)
				System.err.println("ReadBlock: INCOMPLETE READ " + numBytes + " of " + blockSize + " bytes read.");
		} while (true);
		currBlkIdx++;
		return true;
	}

	public int getCurrentBlockNum() {
		return currBlkIdx;
	}

	public int getCurrentRecordNum() {
		return currRecIdx - 1;
	}

	public void writeRecord(byte record[]) throws IOException {
		if (debug)
			System.err.println("WriteRecord: recIdx = " + currRecIdx + " blkIdx = " + currBlkIdx);
		if (outStream == null)
			throw new IOException("writing to an input buffer");
		if (record.length != recordSize)
			throw new IOException("record to write has length '" + record.length + "' which is not the record size of '" + recordSize + "'");
		if (currRecIdx >= recsPerBlock)
			writeBlock();
		System.arraycopy(record, 0, blockBuffer, currRecIdx * recordSize, recordSize);
		currRecIdx++;
	}

	public void writeRecord(byte buf[], int offset) throws IOException {
		if (debug)
			System.err.println("WriteRecord: recIdx = " + currRecIdx + " blkIdx = " + currBlkIdx);
		if (outStream == null)
			throw new IOException("writing to an input buffer");
		if (offset + recordSize > buf.length)
			throw new IOException("record has length '" + buf.length + "' with offset '" + offset + "' which is less than the record size of '" + recordSize + "'");
		if (currRecIdx >= recsPerBlock)
			writeBlock();
		System.arraycopy(buf, offset, blockBuffer, currRecIdx * recordSize, recordSize);
		currRecIdx++;
	}

	private void writeBlock() throws IOException {
		if (debug)
			System.err.println("WriteBlock: blkIdx = " + currBlkIdx);
		if (outStream == null) {
			throw new IOException("writing to an input buffer");
		} else {
			outStream.write(blockBuffer, 0, blockSize);
			outStream.flush();
			currRecIdx = 0;
			currBlkIdx++;
			return;
		}
	}

	private void flushBlock() throws IOException {
		if (debug)
			System.err.println("TarBuffer.flushBlock() called.");
		if (outStream == null)
			throw new IOException("writing to an input buffer");
		if (currRecIdx > 0)
			writeBlock();
	}

	public void close() throws IOException {
		if (debug)
			System.err.println("TarBuffer.closeBuffer().");
		if (outStream != null) {
			flushBlock();
			if (outStream != System.out && outStream != System.err) {
				outStream.close();
				outStream = null;
			}
		} else if (inStream != null && inStream != System.in) {
			inStream.close();
			inStream = null;
		}
	}
	public static final int DEFAULT_RCDSIZE = 512;
	public static final int DEFAULT_BLKSIZE = 10240;
	private InputStream inStream;
	private OutputStream outStream;
	private byte blockBuffer[];
	private int currBlkIdx;
	private int currRecIdx;
	private int blockSize;
	private int recordSize;
	private int recsPerBlock;
	private boolean debug;
}
