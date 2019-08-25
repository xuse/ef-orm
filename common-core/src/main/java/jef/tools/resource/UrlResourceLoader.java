package jef.tools.resource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import jef.tools.StringUtils;
import jef.tools.reflect.ClassLoaderUtil;

public class UrlResourceLoader extends AResourceLoader {
	/**
	 * 偏好URL
	 */
	// private URL[] perferUrl;
	private java.net.URLClassLoader ul;
	/**
	 * 只在指定目录上搜索
	 */
	private boolean directoryOnly = false;

	public UrlResourceLoader() {

	}

	public static enum EnvURL {
		PATH, JAVA_LIBRARY_PATH, JAVA_HOME, USER_DIR, TEMP_DIR, USER_HOME, JAVA_CLASS_PATH, SYSTEM_TMP, WINDIR
	}

	public UrlResourceLoader(EnvURL... envs) {
		Set<URL> u = new java.util.LinkedHashSet<URL>();
		String pathSp = System.getProperty("path.separator");
		for (EnvURL type : envs) {
			switch (type) {
			case JAVA_CLASS_PATH:
				for (String s : StringUtils.split(System.getProperty("java.class.path"), pathSp)) {
					add(u,s);
				}
				break;
			case JAVA_HOME: {
				add(u,System.getProperty("java.home"));
				break;
			}
			case JAVA_LIBRARY_PATH:
				for (String s : StringUtils.split(System.getProperty("java.library.path"), pathSp)) {
					add(u,s);
				}
				break;
			case PATH:
				for (String s : StringUtils.split(System.getenv("PATH"), pathSp)) {
					add(u,s);
				}
				break;
			case SYSTEM_TMP: 
				add(u,System.getenv("TEMP"));
				break;
			case TEMP_DIR:
				add(u,System.getProperty("java.io.tmpdir"));
				break;
			case USER_DIR:
				add(u,System.getProperty("user.dir"));
				break;
			case USER_HOME:
				add(u,System.getProperty("user.home"));
				break;
			case WINDIR: 
				add(u,System.getenv("windir"));
				break;
			}
		}
		setPerferUrl(u.toArray(new URL[u.size()]));
	}

	private final void add(Set<URL> u, String s) {
		if(s==null || s.length()==0)return;
		File f = new File(s);
		try{
			u.add(f.toURI().toURL());			
		} catch (MalformedURLException e) {
		}
	}

	private void setPerferUrl(URL[] array) {
		this.ul = new URLClassLoader(array);
	}

	public UrlResourceLoader(Class<?> c) {
		this(c, false);
	}

	public UrlResourceLoader(Class<?> c, boolean dirOnly) {
		setSearchURLByClass(c);
		this.directoryOnly = dirOnly;
	}

	public boolean isDirectoryOnly() {
		return directoryOnly;
	}

	public void setDirectoryOnly(boolean directoryOnly) {
		this.directoryOnly = directoryOnly;
	}

	public void setSearchURL(URL... urls) {
		setPerferUrl(urls);
	}

	public void setSearchURLByClass(Class<?> clz) {
		URL u = ClassLoaderUtil.getCodeSource(clz);
		if (u != null) {
			setPerferUrl(new URL[] { u });
		}
	}

	public URL getResource(String name) {
		if (name.startsWith("//")){
			File file=new File(name.substring(1));
			if(file.exists())return super.toURL(file);
			name = name.substring(2);
		}else if(name.startsWith("/")){
			name = name.substring(1);
		}
		if (directoryOnly)
			name = "./" + name;
		if (ul != null) {
			return ul.findResource(name);
		}
		return null;
	}

	public List<URL> getResources(String name) {
		if (name.startsWith("//")){
			File file=new File(name.substring(1));
			if(file.exists())return Arrays.asList(super.toURL(file));
			name = name.substring(2);
		}else if(name.startsWith("/")){
			name = name.substring(1);
		}
		if (directoryOnly)
			name = "./" + name;
		List<URL> result = new ArrayList<URL>();
		try {
			if (ul != null) {
				for (Enumeration<URL> e = ul.findResources(name); e.hasMoreElements();) {
					result.add(e.nextElement());
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage());
		}
		return result;
	}
}
