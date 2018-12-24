package jef;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.geequery.asm.ClassReader;

import jef.accelerator.asm.commons.AnnotationData;
import jef.accelerator.asm.commons.AnnotationFetcher;
import jef.common.log.LogUtil;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration.Item;
import jef.tools.StringUtils;

public class CoreTest {

	@Test
	public void test1() {
		LogUtil.show(System.getProperties());
		System.out.println("-----------------------");
		LogUtil.show(System.getenv());
	}

	@Test
	public void testLogger() {
		Logger log = LoggerFactory.getLogger(this.getClass());
		Exception e = new ClassNotFoundException("sdfdsgfdg");
		log.error("发现一个{}的错误", "是的减肥是", e);
	}

	@Test
	public void testx() throws MalformedURLException {
		URL u = new URL("jar:file:/C:/Users/jiyi/.m2/repository/com/belerweb/pinyin4j/2.5.0/pinyin4j-2.5.0.jar!/META-INF/MANIFEST.MF");
		URL u2 = new URL("file:/C:/Users/jiyi/.m2/repository/com/belerweb/pinyin4j/2.5.0/pinyin4j-2.5.0.jar!/META-INF/MANIFEST.MF");
		System.out.println(u.getFile());
		System.out.println(u.getPath());
		System.out.println(u2.getFile());
		System.out.println(u2.getPath());
	}

	@Test
	public void test2() throws MalformedURLException {
		URL u = new URL("jar:file:/C:/Users/jiyi/.m2/repository/com/belerweb/pinyin4j/2.5.0/pinyin4j-2.5.0.jar!/META-INF/MANIFEST.MF");
		String crc = u.getPath() + StringUtils.getCRC(u.toString());
		System.out.println(crc);
		System.out.println();
	}

	@Test
	public void main1() throws IOException {
		byte[] bytes=IOUtils.toByteArray(new File("G:/Git/geequery-spring-boot/geequery-spring-boot-autoconfigure/target/classes/com/github/geequery/spring/boot/autoconfigure/GeeQueryAutoConfiguration.class"));
		ClassReader reader = new ClassReader(bytes);
		List<AnnotationData> data = AnnotationFetcher.onClass(reader);;
		System.out.println(data);
	}

	/**
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void test3() throws Exception {
		Class<Item> c = Item.class;
		{
			long start = System.nanoTime();
			// Item i=Enums.getIfPresent(c, "HTTP_TIMEOUT").orNull();
			Item i = jef.tools.reflect.Enums.valueOf(c, "HTTP_TIMEOUT", null);
			long end = System.nanoTime();
			System.out.println(end - start);
		}
		{
			long start = System.nanoTime();
			Item i = jef.tools.reflect.Enums.valueOf(c, "HTTP_TIMEOUT", null);
			long end = System.nanoTime();
			System.out.println(end - start);
		}
		{
			long start = System.nanoTime();
			Item i = jef.tools.reflect.Enums.valueOf(c, "HTTP_TIMEOUT", null);
			long end = System.nanoTime();
			System.out.println(end - start);
		}
		{
			long start = System.nanoTime();
			Item i = jef.tools.reflect.Enums.valueOf(c, "HTTP_TIMEOUT", null);
			long end = System.nanoTime();
			System.out.println(end - start);
		}
	}

	@Test
	public void test4() {
		System.out.println(StringUtils.matches("dsds1233", "*123", false));
	}
}
