package jef.tools;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.tools.string.RandomData;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class SortUtilsTest {
	private Comparable[] data;
	private static int MAX_LENGTH=1000;
	private static Comparable[] rawData;
	
	@BeforeClass
	public static void prepareData(){
		rawData=new Integer[MAX_LENGTH];
		for(int i=0;i<MAX_LENGTH;i++){
			rawData[i]= RandomData.randomInteger(0, 500);
		}
	}
	
	@Before
	public void setUp() throws Exception {
		data=new Integer[MAX_LENGTH];
		System.arraycopy(rawData, 0, data, 0, rawData.length);
	}
	
	@SuppressWarnings({ "unchecked"})
	private void check(){
		Comparable last=null;
		for(int i=0;i<data.length;i++){
			Comparable the=data[i];
			if(last!=null){
				assertTrue(the.compareTo(last)>=0);
			}
			last=the;
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSortTArray() {
		SortUtils.sort(data);
		check();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSortTArrayInt() throws Exception {
		long start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_HEAP);
		long end=System.currentTimeMillis();
		System.out.println("堆排序耗时" + (end-start)+"ms");
		check();
		
		//冒泡排序太慢了，移出
		testBubble();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_IMPROVED_MERGE);
		end=System.currentTimeMillis();
		System.out.println("归并 改排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_IMPROVED_QUICK);
		end=System.currentTimeMillis();
		System.out.println("快速 改排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_INSERT);
		end=System.currentTimeMillis();
		System.out.println("插入 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_MERGE);
		end=System.currentTimeMillis();
		System.out.println("归并 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_QUICK);
		end=System.currentTimeMillis();
		System.out.println("快速 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_SELECTION);
		end=System.currentTimeMillis();
		System.out.println("选择 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_SHELL);
		end=System.currentTimeMillis();
		System.out.println("希尔排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Arrays.sort(data);
		end=System.currentTimeMillis();
		System.out.println("希尔排序耗时" + (end-start)+"ms");
		check();
		
		
	}

	@Ignore
	@SuppressWarnings("unchecked")
	private void testBubble() throws Exception {
		setUp();
		long start=System.currentTimeMillis();
		SortUtils.sort(data,SortUtils.ALGORITHM_BUBBLE);
		long end=System.currentTimeMillis();
		System.out.println("冒泡排序耗时" + (end-start)+"ms");
		check();
	}

	@Ignore
	@Test
	public void testToAlgorithmName() {
		LogUtil.show(SortUtils.toAlgorithmName(1));
		LogUtil.show(SortUtils.toAlgorithmName(2));
		LogUtil.show(SortUtils.toAlgorithmName(3));
		LogUtil.show(SortUtils.toAlgorithmName(4));
		LogUtil.show(SortUtils.toAlgorithmName(5));
		LogUtil.show(SortUtils.toAlgorithmName(6));
		LogUtil.show(SortUtils.toAlgorithmName(7));
		LogUtil.show(SortUtils.toAlgorithmName(8));
		LogUtil.show(SortUtils.toAlgorithmName(9));
	}

}
