import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;


public class Urltest {
	@Test
	public void testUrl() throws MalformedURLException{
		URL u=new URL("http://10.19.132.101:8081/apidocs#1?aaa");
		String path=u.getPath();
		System.out.println(path);
	}

}
