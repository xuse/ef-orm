/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools.reflect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.ResourceUtils;

/**
 * 用于动态 编译、加载类的工具
 * 
 * ClassLoader: 记录了各个路径，并且集成加载类文件(.class)，以及相关资源等行为特征的一个类，主要属性有 URL列表：
 * 这个列表记录了到什么地方去找文件，会按顺序遍历寻找。这个路径必须是目录或JAR文件，不能是其他的 parent：
 * 记录了上一层的ClassLoader,(JVM要求将所有的Classloader组织成一棵树)，如果本处找不到类，会到上一Loader里去查找。
 * 
 * 基础常识： JVM启动后，默认有三个持久化的Classloader（以后简称系统Loader）: <LI>BootstrapLoader
 * JVM核心，不允许访问。</LI> <LI>ExtClassloader:JVM下ext目录下的包和类。</LI> <LI>
 * SystemClassLoader: 又名AppClassLoader,记录当前应用程序的ClassPath</LI> 这三个的树型关系是
 * BootstrapLoader 最顶层， ExtClassloader在中间， SystemClassLoader在最下这样的结构。
 * 当我们使用Class.forName()时，就会到AppLoder去找class，实际效果是在这三个ClassLoader里面去寻找(当类载入前)。
 * 
 * 功能说明： 初始化后，即初始化了两个基本的ClassLoader,一个是系统ClassLoader，一个是扩展ClassCloader。
 * 我们要载入一个动态类，有两种方法，一种是将该类所在路径加入到系统Loader当中去。 二是自己定义一个CLassLoader，用完后GC会回收这个资源。
 * 
 * @author Jiyi
 */
public class ClassLoaderUtil {

	/**
	 * URLClassLoader本来是支持动态增加url(addUrl)，并且可以访问其classes字段的，但是这两个都是受保护的(
	 * protected)， 因此初始化要先破除这两个东西的保护。
	 */
	private static Field classes;
	private static Method addURL;
	static {
		try {
			addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			addURL.setAccessible(true);
		} catch (Exception e) {
			LogUtil.show(e.getMessage());
		}
		try {
			classes = ClassLoader.class.getDeclaredField("classes");
			classes.setAccessible(true);
		} catch (Exception e) {
			LogUtil.show(e.getMessage());
		}
	}

	/**
	 * 得到当前线程的ContextClassLoader
	 * 
	 * @return
	 */
	public ClassLoader getContextLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * 得到系统的ClassLoader
	 * 
	 * @return
	 */
	public static URLClassLoader getAppClassLoader() {
		return (URLClassLoader) ClassLoader.getSystemClassLoader();
	}

	/**
	 * 得到虚拟机扩展的ClassLoader
	 */
	public static URLClassLoader getJvmExtClassLoader() {
		return (URLClassLoader) getAppClassLoader().getParent();
	}

	/**
	 * 得到由指定的ClassLoader所载入的所有类
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Class<?>> getClassesLoadedBy(ClassLoader cl) {
		if (classes == null)
			return Collections.EMPTY_LIST;
		try {
			List<Class<?>> list = (List<Class<?>>) classes.get(cl);
			return list;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 构造一个URLClassLoader
	 * 
	 * @param folder
	 * @return
	 */
	public static URLClassLoader createURLClassLoader(File dirOrJar) {
		Assert.isTrue(dirOrJar.exists(), "the input file does not exist:" + dirOrJar.getPath());
		if (dirOrJar.isFile()) {
			String ext = IOUtils.getExtName(dirOrJar.getName());
			if (!ext.equals("jar") && !ext.equals("zip")) {
				dirOrJar = dirOrJar.getParentFile();
			}
		}
		return createURLClassLoader(ResourceUtils.fileToURL(dirOrJar));
	}

	/**
	 * 构造一个URLClassLoader
	 * 
	 * @param url
	 * @return
	 */
	public static URLClassLoader createURLClassLoader(URL url) {
		URLClassLoader newloader = new URLClassLoader(new URL[] { url }, getAppClassLoader());
		return newloader;
	}

	// /**
	// * 得到JRE基础的classpath (Bootstrap 的URL)
	// *
	// * @return
	// */
	// @SuppressWarnings("restriction")
	// public static URL[] getBootstrapClassPath() {
	// return Launcher.getBootstrapClassPath().getURLs();
	// }

	/**
	 * 得到本工程的classpath
	 * 
	 * @return
	 */
	public static URL[] getSystemClassPath() {
		return getAppClassLoader().getURLs();
	}

	public static File[] getClasspath(ClassLoader cl) {
		String classLoaderName = cl.getClass().getSimpleName();
		File[] fs = new File[0];
		if (classLoaderName.equals("DefaultClassLoader")) {// RCP中的ClassLoader
			ClassLoader loader = JefConfiguration.class.getClassLoader();
			fs = ClassLoaderUtil.getClasspathFromDefaultClassLoader(loader);
		} else if (cl instanceof URLClassLoader) {
			fs = ClassLoaderUtil.getClasspath((URLClassLoader) cl);
		} else {
			List<File> result = new ArrayList<File>();
			try {
				for (URL u : ArrayUtils.toIterable(cl.getResources("."))) {
					result.add(IOUtils.urlToFile(u));
				}
			} catch (IOException e) {
				LogUtil.exception(e);
			}
			fs = result.toArray(new File[result.size()]);
		}
		return fs;
	}

	/**
	 * 将一个目录或JAR包添加到classpath
	 * 
	 * @param path
	 */
	public static boolean addClassPath(String path) {
		return addClassPath(new File(path));
	}

	/**
	 * 将一个目录或JAR包添加到classpath
	 * 
	 * @param dirOrJar
	 */
	public static boolean addClassPath(File dirOrJar) {
		return addClassPath(dirOrJar, getAppClassLoader());
	}

	/**
	 * 添加路径
	 * 
	 * @param dirOrJar
	 * @param loader
	 * @return
	 */
	public static void addUrl(URLClassLoader loader, URL... urls) {
		for (URL u : urls) {
			if (u != null) {
				try {
					addURL.invoke(loader, u);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e.getTargetException());
				}
			}
		}

	}

	/**
	 * 在URLClassLoader动态地增加URL
	 * 
	 * @param dirOrJar
	 * @param loader
	 * @return
	 */
	public static boolean addClassPath(File dirOrJar, URLClassLoader loader) {
		try {
			URL url = ResourceUtils.fileToURL(dirOrJar);
			addURL.invoke(loader, new Object[] { url });
			return true;
		} catch (Exception e) {
			LogUtil.exception(e);
			return false;
		}
	}

	/**
	 * 从一个类得到这个类的class文件路径
	 * 
	 * @param c
	 * @return
	 */
	public static String getClassFilePath(Class<?> c) {
		URL u = getCodeSource(c);
		if (u == null)
			return null;
		String path = u.getPath() + c.getName().replace('.', '/') + ".class";
		return path;
	}

	/**
	 * 得到一个类被加载时的路径 一个ClassLoader可以包含多个URL，加载时类可以从其中任意一个位置被读入
	 * 
	 * @param c
	 * @return 可能返回null;
	 */
	public static URL getCodeSource(Class<?> c) {
		ProtectionDomain pd = c.getProtectionDomain();
		CodeSource cs = pd.getCodeSource();
		if (cs != null)
			return cs.getLocation();
		if (!(c.getClassLoader() instanceof URLClassLoader))
			return null;
		URLClassLoader ul = (URLClassLoader) c.getClassLoader();
		for (URL url : ul.getURLs()) {
			File path = new File(url.getPath());
			File classFile = new File(path, c.getName().replace('.', '/') + ".class");
			if (classFile.exists()) {
				return url;
			}
		}
		return null;
	}

	public static void displayClassInfo(Class<?> clazz) {
		StringBuffer results = new StringBuffer();
		displayClassInfo(clazz, results, true);
		System.out.println(results.toString());
	}

	public static void displayClassInfo(Class<?> clazz, StringBuffer results, boolean showParentClassLoaders) {
		ClassLoader cl = clazz.getClassLoader();
		results.append("\n" + clazz.getName() + "(" + Integer.toHexString(clazz.hashCode()) + ").ClassLoader=" + cl);
		ClassLoader parent = cl;
		while (parent != null) {
			results.append("\n.." + parent);
			if (parent instanceof URLClassLoader) {
				URL[] urls = ((URLClassLoader) parent).getURLs();
				int length = urls != null ? urls.length : 0;
				for (int u = 0; u < length; u++) {
					results.append("\n...." + urls[u]);
				}
			}
			if (showParentClassLoaders == false)
				break;
			if (parent != null)
				parent = parent.getParent();
		}
		CodeSource clazzCS = clazz.getProtectionDomain().getCodeSource();
		if (clazzCS != null)
			results.append("\n++++CodeSource: " + clazzCS);
		else
			results.append("\n++++Null CodeSource");

		results.append("\nImplemented Interfaces:");
		Class<?>[] ifaces = clazz.getInterfaces();
		for (int i = 0; i < ifaces.length; i++) {
			Class<?> iface = ifaces[i];
			results.append("\n++" + iface + "(" + Integer.toHexString(iface.hashCode()) + ")");
			ClassLoader loader = ifaces[i].getClassLoader();
			results.append("\n++++ClassLoader: " + loader);
			ProtectionDomain pd = ifaces[i].getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			if (cs != null)
				results.append("\n++++CodeSource: " + cs.getLocation());
			else
				results.append("\n++++Null CodeSource");
		}
	}

	/**
	 * 对于RCP程序，其中ClassLoader是DefaultClassLoader，不是systemClassLoader
	 * 此方法用于在RCP中获取路径
	 * 
	 * @param loader
	 * @return
	 */
	public static File[] getClasspathFromDefaultClassLoader(ClassLoader loader) {
		Object manager = BeanUtils.getFieldValue(loader, "manager");
		List<File> fs = new ArrayList<File>();
		for (Object entry : (Object[]) BeanUtils.getFieldValue(manager, "entries")) {
			Object bf = BeanUtils.getFieldValue(entry, "bundlefile");
			File f = (File) BeanUtils.getFieldValue(bf, "basefile");
			fs.add(f);
		}
		return fs.toArray(new File[0]);
	}

	public static File[] getClasspathOfAppClassLoader() {
		return getClasspath(getAppClassLoader());
	}

	/**
	 * 得到指定的ClassLoader的路径
	 * 
	 * @param cl
	 * @return
	 */
	public static File[] getClasspath(URLClassLoader cl) {
		List<File> fs = new ArrayList<File>();
		for (URL url : cl.getURLs()) {
			File dirOrJar = IOUtils.urlToFile(url);
			fs.add(dirOrJar);
		}
		return fs.toArray(new File[0]);
	}
}
