package jef.tools.resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface ResourceLoader {
	
	URL getResource(String name);
	
	/**
	 * 将资源读取为流，注意使用后要关闭
	 * @param name
	 * @return
	 */
	InputStream getResourceAsStream(String name);
	
	/**
	 * 将资源读取为字符文本，注意使用后要关闭
	 * @param name
	 * @param charset 编码，可以设置为null，会自动检测编码
	 * @return
	 */
	BufferedReader getResourceAsReader(String name,String charset);
	
	/**
	 * 得到所有符合条件的资源
	 * @param name
	 * @return
	 */
	List<URL> getResources(String name);
	
	
	/**
	 * 以JEF封裝的resource格式返回。
	 * 资源一般用URL来描述，包含了网络资源、本地资源、压缩包中的资源。
	 * 
	 * Resource接口封裝了不同協議的资源的差异。
	 * 并且包含了常用的资源操作接口。
	 * 
	 * 最常见对资源的操作无非是读取，解析等等。
	 * 
	 * 也不排除某些特别的实现，可以将上述特殊位置的以內定的協議映像成本地文件，從而可以提供File格式的资源。
	 * 但是file格式的资源如果允许修改，那么这类实现将较难处理远程资源和本地修改后资源的冲突问题。
	 * @param name
	 * @return
	 */
	Resource getResourceEx(String name);
	/**
	 * 得到封装后的资源对象
	 * @param name
	 * @return
	 */
	List<Resource> getgetResourcesEx(String name);
}
