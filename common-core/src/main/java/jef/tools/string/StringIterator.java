package jef.tools.string;

import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;

public class StringIterator implements Iterator<String> {
	private char[] TABLE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private String start;

	private String end;
	
	int startValue;

	int endValue;

	int current;
	
	private int maxLen;
	
	private int tableLen;
	
	public StringIterator(String start, String end,int maxLen,char[] TABLE) {
		this.TABLE=TABLE;
		this.tableLen=TABLE.length;
		this.start=start;
		this.end=end;
		this.maxLen=maxLen;
		this.startValue=getValue(start);
		this.endValue=getValue(end);
		current=startValue;
	}
	
	
	public StringIterator(String start, String end,int maxLen) {
		this.tableLen=TABLE.length;
		this.start=start;
		this.end=end;
		this.maxLen=maxLen;
		this.startValue=getValue(start);
		this.endValue=getValue(end);
		current=startValue;
	}

	//计算一个字符串的码数
	private int getValue(String text) {
		int total=0;
		for(int i=0;i<maxLen;i++){
			if(i<text.length()){
				char c=text.charAt(i);
				int num=ArrayUtils.indexOf(TABLE, c);
				if(num<0)num=0;
				if(num>0){
					int base=power(tableLen,maxLen-i);
					total+=base*num;	
				}
			}
		}
		return total;
	}

	private int power(int base, int i) {
		int result=1;
		for(int x=1;x<i;x++){
			result=result*base;
		}
		return result;
	}

	public boolean hasNext() {
		return current<=endValue;
	}

	public String next() {
		return reverse(current++);
	}

	private String reverse(int code) {
		StringBuilder sb=new StringBuilder();
		for(int i=maxLen;i>0;i--){
			int base=power(tableLen,i);
			int c1=(int)code/base;
			sb.append(TABLE[c1]);
			code-=c1*base;
			if(code<0)break;
		}
		if(code>0){
			sb.append(TABLE[code]);
		}
		return sb.toString();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	
	public static void main(String[] args) {
		String a="12";
		String b="4255";
		StringIterator iter=new StringIterator(a,b,3,CharUtils.NUMBERS);
		while(iter.hasNext()){
			System.out.println(iter.next());
		}
	}
}
