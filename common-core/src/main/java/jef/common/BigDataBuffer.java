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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import jef.tools.Exceptions;
import jef.tools.IOUtils;

/**
 * 用于缓存数据的对象。当数据小于2M时，自动使用内存中的空间
 * 大于2M时，使用文件
 * @author Administrator
 *
 */
public class BigDataBuffer extends OutputStream {
	public static final int memcacheLength=2*1024*1024; //内存缓存数据最大2M
	
	private static final int RECEIVE_MEM=-1;      //正在收取数据，使用缓存
	private static final int RECEIVE_FILE=0;		//正在收取数据，使用文件
	private static final int OPEN_MEM=-2; 	//已就绪可放出数据,使用缓存
	private static final int OPEN_FILE=1;		//已就绪可放出数据,使用文件
	private static final int CLOSED=2;		//数据已经取走，无法再利用
	
	private int state;	//当前状态，为上述之一
	long length;//当前长度
	
	ByteBuffer memCache;
	private File file;
	private FileOutputStream out;//存储位置：文件，仅当RECEIVE_FILE时有效
	private InputStream output;	  //获取方式：流：仅当OPEN_FILE时有效
	
	/**
	 * 构造
	 */
	public BigDataBuffer(){
		state=RECEIVE_MEM;
		memCache=ByteBuffer.allocate(memcacheLength);
	}
	
	private BigDataBuffer(byte[] data){
		state=RECEIVE_MEM;
		memCache=ByteBuffer.wrap(data);
	}
	
	/**
	 * 写入内容
	 * @param clientBuffer
	 * @throws IOException
	 */
	public void write(ByteBuffer clientBuffer) throws IOException {
		write(clientBuffer.array(),0,clientBuffer.limit());
	}
	
	/**
	 * 写入内容
	 * @param buffer
	 * @throws IOException
	 */
	public void write(byte[] buffer) throws IOException{
		write(buffer,0,buffer.length);
	}
	
	public void write(int b) throws IOException {
		if(state==RECEIVE_MEM){
			if(length+1<=memCache.capacity()){
				memCache.put((byte)b);
				length++;
				return;
			}else{
				createFile();//将内存缓存写入磁盘
			}
		}
		if(state!=RECEIVE_FILE){
			throw new IllegalStateException("The BigDataBuffer do not allow write any data, state="+state);
		}
		out.write(b);
		length++;
	}
	
	/**
	 * 写入内容
	 * @param buffer
	 * @throws IOException
	 */
	public void write(byte[] buffer,int off,int len) throws IOException{
		if(state==RECEIVE_MEM){
			if(length+len<=memCache.capacity()){
				memCache.put(buffer, off, len);
				length+=len;
				return;
			}else{
				createFile();//将内存缓存写入磁盘
			}
		}
		if(state!=RECEIVE_FILE){
			throw new IllegalStateException("The BigDataBuffer do not allow write any data, state="+state);
		}
		out.write(buffer,off,len);
		length+=len;
	}
	
	/**
	 * 标记状态为收取结束，开始利用数据
	 */
	public void flip(){
		if(state==RECEIVE_MEM){
			memCache.flip();
			state=OPEN_MEM;
		}
		if(state==RECEIVE_FILE){
			IOUtils.closeQuietly(out);
			state=OPEN_FILE;
		}
	}
	
	
	class InnerInputStream extends FilterInputStream{
		protected InnerInputStream(InputStream in) {
			super(in);
		}
		@Override
		public void close() throws IOException {
			super.close();
			clear();
		}
		@Override
		public String toString() {
			StringBuilder sb=new StringBuilder("Buffer:");
			sb.append(length).append(" bytes");
			return sb.toString();
		}
	}
	
	
	/**
	 * 写入完成，开始获取内容
	 * @return
	 * @throws IOException
	 */
	public InputStream getAsStream() throws IOException{
		flip();
		if(state==OPEN_MEM){
			if(output==null){
				memCache.rewind();
				output=new InnerInputStream(new ByteBufferInputStream(memCache));
			}
			return output;
		}
		if(state==OPEN_FILE){
			if(output==null)output=new InnerInputStream(new FileInputStream(file));
			return output;	
		}
		throw new IllegalStateException("Current state is "+ state);
	}
	
	/**
	 * 清空对象，可以进行下一次收取数据操作
	 */
	public void clear(){
		if(state==RECEIVE_MEM || state==OPEN_MEM){
			memCache.clear();
			return;
		}
		if(state==RECEIVE_FILE){
			IOUtils.closeQuietly(out);
			state=OPEN_FILE;
		}
		if(state==OPEN_FILE){
			if(file.exists())file.delete();
			file=null;
			state=RECEIVE_MEM;	
		}
		length=0;
	}
	
	private void createFile(){
		try {
			file=File.createTempFile("~temp", ".buf");
			out=new FileOutputStream(file);
			memCache.flip();
			out.getChannel().write(memCache);
			memCache.clear();//不再使用的内存区域
			state=RECEIVE_FILE;
		} catch (FileNotFoundException e) {
			throw Exceptions.asIllegalArgument(e);
		} catch (IOException e) {
			throw Exceptions.asIllegalArgument(e);
		}
	}
	
	/**
	 * 读取部分数据
	 * @param buff
	 * @param offset
	 * @param len
	 */
	public void putByte(byte[] buff, int offset, int len){
		System.arraycopy(memCache.array(), offset, buff, 0, len);
	}

	/**
	 * 返回数据总长度
	 * @return
	 */
	public long length() {
		return this.length;
	}
	
	/**
	 * 从文件形成
	 * @param file
	 * @return
	 */
	public static BigDataBuffer wrap(File file){
		BigDataBuffer bb=new BigDataBuffer();
		bb.state=OPEN_FILE;
		bb.file=file;
		bb.length=file.length();
		return bb;
	}

	/**
	 * 从内存数据形成
	 * @param data
	 * @return
	 */
	public static BigDataBuffer wrap(byte[] data){
		BigDataBuffer bb=new BigDataBuffer(data);
		return bb;
	}
	
	/**
	 * 从流形成,流会自动关闭
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	public static BigDataBuffer wrap(InputStream in){
		BigDataBuffer bb=new BigDataBuffer();
		byte[] buf=new byte[1024];
		try{
			int n;
			while((n=in.read(buf))>-1){
				bb.write(buf,0,n);
			}
			IOUtils.closeQuietly(in);
		}catch(IOException e){
			throw new RuntimeException(e.getCause());
		}
		return bb;
	}
	
	/**
	 * 如果缓存是在内存，也以文件的形式返回。
	 * @return
	 */
	public File toFile() {
		if(state==RECEIVE_MEM){
			createFile();
		}
		if(state==OPEN_MEM){
			memCache.rewind();
			createFile();
		}
		if(state==RECEIVE_FILE){
			IOUtils.closeQuietly(out);
			state=OPEN_FILE;
		}
		if(state==OPEN_FILE){
			return file;	
		}
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 如果缓存是文件，则返回，否则返回null;
	 * @return
	 */
	public File getTmpFile(){
		return file;
	}

	public void close() throws IOException {
		flip();
	}
}
