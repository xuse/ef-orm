package jef.tools.string;

import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;

/**
 * 字符串枚举器
 * 居然字符串是能比较大小的，那么字符串显然也是可以枚举的。
 * @author jiyi
 *
 */
public class StringIterator implements Iterator<String> {
	private static char[] TABLE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	AnyNumeral endValue;
	AnyNumeral current;

	/**
	 * 构造 
	 * @param start 开始字符串
	 * @param end   结尾字符串
	 * @param maxLen 字符串最大长度
	 * @param TABLE  字符码表
	 */
	public StringIterator(String start, String end, int maxLen, char[] TABLE) {
		this.current = new AnyNumeral(maxLen, TABLE);
		this.endValue= new AnyNumeral(maxLen, TABLE);
		current.setValue(start);
		endValue.setValue(end);
		current.decreament();
		endValue.decreament();
	}

	/**
	 * 
	 * @param start
	 * @param end
	 * @param maxLen
	 * @param TABLE
	 * @param includeStart 含头
	 * @param includeEnd   含尾
	 */
	public StringIterator(String start, String end, int maxLen, char[] TABLE,boolean includeStart,boolean includeEnd) {
		this.current = new AnyNumeral(maxLen, TABLE);
		this.endValue= new AnyNumeral(maxLen, TABLE);
		current.setValue(start);
		endValue.setValue(end);
		if(includeStart)
			current.decreament();
		if(!includeEnd)
			endValue.decreament();
	}
	
	public StringIterator(String start, String end, int maxLen) {
		this(start,end,maxLen,TABLE);
	}

	public boolean hasNext() {
		return current.compareTo(endValue)<0;
	}

	public String next() {
		return current.increament().getValue();
	}
	
	public String previous() {
		return current.decreament().getValue();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 用于实现字符串遍历的核心算法
	 * 任意进制数算法
	 * 
	 * @author jiyi
	 */

	static class AnyNumeral implements Comparable<AnyNumeral> {
		private int maxLen;//位数
		private char[] TABLE;//字符表
		private byte[] data;
		private byte numeral;
		private int strLen;
		
		@Override
		public String toString() {
			return getValue();
		}

		public AnyNumeral(int num,char[] TABLE) {
			this.maxLen = num;
			this.TABLE=TABLE;
			this.numeral=(byte)TABLE.length;
			this.data = new byte[num];
		}

		public void setValue(String text) {
			for (int i = 0; i < maxLen; i++) {
				if (i < text.length()) {
					char c = text.charAt(i);
					int num = ArrayUtils.indexOf(TABLE, c);
					if (num < 0)
						num = 0;
					data[i] = (byte)num;
				}else{
					data[i] = -1;
				}
			}
			strLen=text.length();
		}
		
		public String getValue(){
			StringBuilder sb=new StringBuilder(maxLen);
			for(int i=0;i<maxLen;i++){
				int code=data[i];
				if(code>=0){
					sb.append(TABLE[code]);	
				}else{
					break;
				}
			}
			return sb.toString();
		}

		public AnyNumeral increament() {
			if(strLen<maxLen){
				data[strLen++]++;
			}else{
				increament(1);
			}
			return this;
		}
		
		private void increament(int i) {
			if(i<=maxLen){
				int value=++data[maxLen-i];
				if(value>=numeral){//进位
					data[maxLen-i]=-1;
					increament(i+1);
					strLen--;
				}	
			}else{
				throw new IllegalArgumentException("Exceed max limit.");
			}
		}

		public AnyNumeral decreament() {
			decreament(1);
			return this;
		}
		
		private void decreament(int i) {
			if(i<=maxLen){
				if(data[maxLen-i]==-1){
					int size=strLen;
					decreament(i+1);
					if(strLen==size){//如果未采用了减位的方式来递减
						while(strLen<maxLen){
							data[strLen++]=(byte)(numeral-1);
						}
					}
					return;
				}
				int v=--data[maxLen-i];
				if(v==-1){
					strLen--;
				}
			}else{
				throw new IllegalArgumentException("Exceed min limit.");
			}
		}


		public int compareTo(AnyNumeral o) {
			for(int i=0;i<maxLen;i++){
				if(data[i]<o.data[i]){
					return -1;
				}else if(data[i]>o.data[i]){
					return 1;
				}
			}
			return 0;
		}
	}

	public static void main(String[] args) {
		String a = "18";
		String b = "30";
		StringIterator iter = new StringIterator(a, b,2, CharUtils.ALPHA_NUM_UNDERLINE);
		int count=0;
		long start=System.currentTimeMillis();
		while (iter.hasNext()) {
			System.out.println(iter.next());
			count++;
		}
		long time=System.currentTimeMillis()-start;
		System.out.println("-------------------");
		System.out.println(time);
		System.out.println("Total:"+count);
	}

}
