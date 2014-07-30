package jef.tools;

import org.junit.Test;

public class PageInfoTest {
	@Test
	public void test123(){
		PageInfo info=new PageInfo(1024,10);
		info.setOffset(12);
		System.out.println(info);
		
		info.setCurPage(3);
		System.out.println(info);		
		
		info.setCurPage(12);
		System.out.println(info);
		
		System.out.println(info.setCurrentPageByOffset(0));
		System.out.println(info.setCurrentPageByOffset(9));
		System.out.println(info.setCurrentPageByOffset(10));
		System.out.println(info.setCurrentPageByOffset(19));
		System.out.println(info.setCurrentPageByOffset(20));
		System.out.println(info.setCurrentPageByOffset(21));
		
	}

}
