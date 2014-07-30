package jef.codegen;

import jef.codegen.support.RegexpNameFilter;

import org.junit.Test;

public class RegexpNameFilterTest extends org.junit.Assert{
	@Test
	public void test() {
		RegexpNameFilter filter=new RegexpNameFilter();
		assertTrue(filter.accept("abc123.s1"));
		
		
		filter=new RegexpNameFilter("abc123\\..+1");
		assertTrue(filter.accept("abc123.s1"));
		assertFalse(filter.accept("abc123.s2"));
		
		filter=new RegexpNameFilter(null,"abc123\\..+2");
		assertTrue(filter.accept("abc123.s1"));
		assertFalse(filter.accept("abc123.s2"));
	}
}
