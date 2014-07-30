package jef.tools.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * 将Writer重新包装为OutputStream
 * @author Administrator
 *
 */
public class WriterOutputStream extends OutputStream {
	/**
	 * 被包装在writer之外
	 */
	protected final Writer _writer;
	/**
	 * 编码
	 */
	protected final String _encoding;
	//缓冲区
	private final byte _buf[];
	public WriterOutputStream(Writer writer, String encoding) {
		_buf = new byte[1];
		_writer = writer;
		_encoding = encoding;
	}

	public WriterOutputStream(Writer writer) {
		_buf = new byte[1];
		_writer = writer;
		_encoding = null;
	}

	public void close() throws IOException {
		_writer.close();
	}

	public void flush() throws IOException {
		_writer.flush();
	}

	public void write(byte b[]) throws IOException {
		if (_encoding == null)
			_writer.write(new String(b));
		else
			_writer.write(new String(b, _encoding));
	}

	public void write(byte b[], int off, int len) throws IOException {
		if (_encoding == null)
			_writer.write(new String(b, off, len));
		else
			_writer.write(new String(b, off, len, _encoding));
	}

	public synchronized void write(int b) throws IOException {
		_buf[0] = (byte) b;
		write(_buf);
	}
}
