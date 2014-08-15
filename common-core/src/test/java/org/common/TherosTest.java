package org.common;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class TherosTest extends org.junit.Assert{
	@DataPoint
	public static Integer INT_VAL=1;
	
	@DataPoint
	public static Integer INT_VAL2=2;

	@DataPoint
	public static String GOOD_USERNAME = "optimus";
	@DataPoint
	public static String USERNAME_WITH_SLASH = "optimus/prime";

	@Theory
	public void filenameIncludesUsername(Integer aa,String username,String pics) {
		System.out.println(aa+"|"+username+"|"+pics);
		//assumeThat(username, not(containsString("/")));
		//assertThat(new User(username).configFileName(), containsString(username));
	}

}
