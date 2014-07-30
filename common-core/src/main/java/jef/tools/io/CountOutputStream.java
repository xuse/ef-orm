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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于计算字节数量的输出流
 * @author Administrator
 *
 */
public class CountOutputStream extends FilterOutputStream{
	protected long count=0;

	public CountOutputStream(OutputStream in) {
		super(in);
	}

	@Override
	public void write(int b) throws IOException {
		super.write(b);
		count++;
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
		count+=b.length;
	}


	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		count+=len;
	}

	public long getSize() {
		return count;
	}
}
