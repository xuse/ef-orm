package jef.tools.resource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于Classpath的资源加载器。
 * 特点：在classpath的所有路径上搜索资源
 * 
 * 特殊功能：
 * <li>跳过jar包</li>
 * 当{setDirectoryOnly(true)}后，只在目录中搜索。
 * 通过
 * <li>可以指定用哪个classloader搜索</li>
 * 
 * @author jiyi
 *
 */
public class ClasspathLoader extends AResourceLoader{
	private boolean directoryOnly;
	
	/**
	 * 使用的classLoader
	 */
	private List<ClassLoader> loaders;
	
	public ClasspathLoader(){
		this(false);
	}
	
	public ClasspathLoader(boolean directoryOnly) {
		this.directoryOnly=directoryOnly;
		setDefaultClassLoader();
	}
	
	public ClasspathLoader(boolean directoryOnly,ClassLoader... loaders) {
		this.directoryOnly=directoryOnly;
		if(loaders.length>0){
			this.loaders=Arrays.asList(loaders);
		}else{
			setDefaultClassLoader();	
		}
	}
	
	public URL getResource(String name) {
		if(name.startsWith("/"))name=name.substring(1);
		if(directoryOnly)name="./"+name;
		if(loaders!=null && loaders.size()>0){
			for(ClassLoader loader:loaders){
				URL res=loader.getResource(name);
				if(res!=null)return res;
			}
		}
		return null;
	}

	public List<URL> getResources(String name) {
		if(name.startsWith("/"))name=name.substring(1);
		if(directoryOnly)name="./"+name;
		Set<URL> result=new LinkedHashSet<URL>();
		try{
			if(loaders!=null && loaders.size()>0){
				for(ClassLoader loader:loaders){
					for(Enumeration<URL> e=loader.getResources(name);e.hasMoreElements();){
						result.add(e.nextElement());	
					}	
				}
			}	
		}catch(IOException e){
			throw new IllegalArgumentException(e.getMessage());
		}
		return new ArrayList<URL>(result);
	}
	
	

	/**
	 * 指定使用系统classloader查找
	 */
	public void setSystemClassLoader(){
		this.loaders=Arrays.asList(ClassLoader.getSystemClassLoader());
	}
	/**
	 * 指定查找资源的classloader
	 * @param loader
	 */
	public void setClassLoaders(ClassLoader... loader){
		this.loaders=Arrays.asList(loader);
	}
	/**
	 * 指定查找资源的classpath使用默认的
	 */
	public void setDefaultClassLoader(){
		ClassLoader cl=Thread.currentThread().getContextClassLoader();
		if(cl==null){
			cl=ClasspathLoader.class.getClassLoader();
		}
		this.loaders=Arrays.asList(cl);
	}

	public boolean isDirectoryOnly() {
		return directoryOnly;
	}

	public void setDirectoryOnly(boolean directoryOnly) {
		this.directoryOnly = directoryOnly;
	}
}
