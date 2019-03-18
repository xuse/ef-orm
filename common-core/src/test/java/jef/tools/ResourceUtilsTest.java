package jef.tools;

import org.junit.Test;

import jef.common.log.LogUtil;

public class ResourceUtilsTest {
	
	@Test
	public void testResource() {
		LogUtil.show(ResourceUtils.findResources("classpath*:META-INF/MANIFE*.MF"));
		
	}

}
