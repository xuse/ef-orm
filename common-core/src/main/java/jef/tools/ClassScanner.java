package jef.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jef.codegen.support.NameFilter;
import jef.common.log.LogUtil;
import jef.tools.reflect.ClassLoaderUtil;

/**
 * 扫描指定包（包括jar）下的class文件 <br>
 * @author jiyi
 */
public class ClassScanner {
	/**
	 * 是否排除内部类 
	 */
	private boolean excludeInnerClass = true;
	
	/**
	 * 是否搜索jar包内部
	 */
	private boolean searchJar = true;

	/**
	 * 过滤器，默认为null，不过滤
	 */
	private NameFilter filter = null;
	
	/**
	 * class path根路径
	 * @return
	 */
	private URL rootClasspath;
	
	/**
	 * 是否递归查找
	 */
	private boolean recursive=true;
	

	public boolean isExcludeInnerClass() {
		return excludeInnerClass;
	}

	public void setExcludeInnerClass(boolean excludeInnerClass) {
		this.excludeInnerClass = excludeInnerClass;
	}

	public boolean isSearchJar() {
		return searchJar;
	}

	public void setSearchJar(boolean searchJar) {
		this.searchJar = searchJar;
	}

	public void setFilter(NameFilter filter) {
		this.filter = filter;
	}


	public void setRootClasspath(URL rootClasspath) {
		this.rootClasspath = rootClasspath;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
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
	public Set<String> scan(String... packages) {
		Set<String> classes = new LinkedHashSet<String>();
//		String[] packageName = basePackage[];
//		if (packageName.endsWith(".")) {
//			packageName = packageName.substring(0, packageName.length()-1);
//		}
		if(rootClasspath==null){ //未指定根路径下的情况，全部classpath搜索
			for(String packageName:packages){
				String packagePath = packageName.replace('.', '/');
				Enumeration<URL> dirs;
				try {
					dirs = Thread.currentThread().getContextClassLoader().getResources(packagePath);
					while (dirs.hasMoreElements()) {
						URL url = dirs.nextElement();
						String protocol = url.getProtocol();
						if ("file".equals(protocol)) {
							String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
							doScanPackageClassesByFile(classes, packageName, new File(filePath));
						} else if ("jar".equals(protocol)) {
							doScanPackageClassesByJar(new String[]{packageName}, url, classes);
						}
					}
				} catch (IOException e) {
					LogUtil.exception("IOException error:", e);
				}
			}
		}else{
			File root=IOUtils.urlToFile(rootClasspath);
			if(root.isFile() && searchJar){//对JAR文件的处理
				doScanPackageClassesByJar(packages, rootClasspath, classes);
			}else{
				if(packages.length==0){
					doScanPackageClassesByFile(classes,"",root);
				}else{
					for (String packageName : packages) {
						File packageFile = new File(root, packageName.replace('.', '/'));
						doScanPackageClassesByFile(classes,packageName,packageFile);
					}	
				}
			}
		}
		return classes;
	}

	/*
	 * 以jar的方式扫描包下的所有Class文件<br>
	 * 
	 * @param basePackage
	 *            eg：michael.utils.
	 * @param url
	 * @param recursive
	 * @param classes
	 */
	private void doScanPackageClassesByJar(String[] basePackage, URL url, Set<String> classes) {
		String[] packageNames = basePackage;
		for(int i=0;i<packageNames.length;i++){
			packageNames[i]=packageNames[i].replace('.', '/');
		}
		JarFile jar=null;
		try {
			File f=IOUtils.urlToFile(url);
			if(f instanceof URLFile){
				jar=new JarFile(((URLFile) f).getZipContainer());
			}else{
				jar=new JarFile(url.getFile());
			}
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				String name = e.getName();
				if (e.isDirectory())
					continue;
				if (!name.endsWith(".class")) {
					continue;
				}
				//去除内部类
				if (name.indexOf('$') > -1 && excludeInnerClass) {
					continue;
				}
				//获得匹配的包名
				String matchedPackage=getMatchedPackage(name,packageNames);
				if(matchedPackage==null){
					continue;
				}
				// 子包中的类，不符合要求
				if (!recursive && name.lastIndexOf('/') != matchedPackage.length()) {
					continue;
				}
				//计算类名
				String className=name.substring(0, name.length() - 6).replace('/', '.');
				//过滤
				if (filter==null || filter.accept(className)) {
					classes.add(className);	
				}
			}
		} catch (IOException e) {
			LogUtil.exception(e);
		}finally{
			closeJarFile(jar);
		}
	}

	//计算匹配的类名,在jar包扫描时使用
	private String getMatchedPackage(String name, String[] packageNames) {
		for(String p:packageNames){
			if(name.startsWith(p)){
				return p;
			}
		}
		return null;
	}

	private static void closeJarFile(JarFile jar) {
		if(jar!=null){
			try {
				jar.close();
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
	}

	/*
	 * 以文件的方式扫描包下的所有Class文件
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	private void doScanPackageClassesByFile(final Set<String> classes, final String packageName, File packagePath) {
		if (!packagePath.exists() || !packagePath.isDirectory()) {
			return;
		}
		final boolean fileRecursive = recursive;
		File[] dirfiles = packagePath.listFiles(new FileFilter() {
			// 自定义文件过滤规则
			public boolean accept(File file) {
				if (file.isDirectory()) {
					return fileRecursive;
				}
				String filename = file.getName();
				//排除非class
				if(!filename.endsWith(".class")){
					return false;
				}
				//排除内部类
				if (excludeInnerClass && filename.indexOf('$') != -1) {
					return false;
				}
				//计算类名
				StringBuilder sb = new StringBuilder(packageName);
				if(!StringUtils.isEmpty(packageName)){
					sb.append('.');	
				}
				sb.append(filename.substring(0,filename.length()-6));//去除头
				String className = sb.toString();
				if(filter==null || filter.accept(className)){
					classes.add(className);
				}
				return false;
			}
		});
		for (File file : dirfiles) {
			String newPackage=StringUtils.isEmpty(packageName)?file.getName():StringUtils.concat(packageName, "." ,file.getName());
			doScanPackageClassesByFile(classes, StringUtils.concat(newPackage), file);
		}
	}



	public static String[] listClassNameInPackage(File root, String[] pkgNames, boolean recursion, boolean includeInner,boolean searchJar) {
		ClassScanner cs=new ClassScanner();
		cs.setRecursive(recursion);
		cs.setExcludeInnerClass(!includeInner);
		cs.setSearchJar(searchJar);
		try {
			cs.setRootClasspath(root.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
		Set<String> result=cs.scan(pkgNames);
		return result.toArray(new String[result.size()]);
	}
	
	/**
	 * 用相同类的一个已经加载的类来设置要搜索的classpath
	 * @param rootCls
	 */
	public void setRootBySameUrlClass(Class<?> rootCls){
		if(rootCls==null)return;
		this.rootClasspath=ClassLoaderUtil.getCodeSource(rootCls);
		if(rootClasspath==null)rootClasspath = rootCls.getResource("/");
	}
	
	/**
	 * 通过文件系统枚举指定包下面的所有类。
	 * 
	 * @Title: listClassNameInPackage
	 * @param rootCls
	 *            根类，搜索将只针对指定类所在的Classpath进行，不会从全JVM的Classpath搜索
	 * @param pkgNames
	 *            搜索的包名
	 * @param recursion
	 *            是否递归获取子包下的类
	 * @param includeInner
	 *            是否包含内部类
	 * @param searchJar
	 *  搜索jar
	 */
	public static String[] listClassNameInPackage(Class<?> rootCls, String[] pkgNames, boolean recursion, boolean includeInner,boolean searchJar) {
		ClassScanner cs=new ClassScanner();
		cs.setRecursive(recursion);
		cs.setExcludeInnerClass(!includeInner);
		cs.setSearchJar(searchJar);
		cs.setRootBySameUrlClass(rootCls);
		Set<String> result=cs.scan(pkgNames);
		return result.toArray(new String[result.size()]);
	}
}
