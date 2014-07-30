package jef.testbase;

import java.io.IOException;

import jef.common.wrapper.IntRange;
import jef.tools.StringUtils;
import jef.tools.ThreadUtils;
import jef.tools.string.CharUtils;
import jef.tools.string.RandomData;

import org.junit.BeforeClass;
import org.junit.Test;


public class StringAddTest {
	private static String s1;
	private static String s2;
	private static String s3;
	private static String s4;
	private static String s5;
	private static String s6;
	private static final int LOOP_TIMES=500;
	
	public static void main(String[] args) {
		ThreadUtils.doSleep(12000);
		for(int i=0;i<10;i++){
			Task t=new Task();
			t.start();
		}
		Task t=new Task();
		t.run();
	}
	
	static class Task extends Thread{

		@Override
		public void run() {
			for(int i=0;i<100;i++){
				StringAddTest t=new StringAddTest();
				try{
					StringAddTest.setup();
					t.test3();
					t.test4();
					t.test5();
					t.testx();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	
	@BeforeClass
	public synchronized static void setup() throws IOException{
		s1="asdkjslfnlsdfndskjgndkjgnkjdsfbgkdjfgndfkjgndfka";
		s3="sad";
		s4="csdklgnfkldngfdlkgd";
		s5="csfdfmslkfmldsfmsd";
		s6="cdskfkkkkdkaskldlksdmklsdmkldsfmdkslfmdslkfmdfgndflgfdnfdjd";
		s2=StringUtils.repeat(s6, 1000);//通过重复生成一个很长的字符串
		
		System.out.println("s1 length=" + s1.length());
		System.out.println("s2 length=" + s2.length());
		System.out.println("s3 length=" + s3.length());
	}

	
	//JDK5以后教科书提供的标准方法，但其实比test1快不了多少(5734ms)
	@Test
	public void test3(){
		long start=System.currentTimeMillis();
		for(int i=0;i<LOOP_TIMES;i++){
			String x=new StringBuilder(s1).append(s2).append(s3).append(s4).append(s5).append(s6).toString();
		}
		long cost=System.currentTimeMillis()-start;
		System.out.println("==== the method test3 cost " + cost+"ms.");
	}
	
	//优化后的方法，性能已经明显提高了。使用StringBuilder的极限也就是这样了(2969ms)
	@Test
	public void test4(){
		long start=System.currentTimeMillis();
		for(int i=0;i<LOOP_TIMES;i++){
			String x=new StringBuilder(s1.length()+s2.length()+s3.length()+s4.length()+s5.length()+s6.length()).append(s1).append(s2).append(s3).
			append(s4).append(s5).append(s6).toString();
		}
		long cost=System.currentTimeMillis()-start;
		System.out.println("==== the method test4 cost " + cost+"ms.");
	}
	
	//JDK String内建的方法。
	//无论怎么变化String大小和循环次数，一定是最快和内存占用最少的方法(2078ms)
	@Test
	public void test5(){
		long start=System.currentTimeMillis();
		for(int i=0;i<LOOP_TIMES;i++){
			String x=s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6);
		}
		long cost=System.currentTimeMillis()-start;
		System.out.println("==== the method test5 cost " + cost+"ms.");
	}
	
	//JDK String内建的方法。
	//无论怎么变化String大小和循环次数，一定是最快和内存占用最少的方法(2078ms)
	@Test
	public void testx(){
		long start=System.currentTimeMillis();
		for(int i=0;i<LOOP_TIMES;i++){
			String x=StringUtils.concat(s1,s2,s3,s4,s5,s6);
		}
		long cost=System.currentTimeMillis()-start;
		System.out.println("==== the method testx cost " + cost+"ms.");
	}
	@Test
	public void aa(){
		String a=RandomData.randomString(CharUtils.ALPHA_NUM_UNDERLINE, new IntRange(256,256));
		String b=new String(a);
		String c=new String(a);
		
		long start=System.nanoTime();
		for(int i=1;i<10000;i++){
			String d=a+b+c;
		}
		long x=System.nanoTime();
		System.out.println(x-start);
		
	}
	
	public void call(){
	}
}
