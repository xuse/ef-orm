package jef.tools;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.tools.algorithm.BFPRT;
import jef.tools.algorithm.Sorts;
import jef.tools.string.RandomData;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class SortUtilsTest {
	private Comparable[] data;
	private static int MAX_LENGTH=50000;
	private static Comparable[] rawData;
	
	@BeforeClass
	public static void prepareData(){
		rawData=new Integer[MAX_LENGTH];
		for(int i=0;i<MAX_LENGTH;i++){
			rawData[i]= RandomData.randomInteger(0, 200000);
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
		Sorts.sort(data);
		check();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSortTArrayInt() throws Exception {
		long start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_HEAP);
		long end=System.currentTimeMillis();
		System.out.println("堆排序耗时" + (end-start)+"ms");
		check();
		
		//冒泡排序太慢了，移出
		if(data.length<10000){
			testBubble();
		}else{
			System.out.println("冒泡排序太耗时，跳过测试。");
		}

		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_INSERT);
		end=System.currentTimeMillis();
		System.out.println("插入 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_MERGE);
		end=System.currentTimeMillis();
		System.out.println("归并 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_QUICK);
		end=System.currentTimeMillis();
		System.out.println("快速 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_SELECTION);
		end=System.currentTimeMillis();
		System.out.println("选择 排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_SHELL);
		end=System.currentTimeMillis();
		System.out.println("希尔排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_IMPROVED_MERGE);
		end=System.currentTimeMillis();
		System.out.println("归并 改排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_IMPROVED_QUICK);
		end=System.currentTimeMillis();
		System.out.println("快速 改排序耗时" + (end-start)+"ms");
		check();
		
		setUp();
		start=System.currentTimeMillis();
		Arrays.sort(data);
		end=System.currentTimeMillis();
		System.out.println("Arrays.sort排序耗时" + (end-start)+"ms");
		check();
	}

	@Ignore
	@SuppressWarnings("unchecked")
	private void testBubble() throws Exception {
		setUp();
		long start=System.currentTimeMillis();
		Sorts.sort(data,Sorts.ALGORITHM_BUBBLE);
		long end=System.currentTimeMillis();
		System.out.println("冒泡排序耗时" + (end-start)+"ms");
		check();
	}

	@Ignore
	@Test
	public void testToAlgorithmName() {
		LogUtil.show(Sorts.toAlgorithmName(1));
		LogUtil.show(Sorts.toAlgorithmName(2));
		LogUtil.show(Sorts.toAlgorithmName(3));
		LogUtil.show(Sorts.toAlgorithmName(4));
		LogUtil.show(Sorts.toAlgorithmName(5));
		LogUtil.show(Sorts.toAlgorithmName(6));
		LogUtil.show(Sorts.toAlgorithmName(7));
		LogUtil.show(Sorts.toAlgorithmName(8));
		LogUtil.show(Sorts.toAlgorithmName(9));
	}

	@Test
	public void testBFPRT() {
		Comparable[] result = Arrays.copyOf(data, data.length);
		Arrays.sort(result);

		System.out.println("=====================");
		// 计算
		doTest(data, 0, data.length , 1, result);
		doTest(data, 0, data.length , 3, result);
		doTest(data, 0, data.length , 6, result);
		doTest(data, 0, data.length , 12, result);
		doTest(data, 0, data.length , data.length / 2, result);
		doTest(data, 0, data.length , data.length, result);

	}

	private void doTest(Comparable[] data, int i, int j, int k, Comparable[] orderd) {
		Comparable x =BFPRT.get(data, i, j, k);
		System.out.println("第" + k + "个元素是:" + x);
		Assert.assertEquals(x, orderd[k - 1]);
	}
	
}
