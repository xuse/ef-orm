package jef.tools.resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 一个ResourceLoader一般代表一种加载方式（策略），这个类用来组合多种加载方式（策略），
 * 用来实现一些较为复杂的加载策略。
 * 
 * 但是有一个案例比较麻烦，比如首先加载class文件同包下的资源，没有的情况下再加载其他classpath下的同包资源，这种实现比较难处理。
 * 
 * @author jiyi
 *
 */
public class CompsiteLoader extends AResourceLoader implements Cloneable{
	private List<ResourceLoader> loaders=new ArrayList<ResourceLoader>();
	private boolean firstCollection=true;
	
	public CompsiteLoader(){
	}
	
	@Override
	public CompsiteLoader clone(){
		CompsiteLoader c=new CompsiteLoader();
		c.loaders.addAll(this.loaders);
		return c;
	}

	public CompsiteLoader(ResourceLoader... loaders){
		this.loaders.addAll(Arrays.asList(loaders));
	}
	
	public CompsiteLoader(boolean first,ResourceLoader... loaders){
		this.firstCollection=first;
		this.loaders.addAll(Arrays.asList(loaders));
	}
	
	public void addResourceLoader(ResourceLoader loader){
		loaders.add(loader);
	}
	
	public boolean removeResourceLoader(ResourceLoader loader){
		return loaders.remove(loader);
	}
	
	public int loaderSize(){
		return loaders.size();
	}
	
	public boolean isFirstCollection() {
		return firstCollection;
	}

	public CompsiteLoader setFirstCollection(boolean firstCollection) {
		this.firstCollection = firstCollection;
		return this;
	}

	public URL getResource(String name) {
		URL u=null;
		for(ResourceLoader rl:loaders){
			u=rl.getResource(name);
			if(u!=null)return u;
		}
		return null;
	}

	public List<URL> getResources(String name) {
		Set<URL> urls=new HashSet<URL>();
		for(ResourceLoader rl:loaders){
			urls.addAll(rl.getResources(name));
			if(firstCollection && urls.size()>0)break;
		}
		return new ArrayList<URL>(urls);
	}


}
