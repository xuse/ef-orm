package jef.tools.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jef.tools.IOUtils;

public abstract class AResourceLoader implements ResourceLoader{
	public InputStream getResourceAsStream(String name){
		URL u= getResource(name);
		try {
			return u==null?null:u.openStream();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	protected File toFile(URL url){
		try {
			return new File(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	protected URL toURL(File file){
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	public BufferedReader      getResourceAsReader(String name,String charset){
		URL u= getResource(name);
		try {
			return u==null?null:IOUtils.getReader(u.openStream(), charset);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public Resource getResourceEx(String name) {
		URL url=getResource(name);
		return url==null?null:Resource.getResource(url);
	}

	public List<Resource> getgetResourcesEx(String name) {
		List<URL> urls= getResources(name);;
		List<Resource> result=new ArrayList<Resource>(urls.size());
		for(URL u:urls){
			result.add(Resource.getResource(u));
		}
		return result;
	}
}
