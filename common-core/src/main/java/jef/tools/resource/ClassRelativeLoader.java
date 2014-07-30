package jef.tools.resource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jef.tools.reflect.ClassLoaderUtil;

/**
 * 根据指定的class来定位资源。
 * 如果用/开头表示从该class的根目录加载，否则认为从该class的相对路径加载。
 * 
 * 
 * 下面两个属性是可以自行修改的
 * <li>directoryOnly</li>
 * 跳过jar包
 * <li>onlySamePathOfClz</li>
 * 必须从指定class所在的classpath加载，不允许使用classloader中的其他位置(无论是否设置此属性，都优先从指定class的cp中加载)
 * 
 * 
 * @author jiyi
 *
 */
public class ClassRelativeLoader extends AResourceLoader {
	private Class<?> clz;
	private boolean directoryOnly=false;
	private boolean noOtherUrlOfClassLoader=true;
	
	public ClassRelativeLoader(Class<?> clz) {
		while (clz.isArray()) {
			clz = clz.getComponentType();
		}
		this.clz = clz;
	}
	
	/**
	 * 和JDK class.getResource()的默认实现不同，这个实现是优先从指定class的加载路径去寻找资源的。
	 */
	public URL getResource(String name) {
		URL path = ClassLoaderUtil.getCodeSource(clz);
		name=resolveName(name,clz);
		if (path != null) {
			if(!(directoryOnly && toFile(path).isFile())){
				java.net.URLClassLoader ul=new java.net.URLClassLoader(new URL[]{path});
				URL url=ul.findResource(name);
				if(url!=null)return url;	
			}
			
		}
		if(noOtherUrlOfClassLoader){
			return null;
		}
		if(directoryOnly)name="./"+name;
		return clz.getClassLoader().getResource(name);
	}

	private static final String resolveName(String name,Class<?> clz) {
		if (name == null) {
			return name;
		}
		if (!name.startsWith("/")) {
			String baseName = clz.getName();
			int index = baseName.lastIndexOf('.');
			if (index != -1) {
				name = baseName.substring(0, index).replace('.', '/') + "/"+ name;
			}
		} else {
			name = name.substring(1);
		}
		int n=name.indexOf("../");
		while(n>-1){
			int front=name.lastIndexOf('/', n-2);
			if(front==-1){
				name=name.substring(n+3);
			}else{
				name=name.substring(0,front)+name.substring(n+2);
			}
			n=name.indexOf("../");
		}
		return name;
	}

	public List<URL> getResources(String name) {
		List<URL> result = new ArrayList<URL>();
		//如果限定只允许从指定class所在的classpath查找——
		if(noOtherUrlOfClassLoader){
			URL u=getResource(name);
			if(u!=null)result.add(u);
			return result;
		}
		//通用查找
		name = resolveName(name,clz);
		if(directoryOnly)name="./"+name;
		try {
			for (Enumeration<URL> e = clz.getClassLoader().getResources(name); e
					.hasMoreElements();) {
				result.add(e.nextElement());
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		return result;
	}

	public boolean isDirectoryOnly() {
		return directoryOnly;
	}

	public void setDirectoryOnly(boolean directoryOnly) {
		this.directoryOnly = directoryOnly;
	}

	public boolean isOnlySamePathOfClz() {
		return noOtherUrlOfClassLoader;
	}

	public void setOnlySamePathOfClz(boolean onlySamePathOfClz) {
		this.noOtherUrlOfClassLoader = onlySamePathOfClz;
	}

}
