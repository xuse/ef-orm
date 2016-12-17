package jef.codegen;

import org.junit.Test;

public class EntityEnhancerTest {

	@Test
	public void testEnhanceClass() {
		new EntityEnhancer().enhanceClass(Person.class.getName());
		
	
	}

}
