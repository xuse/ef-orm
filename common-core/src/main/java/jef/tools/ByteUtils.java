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
package jef.tools;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;



public class ByteUtils {
	/**
	 * 将long转换为byte[8](高位在前)
	 * @param l
	 * @return
	 */
	public static byte[] toBytes(long l){
		byte[] b=new byte[8];
		putLong(b, l, 0);
		return b;
	}
	
	/**
	 * 将int转换为byte[4](高位在前)
	 * @param i
	 * @return
	 */
	public static byte[] toBytes(int i){
		byte[] b=new byte[4];
		putInt(b, i, 0);
		return b;
	}
	
	/**
	 * 将short转换为byte[2]. 高位在前，低位在后
	 * @param i
	 * @return
	 */
	public static byte[] toBytes(short i){
		byte[] b=new byte[2];
		putShort(b, i, 0);
		return b;
	}
	
	/**
	 * 将short放入byte数组，高位在前，低位在后
	 * @param b byte数组
	 * @param s 要放入的值
	 * @param offset 所在序号
	 */
	public static void putShort(byte b[], short s, int offset) {
		int len=b.length;
		if(offset<len)b[offset] = (byte) (s >> 8);
		if(offset+1<len)b[offset + 1] = (byte) (s >> 0);
	}

	/**
	 * 将short放入byte数组，低位在前，高位在后
	 * @param b byte数组
	 * @param s 要放入的值
	 * @param offset 所在序号
	 */
	public static void putReverseBytesShort(byte b[], short s, int offset) {
		int len=b.length;
		if(offset<len)b[offset] = (byte) (s >> 0);
		if(offset<len)b[offset + 1] = (byte) (s >> 8);
	}

	/**
	 * 从字节组中获得long（正常顺序：高位在前，低位在后）
	 * @param b
	 * @param offset
	 * @return
	 */
	public static short getShort(byte[] b, int offset) {
		return (short) (((b[offset] << 8) | b[offset + 1] & 0xff));
	}
	
	/**
	 * 从字节数组中获得short,(高位在后，低位在前)
	 * @param b
	 * @param offset
	 * @return
	 */
	public static short getReverseBytesShort(byte[] b, int offset) {
		return (short) (((b[offset + 1] << 8) | b[offset] & 0xff));
	}

	// ///////////////////////////////////////////////////////

	/**
	 * 将short放入byte数组，高位在前，低位在后
	 * @param b byte数组
	 * @param s 要放入的值
	 * @param offset 所在序号
	 */
	public static void putInt(byte[] bb, int x, int offset) {
		bb[offset + 0] = (byte) (x >> 24);
		bb[offset + 1] = (byte) (x >> 16);
		bb[offset + 2] = (byte) (x >> 8);
		bb[offset + 3] = (byte) (x >> 0);
	}

	/**
	 * 将int放入byte数组，低位在前，高位在后
	 * @param b byte数组
	 * @param s 要放入的值
	 * @param offset 所在序号
	 */	
	public static void putReverseBytesInt(byte[] bb, int x, int offset) {
		bb[offset + 3] = (byte) (x >> 24);
		bb[offset + 2] = (byte) (x >> 16);
		bb[offset + 1] = (byte) (x >> 8);
		bb[offset + 0] = (byte) (x >> 0);
	}

	/**
	 * 从字节流中读取一个int(占4个 byte,高位在前低位在后)
	 * @param bb
	 * @param offset
	 * @return
	 */
	public static int getInt(byte[] bb, int offset) {
		return (int) ((((bb[offset + 0] & 0xff) << 24) | ((bb[offset + 1] & 0xff) << 16) | ((bb[offset + 2] & 0xff) << 8) | ((bb[offset + 3] & 0xff))));
	}
	
	/**
	 * 从字节流中读取一个int(占4个 byte,低位在前，高位在后)
	 * @param bb
	 * @param offset
	 * @return
	 */
	public static int getReverseBytesInt(byte[] bb, int offset) {
		return (int) ((((bb[offset + 3] & 0xff) << 24) | ((bb[offset + 2] & 0xff) << 16) | ((bb[offset + 1] & 0xff) << 8) | ((bb[offset + 0] & 0xff))));
	}

	// /////////////////////////////////////////////////////////
	/**
	 * 将long放入byte数组，高位在前，低位在后
	 * @param b byte数组
	 * @param s 要放入的值
	 * @param offset 所在序号
	 */	
	public static void putLong(byte[] bb, long x, int offset) {
		bb[offset + 0] = (byte) (x >> 56);
		bb[offset + 1] = (byte) (x >> 48);
		bb[offset + 2] = (byte) (x >> 40);
		bb[offset + 3] = (byte) (x >> 32);
		bb[offset + 4] = (byte) (x >> 24);
		bb[offset + 5] = (byte) (x >> 16);
		bb[offset + 6] = (byte) (x >> 8);
		bb[offset + 7] = (byte) (x >> 0);
	}
	
	/**
	 * 将long放入byte数组，低位在前，高位在后
	 * @param b byte数组
	 * @param s 要放入的值
	 * @param offset 所在序号
	 */	
	public static void putReverseBytesLong(byte[] bb, long x, int offset) {
		bb[offset + 7] = (byte) (x >> 56);
		bb[offset + 6] = (byte) (x >> 48);
		bb[offset + 5] = (byte) (x >> 40);
		bb[offset + 4] = (byte) (x >> 32);
		bb[offset + 3] = (byte) (x >> 24);
		bb[offset + 2] = (byte) (x >> 16);
		bb[offset + 1] = (byte) (x >> 8);
		bb[offset + 0] = (byte) (x >> 0);
	}

	/**
	 * 
	 * @param bb
	 * @param offset
	 * @return
	 */
	public static long getLong(byte[] bb, int offset) {
		return ((((long) bb[offset + 0] & 0xff) << 56) | (((long) bb[offset + 1] & 0xff) << 48) | (((long) bb[offset + 2] & 0xff) << 40) | (((long) bb[offset + 3] & 0xff) << 32) | (((long) bb[offset + 4] & 0xff) << 24) | (((long) bb[offset + 5] & 0xff) << 16)
				| (((long) bb[offset + 6] & 0xff) << 8) | (((long) bb[offset + 7] & 0xff) << 0));
	}
	
	/**
	 * 
	 * @param bb
	 * @param offset
	 * @return
	 */
	public static long getReverseBytesLong(byte[] bb, int offset) {
		return ((((long) bb[offset + 7] & 0xff) << 56) | (((long) bb[offset + 6] & 0xff) << 48) | (((long) bb[offset + 5] & 0xff) << 40) | (((long) bb[offset + 4] & 0xff) << 32) | (((long) bb[offset + 3] & 0xff) << 24) | (((long) bb[offset + 2] & 0xff) << 16)
				| (((long) bb[offset + 1] & 0xff) << 8) | (((long) bb[offset + 0] & 0xff) << 0));
	}
	
	/**
	 * 将byte转换为int
	 * @param b
	 * @return
	 */
	public static int byteToInt(byte b){
		return  b>= 0 ? b : 256 + b;  
	}
	
	/**
	 * 将long转换为指定字节数的数组
	 */
	public static byte[] longToBytes(long x,int size){
		byte[] bb=new byte[size];
		for(int n=0;n<8;n++){
			if(size==n)break;
			bb[n]=(byte)(x>>(8*n));
		}
		return bb;
	}
	
	/**
	 * 从字节码转换成Long
	 * @Title: bytesToLong
	 */
	public static long bytesToLong(byte[] bs) {
		long rst = 0;
		for (int n = 0; n < bs.length; n++) {
			long s = (long) bs[n] >= 0 ? bs[n] : 256 + bs[n];
			rst |= s << (n * 8);
		}
		return rst;
	}

	/**
	 * 判断buf中的开头字节是否匹配key中的所有字节
	 * @param buf
	 * @param key
	 * @return
	 */
	public static boolean matchStart(byte[] buf, byte[] key) {
		if(key.length>buf.length){
			return false;
		}
		for(int i=0;i<key.length;i++){
			if(buf[i]!=key[i]){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 将输入流中所有非[0-9a-fA-F]以外的字符全部丢弃，然后作为16进制文本，转换为原始的字节数组
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static byte[] hexReader2byte(Reader input) throws IOException {
		CharArrayWriter cw=new CharArrayWriter(512);
		int c;
		while((c=input.read())>-1){
			if(c<48 || (c>57 && c<65) || (c>70 && c<97) || c>102){
				continue;				
			}
			cw.append((char)c);
		}
		return hex2byte(cw.toCharArray(), false);
	}
	
	/**
	 * 将二进制文本列表转换为字节数组
	 * @param hexString
	 * @param hasSpace
	 * @return
	 * @throws IOException
	 */
	public static byte[] hex2byte(char[] hexString, boolean hasSpace) throws IOException {
		int len = hexString.length;
		byte[] result = new byte[hasSpace ? (len + 1) / 3 : len / 2];
		int count = 0;
		for (int i = 0; i < len; i++) {
			char c1 = hexString[i];
			char c2 = hexString[++i];
			int i1 = hexChar2dec(c1);
			int i2 = hexChar2dec(c2);
			result[count++] = (byte) ((i1 << 4) + i2);
			if (hasSpace)
				++i;
		}
		return result;
	}
	/**
	 * byte2hex的逆运算（有实际用处吗？） 实际使用可以用Byte Byte.parseByte("dd", 16);
	 * 
	 * @param hexString
	 * @return
	 * @throws IOException
	 */
	public static byte[] hex2byte(CharSequence hexString, boolean hasSpace) throws IOException {
		int len = hexString.length();
		byte[] result = new byte[hasSpace ? (len + 1) / 3 : len / 2];
		int count = 0;
		for (int i = 0; i < len; i++) {
			char c1 = hexString.charAt(i);
			char c2 = hexString.charAt(++i);
			int i1 = hexChar2dec(c1);
			int i2 = hexChar2dec(c2);
			result[count++] = (byte) ((i1 << 4) + i2);
			if (hasSpace)
				++i;
		}
		return result;
	}

	/*
	 * 将输入的十六进制字符转换为十进制数字
	 */
	private static int hexChar2dec(char hex) {
		if (hex > 47 && hex < 58) {
			hex -= 48;
		} else if (hex > 64 && hex < 71) {
			hex -= 55;
		} else if (hex > 96 && hex < 103) {
			hex -= 87;
		} else {
			throw new RuntimeException(hex + "is not a valid hex char.");
		}
		return hex;
	}
}
