package org.easyframe.tutorial.lessonb;

import jef.database.datasource.PropertiesDataSourceLookup;
import jef.tools.Assert;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;


@ContextConfiguration(locations = { "classpath:spring/spring-ds-test1.xml" })
public class DatasourceLookupTest extends AbstractJUnit4SpringContextTests {
	@Test
	public void test1123(){
		PropertiesDataSourceLookup rds=applicationContext.getBean(PropertiesDataSourceLookup.class);
		Assert.notNull(rds);
		
		System.out.println(rds.getDataSource("ds1"));
		System.out.println(rds.getAvailableKeys());
		
	}
	
	

}
