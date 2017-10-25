package org.googlecode.jef.spring;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "classpath:simple/simple-spring-test.xml" })
public class SimpleSpringTest extends AbstractJUnit4SpringContextTests{
	@Test
	public void testPropertiesFile(){
		System.out.println(System.getProperty("db.name"));
	}
}
