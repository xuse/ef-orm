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
package jef.tools.string;

import java.io.IOException;
import java.io.Reader;

import jef.tools.ArrayUtils;
/**
 * 在Java类StringReader基础上进行了扩展，增加了若干方法，比较适合文本语义的解析操作。
 * @author Administrator
 */
public class JefStringReader extends Reader {

	private String str;

	private int length;

	private int next = 0;

	private int mark = 0;
	private char[] ignoreChars;
	

	/**
	 * Creates a new string reader.
	 * 
	 * @param s
	 *            String providing the character stream.
	 */
	public JefStringReader(String s) {
		this.str = s;
		this.length = s.length();
	}

	/** Check to make sure that the stream has not been closed */
	private void ensureOpen() throws IOException {
		if (str == null)
			throw new IOException("Stream closed");
	}

	/**
	 * Reads a single character.
	 * 
	 * @return The character read, or -1 if the end of the stream has been
	 *         reached
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public int read() throws IOException {
		synchronized (lock) {
			ensureOpen();
			if (next >= length)
				return -1;
			return str.charAt(next++);
		}
	}
	

	@Override
	public String toString() {
		return str.substring(next);
	}

	/**
	 * Reads characters into a portion of an array.
	 * 
	 * @param cbuf
	 *            Destination buffer
	 * @param off
	 *            Offset at which to start writing characters
	 * @param len
	 *            Maximum number of characters to read
	 * 
	 * @return The number of characters read, or -1 if the end of the stream has
	 *         been reached
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public int read(char cbuf[], int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if ((off < 0) || (off > cbuf.length) || (len < 0)
					|| ((off + len) > cbuf.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}
			if (next >= length)
				return -1;
			int n = Math.min(length - next, len);
			str.getChars(next, next + n, cbuf, off);
			next += n;
			return n;
		}
	}

	/**
	 * Skips the specified number of characters in the stream. Returns the
	 * number of characters that were skipped.
	 * 
	 * <p>
	 * The <code>ns</code> parameter may be negative, even though the
	 * <code>skip</code> method of the {@link Reader} superclass throws an
	 * exception in this case. Negative values of <code>ns</code> cause the
	 * stream to skip backwards. Negative return values indicate a skip
	 * backwards. It is not possible to skip backwards past the beginning of the
	 * string.
	 * 
	 * <p>
	 * If the entire string has been read or skipped, then this method has no
	 * effect and always returns 0.
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public long skip(long ns) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if (next >= length)
				return 0;
			// Bound skip by beginning and end of the source
			long n = Math.min(length - next, ns);
			n = Math.max(-next, n);
			next += n;
			return n;
		}
	}
	
	

	/**
	 * Tells whether this stream is ready to be read.
	 * 
	 * @return True if the next read() is guaranteed not to block for input
	 * 
	 * @exception IOException
	 *                If the stream is closed
	 */
	public boolean ready() throws IOException {
		synchronized (lock) {
			ensureOpen();
			return true;
		}
	}

	/**
	 * Tells whether this stream supports the mark() operation, which it does.
	 */
	public boolean markSupported() {
		return true;
	}

	/**
	 * Marks the present position in the stream. Subsequent calls to reset()
	 * will reposition the stream to this point.
	 * 
	 * @param readAheadLimit
	 *            Limit on the number of characters that may be read while still
	 *            preserving the mark. Because the stream's input comes from a
	 *            string, there is no actual limit, so this argument must not be
	 *            negative, but is otherwise ignored.
	 * 
	 * @exception IllegalArgumentException
	 *                If readAheadLimit is < 0
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public void mark(int readAheadLimit) throws IOException {
		if (readAheadLimit < 0) {
			throw new IllegalArgumentException("Read-ahead limit < 0");
		}
		synchronized (lock) {
			ensureOpen();
			mark = next;
		}
	}

	/**
	 * Resets the stream to the most recent mark, or to the beginning of the
	 * string if it has never been marked.
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public void reset() throws IOException {
		synchronized (lock) {
			ensureOpen();
			next = mark;
		}
	}

	/**
	 * Closes the stream and releases any system resources associated with it.
	 * Once the stream has been closed, further read(), ready(), mark(), or
	 * reset() invocations will throw an IOException. Closing a previously
	 * closed stream has no effect.
	 */
	public void close() {
		str = null;
	}

	/**
	 * 得到接下来的一个字符，如果已经到末尾则返回-1
	 * 整体游标不向前滚动
	 * @return 下一个字符
	 */
	public int nextChar() {
		if (next >= length)
			return -1;
		return str.charAt(next);
	}
	
	/**
	 * 得到接下来的一个字符，如果已经到末尾则返回-1
	 * 整体游标向前滚动
	 * @return 下一个字符
	 */
	public int readChar() {
		if (next >= length)
			return -1;
		return str.charAt(next++);
	}

	/**
	 * 
	 * @return true if the reader is end;
	 */
	public boolean eof(){
		return (next >= length);
	}
	
	/**
	 * 得到接下来的若干字符，如果剩余长度不足返回null;
	 * 
	 * @param n
	 * @return 读到的文字
	 */
	public String nextString(int n) {
		if (next+n > length)
			return null;
		return str.substring(next, next+n);
	}
	
	/**
	 * 读取指定数量的字符形成String，如果长度不足返回null
	 * @return 读到的字符
	 */
	public String readString(int n){
		if (next+n > length)
			return null;
		String result=str.substring(next, next+n);
		next+=n;
		return result;
	}
	
	/**
	 * 跳过指定个数的字符
	 * @param n
	 */
	public int omit(int n){
		next+=n;
		if (next > length){
			n=n-(next-length);
			next=length;
		}
		return n;
	}
	
	/**
	 * @deprecated 请使用{@link #readUntilCharIs(char[])}
	 * @param keyChars
	 * @return
	 */
	public char[] readUntil(char[] keyChars) {
		return readUntilCharIs(keyChars);
	}
	
	/**
	 * 读取Char直到出现指定的匹配（或者结束）为止。指向匹配的char
	 * @return 读到的字符
	 * @throws IOException 
	 */
	public char[] readUntilCharIs(char... keyChars){
		int n=0;
		char result[]=new char[length-next];
		for(int nc=nextChar();nc!=-1 && !ArrayUtils.contains(keyChars,(char)nc);nc=nextChar()){
			result[n]=(char) readChar();
			n++;
		}
		return ArrayUtils.subarray(result, 0, n);
	}
	
	/**
	 * 忽略字符，直到出现指定的字符位置，指向匹配的char
	 * @return
	 */
	public int omitUntillChar(char... keyChars){
		int n=next;
		for(int nc=nextChar();nc!=-1 && !ArrayUtils.contains(keyChars,(char)nc);nc=nextChar()){
			next++;
		}
		return next-n;
	}
	
	/**
	 * @deprecated 使用{@link #readChars(char...)}
	 * @param keyChars
	 * @return
	 */
	public char[] readWhileCharIs(char[] keyChars){
		return readChars(keyChars);
	}
	
	/**
	 * 读取指定范围内char序列。结束后指向第一个不在指定范围内的char
	 * @param keyChars
	 * @return 读到的字符
	 */
	public char[] readChars(char... keyChars){
		int n=0;
		char result[]=new char[length-next];
		for(int nc=nextChar();nc!=-1 && ArrayUtils.contains(keyChars,(char)nc);nc=nextChar()){
			result[n]=(char) readChar();
			n++;
		}
		return ArrayUtils.subarray(result, 0, n);
	}
	
	/**
	 * 跳过这些字符
	 * @param keyChars
	 * @return
	 * @throws IOException
	 */
	public int omitChars(char... keyChars){
		int offset=next;
		if(keyChars.length==0){
			keyChars=this.ignoreChars;
		}
		for(int nc=nextChar();nc!=-1 && ArrayUtils.contains(keyChars,(char)nc);nc=nextChar()){
			next++;
		}
		return next-offset;
	}

	
	/**
	 * 检测后续字符是否符合指定的字符串
	 * @param key 要查找的关键字
	 * @param ignoreChars 如果遇到ignoreChars中的字符则会自动略过(即便在关键字匹配开始后，出现此类字符也会忽略，比较典型的使用场景是：
	 * <pre>
	 *   a = 'hello world!'
	 *   上例中，如果以a=作为key，那么中间的空格会影响搜索结果，此时设置空格为ignorChar，那么就可以排除这种影响。
	 * </pre>
	 * 如果通过{@link #setIgnoreChars(char...)}设置的忽略表，不会忽略关键字当中的特殊字符，因此两者有细微的差别，使用者需要掌握使用这个差别来达到解析的目的。
	 * 备注：ignoreChars 一般可以是空格, \r\n\t等字符，目的是让被这些字符隔断的文字形成完整的语义。
	 * 
	 * @return end char offset from current offset
	 * 返回偏移量，match完成后的位置距离当前位置的偏移量。
	 */
	public int matchNext(String key,char... ignoreChars) {
		int offset=0;
		int match=0;
		boolean matched=false;
		while(!matched && next+offset<length){
			char c=str.charAt(next+offset);
			offset++;
			if(c==key.charAt(match)){//先匹配
				match++;
				if(match==key.length())matched=true;
				continue;
			}
			//匹配失败
			if(match==0){//在匹配开始前允许忽略的字符
				if(ArrayUtils.contains(this.ignoreChars, c)){
					continue;
				}
			}
			//匹配失败
			if(ArrayUtils.contains(ignoreChars, c)){
				continue;
			}
			//匹配失败且字符不可忽略
			return -1;
		}
		return matched?offset:-1;
	}
	
	/**
	 * 检测后续字符是否符合指定的字符串(忽略大小写)
	 * @param key 要查找的关键字
	 * @param ignoreChars 如果遇到ignoreChars中的字符则会自动略过(即便在关键字匹配开始后，出现此类字符也会忽略，比较典型的使用场景是：
	 * <pre>
	 *   a = 'hello world!'
	 *   上例中，如果以a=作为key，那么中间的空格会影响搜索结果，此时设置空格为ignorChar，那么就可以排除这种影响。
	 * </pre>
	 * 如果通过{@link #setIgnoreChars(char...)}设置的忽略表，不会忽略关键字当中的特殊字符，因此两者有细微的差别，使用者需要掌握使用这个差别来达到解析的目的。
	 * 备注：ignoreChars 一般可以是空格, \r\n\t等字符，目的是让被这些字符隔断的文字形成完整的语义。
	 * 
	 * @return end char offset from current offset
	 * 返回偏移量，match完成后的位置距离当前位置的偏移量。
	 */
	public int matchNextIgnoreCase(String key,char... ignoreChars) {
		int offset=0;
		int match=0;
		boolean matched=false;
		while(!matched && next+offset<length){
			char c=str.charAt(next+offset);
			offset++;
			if(Character.toLowerCase(c)==Character.toLowerCase(key.charAt(match))){//先匹配
				match++;
				if(match==key.length())matched=true;
				continue;
			}
			//匹配失败
			if(match==0){//在匹配开始前允许忽略的字符
				if(ArrayUtils.contains(this.ignoreChars, c)){
					continue;
				}
			}
			//匹配失败
			if(ArrayUtils.contains(ignoreChars, c)){
				continue;
			}
			//匹配失败且字符不可忽略
			return -1;
		}
		return matched?offset:-1;
	}

	public char[] getIgnoreChars() {
		return ignoreChars;
	}

	/**
	 * 设置通用的字符忽略表
	 * @see #matchNext(String, char...)
	 * @see #matchNextIgnoreCase(String, char...)
	 * @param ignoreChars
	 */
	public void setIgnoreChars(char... ignoreChars) {
		this.ignoreChars = ignoreChars;
	}

	private static final char[] lb={'\n'};
	
	/**
	 * 读取一行数据，到\n为止
	 * @return 读到的文字
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		if(eof())return null;
		char[] line=readUntilCharIs(lb);
		omit(1);
		if(line[line.length-1]=='\r'){
			return new String(line,0,line.length-1);
		}else{
			return new String(line);
		}
	}
	
	/**
	 * 返回当前的哦啊第几个字符
	 * @return
	 */
	public int getOffset(){
		return next;
	}

	/**
	 * 查找直到指定的key出现，next指向key的第一个字符。如果匹配始终没找到，next指向不会后移
	 * @param key
	 * @param ignorchars
	 * @return -1，if the key was not found.
	 */
	public int omitUntillKey(String key,char...ignorchars){
		int offset=next;//保留初始状态
		int n;
		while((n=matchNext(key,ignorchars))==-1 && next<length){
			next++;
		}
		if(n<0){//始终没有匹配成功
			next=offset;//回滚
			return -1;
		}
		return next-offset;
	}
	
	/**
	 * 查找直到指定的key出现，next指向key的第一个字符。如果匹配始终没找到，next指向不会后移
	 * @param key
	 * @param ignorchars
	 * @return -1，if the key was not found.
	 */
	public int omitUntillKeyIgnoreCase(String key,char...ignorchars){
		int offset=next;//保留初始状态
		int n;
		while((n=matchNextIgnoreCase(key,ignorchars))==-1 && next<length){
			next++;
		}
		if(n<0){//始终没有匹配成功
			next=offset;//回滚
			return -1;
		}
		return next-offset;
	}
	
	/**
	 * 查找直到指定的key出现并完成，next指向key后面的第一个字符。如果匹配始终没找到，next指向不会后移
	 * @param key
	 * @param ignorchars
	 * @return
	 */
	public int omitAfterKey(String key,char...ignorchars){
		int offset=next;//保留初始状态
		int n;
		while((n=matchNext(key,ignorchars))==-1 && next<length){
			next++;
		}
		if(n==-1){//始终没有匹配成功
			next=offset;//回滚
		}else{
			next+=n;
		}
		return next-offset;
	}
	
	/**
	 * 查找直到指定的key出现并完成，next指向key后面的第一个字符。如果匹配始终没找到，next指向不会后移
	 * @param key
	 * @param ignorchars
	 * @return
	 */
	public int omitAfterKeyIgnoreCase(String key,char...ignorchars){
		int offset=next;//保留初始状态
		int n;
		while((n=matchNextIgnoreCase(key,ignorchars))==-1 && next<length){
			next++;
		}
		if(n==-1){//始终没有匹配成功
			next=offset;//回滚
		}else{
			next+=n;
		}
		return next-offset;
	}
	
	
	/**
	 *读取文本，知道出现指定的文本为止。指定的文本不含
	 * @param key
	 * @return 读到的文字
	 * @throws IOException
	 */
	public String readUntillKey(String key,char... ignorchars){
		StringBuilder sb = new StringBuilder();
		while(matchNext(key,ignorchars)==-1 && next<length){
			sb.append((char)readChar());
		}
		return sb.toString();
	}
	
	/**
	 * 读到指定的字符串出现位置
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public String readUntillKeyIgnoreCase(String key,char... ignorchars) {
		StringBuilder sb = new StringBuilder();
		while(matchNextIgnoreCase(key,ignorchars)==-1 && next<length){
			sb.append((char)readChar());
		}
		return sb.toString();
	}
	
	
	/**
	 * 读取直到出现指定的字符串。指定的字符串作为结束符，被消耗掉
	 * @param endChars token结束的字符
	 * @return
	 */
	public String readToken(char... endChars){
		StringBuilder sb = new StringBuilder();
		char c;
		while(!eof()){
			c=(char)readChar();
			if(ArrayUtils.contains(endChars, c)){
				return sb.toString();
			}
			if(ArrayUtils.contains(this.ignoreChars, c)){
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * 消费字符串，在知道下一个词的情况下，读取下一个词
	 * @param key
	 * @param ignoreChars 参见{@link #matchNext(String, char...)}
	 */
	public void consume(String key,char... ignoreChars) {
		int x=matchNext(key,ignoreChars);
		if(x>-1){
			omit(x);
		}else{
			throw new IllegalArgumentException("not expected chars:"+ key);
		}
	}
	/**
	 * 消费字符串
	 * @param key
	 * @param ignoreChars
	 */
	public void consumeIgnoreCase(String key,char... ignoreChars) {
		int x=matchNextIgnoreCase(key,ignoreChars);
		if(x>-1){
			omit(x);
		}else{
			throw new IllegalArgumentException("not expected chars:"+ key);
		}
	}
	
	
	/**
	 * 跳过这些字符
	 * 效果和@{link {@link #omit(int)}相同
	 * @param chars
	 */
	public void consumeChars(char... chars){
		omitChars(chars);
	}
}
