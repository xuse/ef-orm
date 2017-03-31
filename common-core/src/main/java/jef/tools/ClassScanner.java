package jef.tools;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;

import jef.tools.reflect.ClassLoaderUtil;
import jef.tools.resource.IResource;

/**
 * 扫描指定包（包括jar）下的class文件 <br>
 * 
 * @author jiyi
 */
public class ClassScanner {
	/**
	 * 是否排除内部类
	 */
	private boolean excludeInnerClass = true;

	/**
	 * class path根路径，如果不指定那么就在所有ClassPath下寻找
	 * 
	 * @return
	 */
	private URL rootClasspath;

	public boolean isExcludeInnerClass() {
		return excludeInnerClass;
	}

	public ClassScanner excludeInnerClass(boolean excludeInnerClass) {
		this.excludeInnerClass = excludeInnerClass;
		return this;
	}

	public ClassScanner rootClasspath(URL rootClasspath) {
		this.rootClasspath = rootClasspath;
		return this;
	}

	/**
	 * 扫描包
	 * 
	 * @param basePackage
	 *            基础包
	 * @param recursive
	 *            是否递归搜索子包
	 * @return Set
	 */
	public Set<String> scan(String[] packages) {
		URLClassLoader cl = rootClasspath == null ? null : new URLClassLoader(new URL[] { rootClasspath });

		Set<String> classes = new LinkedHashSet<String>();
		for (String packageName : packages) {
			IResource[] res = ResourceUtils.findResources(cl,"classpath*:"+ packageName.replace('.', '/') + "/*.class");
			for (IResource r : res) {
				String name = r.getFilename();
				name = name.substring(0, name.length() - 6);
				if (name.indexOf('$') > -1 && excludeInnerClass) {
					continue;
				}
				classes.add(packageName + "." + name);
			}
		}
		return classes;
	}

	/**
	 * 用相同类的一个已经加载的类来设置要搜索的classpath
	 * 
	 * @param rootCls
	 */
	public void setRootBySameUrlClass(Class<?> rootCls) {
		if (rootCls == null)
			return;
		this.rootClasspath = ClassLoaderUtil.getCodeSource(rootCls);
		if (rootClasspath == null)
			rootClasspath = rootCls.getResource("/");
	}
	
	public static String[] listClassNameInPackage(Class<?> rootCls, String[] pkgNames, boolean includeInner) {
		ClassScanner cs = new ClassScanner().excludeInnerClass(!includeInner);
		cs.setRootBySameUrlClass(rootCls);
		Set<String> result = cs.scan(pkgNames);
		return result.toArray(new String[result.size()]);
	}
	
	public static String[] listClassNameInPackage(File root, String[] pkgNames, boolean includeInner) {
		ClassScanner cs=new ClassScanner().excludeInnerClass(!includeInner);
		try {
			cs.rootClasspath(root.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
		Set<String> result=cs.scan(pkgNames);
		return result.toArray(new String[result.size()]);
	}
	
}
