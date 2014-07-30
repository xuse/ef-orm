package jef.tools.zip;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractVolchgInoputStream extends InputStream{
	protected InputStream in;
	@Override
	public int read() throws IOException {
		if(in==null)return -1;
		int n= in.read();
		while(n==-1 && changeVolumn()){
			n=in.read();
		}
		return n;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int size=in.read(b, off, len);
		while(size==-1 && changeVolumn()){
			size=in.read(b,off,len);
		}
		return size;
	}
	

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		if(in==null)return 0;
		return in.available();
	}

	@Override
	public void close() throws IOException {
		if(in!=null)
			in.close();
		in=null;
	}
	

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
	
	protected abstract boolean changeVolumn() throws IOException;
}
