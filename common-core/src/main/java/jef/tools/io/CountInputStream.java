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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 用于计算字节数量的输入流
 * @author Administrator
 *
 */
public class CountInputStream extends FilterInputStream{
	private long count=0;

	public CountInputStream(InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		int c=in.read();
		if(c!=-1)count++;
		return c;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int s=in.read(b, off, len);
		count+=s;
		return s;	
	}
	

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
		count=0;
	}

	@Override
	public long skip(long n) throws IOException {
		 long r=super.skip(n);
		 count+=r;
		 return r;
	}

	public long getSize() {
		return count;
	}
}
