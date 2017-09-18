package com.github.geequery.database;

import jef.database.wrapper.clause.SqlBuilder;

import org.junit.Test;

public class SqlBuilderTest extends org.junit.Assert{
	@Test
	public void testBuilder() {
		SqlBuilder builder = new SqlBuilder();
		builder.append("1=1]");
		builder.addBefore("[2=1 and ");
		assertEquals("[2=1 and 1=1]", builder.build().getSql());
		
		builder.startSection(" or ");
		builder.endSection();
		assertEquals("[2=1 and 1=1]", builder.build().getSql());
		
		builder.startSection(" or ");
		builder.append("a=b");
		
		builder.startSection(" 或者 ");
		builder.append("中国=杭州");
		
		builder.endSection();
		builder.append(" is null");
		builder.endSection();
		
		assertEquals("[2=1 and 1=1] or a=b 或者 中国=杭州 is null", builder.build().getSql());
	}
}
