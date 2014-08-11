package jef.testbase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jef.common.StringCacheMap;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.tools.ArrayUtils;
import jef.tools.DateUtils;
import jef.tools.IOUtils;
import jef.tools.ResourceUtils;
import jef.tools.StringUtils;
import jef.tools.TextFileCallback;
import jef.tools.ThreadUtils;
import jef.tools.reflect.CloneUtils;
import jef.tools.resource.ResourceLoader;
import jef.tools.string.RandomData;
import jef.tools.support.NumberText;
import jef.tools.support.TimeIterable;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author jiyi
 *
 */
@SuppressWarnings("rawtypes")
public class JefTest extends Assert {
	private static final String HEAD_HTML_RESOURCE   = "--------------------7d71b526e00e4\r\n" + 
			"Content-Location: \"%s\"\r\n" +			
			 "\r\nContent-Type: %s\r\n\r\n"; // 每个文件部分的开头
	
	
	public static void main(String[] args) {
		String s="java.lang.Object".replaceAll(".","_");
		System.out.println(s);

	}
	/**
	 * 拷贝文件
	 * @param file1 源文件
	 * @param file2 目标文件
	 * @throws IOException
	 */
	public static void copyFile(String file1,String file2) throws IOException {
		InputStream in=new FileInputStream(file1);;
		OutputStream out=new FileOutputStream(file2);
		try{
			while(true){
				byte[] buffer=new byte[1024];
				int len=in.read(buffer);
				if(len==-1){
					break;
				}else{
					out.write(buffer, 0, len);
				}
			}	
		}finally{
			in.close();
			out.flush();
			out.close();
		}
	}
	
	/**
	 *  您<em>甚至</em><i>可以</i>插入一个列表：
    * <ol>
    * <li> 项目一
    * <li> 项目二
    * <li> 项目三
    * </ol>
	 */
	@Test
	public void processjavaFile() throws IOException{
		
		IOUtils.processFiles(new File("E:\\MyWork\\jef\\support-lib\\"), new TextFileCallback("UTF-8") {
			@Override
			protected String processLine(String line) {
				if(line.startsWith("/* $Id:")){
					System.out.println(this.sourceFile.getPath());
					return null;
				}
				return line;
				
			}

			@Override
			protected Dealwith dealwithSourceOnSuccess(File source) {
				return Dealwith.REPLACE;
			}
			
		},"java");
	}
	
	@Test
	public void process() throws Exception{
		System.out.println(StringUtils.formatSize(Long.MAX_VALUE));
	}
	

	
	@Test
	public void testNullKey(){
		String x=null;
		Map<String,String> map=new HashMap<String,String>();
		map.put(null, "1");
		map.put("", "2");
		map.put(null, "3");
		System.out.println(map.get(null));
		
	}
	
	@Test
	@Ignore
	public void testWait() throws InterruptedException{
		final CountDownLatch countdown=new CountDownLatch(1);
		Thread t1=new Thread(){
			@Override
			public void run() {
				System.out.println("t1 start:"+System.currentTimeMillis());
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("t1 stop:"+System.currentTimeMillis());
			}
			
		};
		Thread t2=new Thread(){
			@Override
			public void run() {
				System.out.println("t2 start:"+System.currentTimeMillis());
				try {
					countdown.await(30000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("t2 stop:"+System.currentTimeMillis());
			}
		};
		t1.start();
		t2.start();
		countdown.await();
		
	}
	
	@Test
	public void testSttinh(){

		String[] arrays=new String[]{"1","2","3"};
		Map<String,String> map=new HashMap<String,String>();
		for(int i=0;i<arrays.length;i+=2){
			if(i==arrays.length-1){
				map.put(arrays[i], null);
			}else{
				map.put(arrays[i], arrays[i+1]);
			}
		}
		System.out.println(map);
	}
	
	private static final String AXXX="";
	private String a="aaa";
	private String b="dsjkjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj";
	
	@Test
	public void main1x() {
		WeakHashMap<Object, String> aa=new WeakHashMap<Object, String>();
		aa.put(new Integer(123456), "123");
		System.out.println(aa.size());
		System.gc();
		System.gc();
		ThreadUtils.doSleep(1000);
		System.out.println(aa.get(new Integer(123456)));
	}
	
	
	public void saveDocumentAsFile(Document doc,String filepath) throws IOException{
		
	}
	
	@Test
	public void cloneTest() throws CloneNotSupportedException{
		Parent p=new Parent();
		
		Parent clone=(Parent) CloneUtils.clone(p);
		System.out.println(p);
		System.out.println(clone);
		
	}
	
	
	@Test
	public void testRemoveDup10(){
		String[] array=RandomData.randomStringArray(5, 20);
		array=(String[]) ArrayUtils.addAll(array, array);
//		LogUtil.show(array);
		
//		8652137
//		14833426
		
//LinkedHashSet          HashSet
//		-server 
//		14543493   		14506930  
//		54659492        46454442   
		
//去除数据导出后		
//		                 10992130
//		                 23243418
		
		
		long start=System.nanoTime();
		for(int i=0;i<10000;i++){
			List<String> list = new ArrayList<String>();
			for (String str : array) {
				if (!list.contains(str))
					list.add(str);
			}
			String[] result= list.toArray(new String[list.size()]);
		}
		System.out.println(System.nanoTime()-start);
		start=System.nanoTime();
		for(int i=0;i<10000;i++){
			Set<String> list = new HashSet<String>();//如果要保证数据不会打乱，那么要使用LinkedHashSet，速度更慢
			for (String str : array) {
				list.add(str);
			}
			String[] result= list.toArray(new String[list.size()]);
		}
		System.out.println(System.nanoTime()-start);
	}
	
	@Test
	public void testNSS(){
		long start=System.nanoTime();
		for(int j=0;j<10000;j++){
			double x=54534333d/43454.3;
		}
		System.out.println(System.nanoTime()-start);
		

		
	}
	
	@Test
	public void testRemoveDup500(){
		String[] array=RandomData.randomStringArray(250, 40);
		array=(String[]) ArrayUtils.addAll(array, array);
		LogUtil.show(array.length);
		int rSize=0;
		
		long start=System.nanoTime();
		for(int i=0;i<1000;i++){
			List<String> list = new ArrayList<String>();
			for (String str : array) {
				if (!list.contains(str))
					list.add(str);
			}
//			String[] result= list.toArray(new String[list.size()]);
//			rSize=result.length;
		}
		System.out.println(System.nanoTime()-start);
		System.out.println(rSize);
		start=System.nanoTime();
		for(int i=0;i<1000;i++){
			Set<String> list = new HashSet<String>();
			for (String str : array) {
				list.add(str);
			}
//			String[] result= list.toArray(new String[list.size()]);
//			rSize=result.length;
		}
		System.out.println(System.nanoTime()-start);
		System.out.println(rSize);
	}
	
	
	
	@Test
	public void testUrl() throws Exception{
		String s="%E5%A5%BD";
		System.out.println(StringUtils.urlDecode(s));
		
	}


	@Test
	public void testList() throws IOException{
		String s="asasasasas\nbbbbbbd\n0";
		StringReader r=new StringReader(s);
		String line=IOUtils.readTill(r, '\n');
		while(line!=null){
			System.out.println(line);
			line=IOUtils.readTill(r, '\n');
		}
	}
	
	public synchronized void methodLocked(int owner) {
		System.out.println(Thread.currentThread().getName() + " is calling "
				+ owner);
		if (owner == 1) {
			ThreadUtils.doSleep(100000);
			System.out.println(Thread.currentThread().getName() + " finished.");
		} else {
			System.out.println("方法执行了!");
		}
	}
	@Test
	public void testResourcePath() throws IOException, URISyntaxException {
		LogUtil.show(this.getClass().getResource("/META-INF"));
		System.out.println(  "----------------");
		LogUtil.show(this.getClass().getResource("."));
		System.out.println(  "----------------");
		
		//1.CL永远不要将/作为path的第一个字符
		//./是由含义的，表示目录，从而避免查找jar包
		LogUtil.show(this.getClass().getClassLoader().getResources("./META-INF"));
		System.out.println(  "----------------");
		LogUtil.show(this.getClass().getClassLoader().getResources("META-INF"));
		
	}
	@Test
	public void tesdResourcePath2() throws IOException, URISyntaxException {
		ResourceLoader l=new jef.tools.resource.ClassRelativeLoader(net.sourceforge.pinyin4j.PinyinHelper.class);
		URL u = l.getResource("../../../META-INF/MANIFEST.MF");
		// URLClassPath uc=new URLClassPath(new URL[]{new URL("/")});
		
		System.out.println(u);
		
		LogUtil.show(ArrayUtils.toArray(this.getClass().getClassLoader()
				.getResources("META-INF/MANIFEST.MF"), URL.class));
	}
	
	
	@Test
	public void testResourceUtil() throws IOException {
		int size1=ArrayUtils.toArray(getClass().getClassLoader().getResources("./META-INF/MANIFEST.MF"), URL.class).length;
		int size2=ResourceUtils.getResources("META-INF/MANIFEST.MF",true).size();
		assertEquals(size1, size2);
	}
	
	
	@Test
	public void testMap(){
		Map<String, Object> map1=new java.util.IdentityHashMap<String,Object>();
//		jef.accelerator.IdentityHashMap<String,Object> map1=new jef.accelerator.IdentityHashMap<String,Object>();
		long start=System.nanoTime();
		for(int i=0;i<100000;i++){
			map1.put(String.valueOf(i), this);
		}
		System.out.println(System.nanoTime()-start);
		start=System.nanoTime();
		for(int i=100000;i>=1;i--){
			map1.get(String.valueOf(i));
		}
		System.out.println(System.nanoTime()-start);
//		SetMultimap<String,String> m1=Multimaps.synchronizedSetMultimap(HashMultimap.create());
//		ListMultimap<String,String> m2=Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
	}

	@Test
	@Ignore
	public void main1() {
		if (ThreadUtils.tryLock(this)) {
			System.out.println("获得锁");
			methodLocked(0);
			// 同步快出来后，如果没有手工释放锁，那么其实没有释放，因此手工释放是必须的
			ThreadUtils.unlock(this);
		}
		Thread t = new Thread() {
			@Override
			public void run() {
				System.out.println(ThreadUtils.isLocked(JefTest.this));
				methodLocked(1);
			}
		};
		t.start();

		ThreadUtils.doSleep(10000);
	}

	@Test
	public void testStringCacheMap() {
		StringCacheMap sc = new StringCacheMap(5, "Luas");
		for (int i = 2; i < 50; i++) {
			String key = "key" + i;
			String value = RandomData.randomChineseName(20, 100);
			sc.put(key, value);
		}
		System.out.println(sc.size());
		for (int i = 0; i < 52; i++) {
			String key = "key" + i;
			String value = sc.get(key);
			System.out.println("---" + key + "----");
			System.out.println(value);
		}
	}

	@Test
	public void count1x() throws IOException {
		double d1 = 1.40;
		double d2 = 1.49;
		System.out.println(String.format("%.1f", d1));
		System.out.println(String.format("%.1f", (int) (d2 * 10) / 10f));
	}

	/**
	 * TODO ims_war_101.log is required，故先ingore
	 * 
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void count1() throws IOException {
		final Map<String, String> trans = new HashMap<String, String>();

		IOUtils.processFile(new File("E:/ims/ims_war_101.log"),
				new TextFileCallback( "UTF-8") {
					@Override
					protected String processLine(String line) {
						// [JPA DEBUG]:Transaction [Tx472720354@(service_name =
						// shzw):OB60@112] started at
						// jef.database.JefEntityManager@4da8a55
						if (line.indexOf("] started at jef.database.JefEntityManager@") > -1) {// 开始
							String tx = StringUtils.substringBetween(line,
									":Transaction [", "] started at ");
							trans.put(tx, null);
						} else if (line.indexOf("] commited.[") > -1) {
							String tx = StringUtils.substringBetween(line,
									":Transaction [", "] commited.[");
							if (trans.containsKey(tx)) {
								trans.put(tx, "commited");
							} else {
								System.out.println("Warn:" + tx
										+ "commit, but no start.");
							}
						} else if (line.indexOf("] rollback.[") > -1) {
							// [JPA DEBUG]:Transaction
							// [Tx649644295@(service_name =
							// shzw):OB60@110] rollback.[3ms]
							String tx = StringUtils.substringBetween(line,
									":Transaction [", "] rollback.");
							if (trans.containsKey(tx)) {
								trans.put(tx, "rollback");
							} else {
								System.out.println("Warn:" + tx
										+ "commit, but no start.");
							}
						}
						return null;
					}

					@Override
					protected File getTarget(File source) {
						return null;
					}
				});
		int commit = 0;
		int rollbalc = 0;
		int failure = 0;
		for (Map.Entry<String, String> e : trans.entrySet()) {
			if (e.getValue() == null) {
				System.out.println(e.getKey() + " 未提交或回滚");
				failure++;
			} else if ("commited".equals(e.getValue())) {
				commit++;
			} else if ("rollback".equals(e.getValue())) {
				System.out.println(e.getKey() + " rollback");
				rollbalc++;
			}
		}

		System.out.println("Commit:" + commit + "   Rollback:" + rollbalc
				+ "    no:" + failure);
	}

	Map<String, Object> context = new HashMap<String, Object>();

	/**
	 * TODO so_JOB_LOG is required，故先ingore
	 * 
	 * @throws Exception
	 */
//	@Ignore
//	@Test
//	public void testLogAnaly() throws Exception {
//		File[] list = IOUtils
//				.listFiles(new File("E:/so_JOB_LOG"), "log", "txt");
//		BufferedReader reader = IOUtils.getReader(
//				new MultiFileInputStream(list), "GB18030");
//
//		int count = 0;
//		long length = 0;
//		String line;
//		StringBuilder sb = new StringBuilder();
//		int MAX = 10;
//		while ((line = reader.readLine()) != null) {
//			count++;
//			length += line.length();
//			if (line.startsWith("####")) {
//				if (sb.length() > 0) {
//					LogInfo l = parseLog(sb.toString());
//					staticLog(l);
//					sb.setLength(0);
//				}
//				sb.append(line);
//			} else {
//				sb.append("\r\n").append(line);
//			}
//			if (MAX > 0 && count > MAX) {
//				break;
//			}
//		}
//		reader.close();
//		System.out.println("count=" + count + "  length=" + length);
//	}

	/**
	 * TODO 依赖特定测试文件，故先ingore
	 * 
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void ana3() throws IOException {
		final Set<String> set = new HashSet<String>();
		IOUtils.processFile(new File("E:/connectionUsed/sunyz1"),
				new TextFileCallback() {
					@Override
					protected String sourceCharset(File source) {
						return "UTF-8";
					}

					@Override
					protected File getTarget(File source) {
						return new File(source.getAbsolutePath() + ".txt");
					}

					@Override
					protected String targetCharset() {
						return "UTF-8";
					}

					@Override
					protected String processLine(String line) {
						if (!line.startsWith("24537"))
							return null;
						if (line.indexOf("(service_name = shzw)") > -1) {
							String threadId = org.apache.commons.lang.StringUtils
									.substringBetween(line,
											"(service_name = shzw):OB60@", "]");
							set.add(threadId);
							return threadId + " " + line;
						}
						return null;
						// return line;
					}
				});

		System.out.println(set.size());
		LogUtil.show(set);
	}

//	private void staticLog(LogInfo l) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private LogInfo parseLog(String line) {
//		int index = line.indexOf('|', 5);
//		int indexThread = line.indexOf('|', index + 17);
//		String time = line.substring(5, index);
//		String thread = line.substring(index + 17, indexThread);
//		System.out.println(time);
//		System.out.println(thread);
//		System.out.println(line.substring(indexThread + 1));
//		return null;
//	}


	@Test
	public void test() throws UnsupportedEncodingException {
//		Deque d=new ArrayDeque<String>();
		Deque d=new LinkedList<String>();
		String a="asd";
		long start=System.currentTimeMillis();
		for(int i=0;i<100000;i++){
			d.push(a);
		}
		System.out.println(System.currentTimeMillis()-start);
		start=System.currentTimeMillis();
		for(int i=0;i<100000;i++){
			d.pop();
		}
		System.out.println(System.currentTimeMillis()-start);
	}
	
	@Test
	public void doubleDelete(){
		String[] a=new String[]{"aaa","bbb","cccc","ddd","eeeee"};
		String[] b=new String[]{"aaa","bbb","cccc","ddd","eeeee"};
		for(int i=0;i<a.length;i++){
			String s1=a[i];
			if(s1.length()>3){
				a=(String[]) ArrayUtils.remove(a,i);
				b=(String[]) ArrayUtils.remove(b,i);
				i--;
			}
		}
		System.out.println(Arrays.toString(a));
		System.out.println(Arrays.toString(b));
		
	}

	@Test
	public void test1() throws UnsupportedEncodingException {
		String s = "\u9369\u9e3f\u77fe\u5bf0\ufffd";
		System.out.println(StringUtils.convert(s.getBytes("GB18030"), "UTF-8"));

	}

	@Test
	public void test3() {
		byte[] value = new byte[60];
		int left = value.length;
		int times = 0;
		while (left > 16) {
			System.out.println(times);
			System.out.println(times * 16 + " " + (times * 16 + 16) + "len=16");
			left -= 16;
			times++;
		}
		System.out.println(times);
		System.out.println(times * 16 + " " + value.length + "len=" + left);
	}
	
	@Test
	public void testReserve2Point(){
		double f=12.345f;
		{
			BigDecimal b=new BigDecimal(1234.345);
			b.setScale(2, BigDecimal.ROUND_DOWN).doubleValue();	
		}
		
		//-----------
		
		long start=System.nanoTime(); 
		
		long end=System.nanoTime();
		System.out.println(end-start);
		System.out.println(f);
	}
	
	@Test
	public void testRehexp(){
		String s="select T1.name AS T1__name,T1.id AS T1__id,T1.parentId AS T1__parentId,T1.desc1 AS T1__desc,T2.name AS T2__name from treetable T1 LEFT JOIN leaf T2 ON T1.id=T2.id where T1.name=? and T2.childId=? order by T2.id ASC | [mysql:test@1]"+
		"\n(1)name:         	[a]"+
		"\n(2)childId:      	[12]";
		
		Pattern p=Pattern.compile("select .* where (.+) order by (.+) \\|.+",Pattern.DOTALL);
		Matcher m=p.matcher(s);
		System.out.println(m.matches()+" "+m.groupCount());
		for(int i=0;i<m.groupCount();i++){
			System.out.println(m.group(i+1));	
		}
		
		
	}
	
	@Test
	public void testCal(){
		Locale jp=new Locale("ja","JP","JP");
		Calendar c=Calendar.getInstance(jp);
		System.out.println(c.getDisplayName(Calendar.ERA,Calendar.LONG,jp)+NumberText.getInstance().getText(c.get(Calendar.YEAR))+"年");
		
//		LogUtil.show(c.getDisplayNames(Calendar.ERA, Calendar.ALL_STYLES, jp));
	}
	
	private Class c=String.class;
	@Test
	public void getClz1(){
		//str.replaceAll("\\sas[^,]+(,?)", "$1");
		System.out.println("select ff as asa, dfsfd as asd, sfsdf as dfsdf form ddsa".replaceAll("(select\\s?)\\D+\\sas[^,]+(,?)", "$1"));
	}
	
	@Test
	public void getClz2(){//12225 9300//
		
		dol(Integer.TYPE);
		dol(Short.TYPE);
		dol(Long.TYPE);
		dol(Boolean.TYPE);
		dol(Float.TYPE);
		dol(Double.TYPE);
		dol(Character.TYPE);
		dol(Byte.TYPE);
	}
	
	private void dol(Class<?> type) {
		String name=type.getName();
		int x=name.charAt(1)+name.charAt(2);
		System.out.println(type.getName()+"  "+x);
		
	}
	@Test
	public void getClz3(){//8660
		IntRange i=new IntRange(1,200);
		System.out.println(i.contains(Integer.valueOf(-1)));
		System.out.println(i.contains(Integer.valueOf(0)));
		System.out.println(i.contains(Integer.valueOf(1)));
		System.out.println(i.contains(Integer.valueOf(13)));
		System.out.println(i.contains(Integer.valueOf(199)));
		System.out.println(i.contains(Integer.valueOf(200)));
		System.out.println(i.contains(Integer.valueOf(201)));
		System.out.println(i.contains(Integer.MAX_VALUE));
	}
	
	@Test
	public void testIterator(){
		Date start=new Date();
		Date end=new Date();
		DateUtils.addDay(end, 34);
		System.out.println(start+" ~ "+ end);
		System.out.println("====DAYS====");
		for(Date d:jef.tools.DateUtils.dayIterator(start, end)){
			System.out.println(d);
		}
		System.out.println("====MONTHS====");
		end=DateUtils.getDate(2013, 10, 2);
		System.out.println(start+" ~ "+ end);
		for(Date d:jef.tools.DateUtils.monthIterator(start, end)){
			System.out.println(d);
		}
		
		System.out.println("====HOURS====");
		end=DateUtils.getDate(2013, 9, 30);
		System.out.println(start+" ~ "+ end);
		for(Date d: new TimeIterable(start, end, Calendar.HOUR)){
			System.out.println(d);
		}
	}
}
