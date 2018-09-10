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
package jef.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.ResourceUtils;
import jef.tools.io.Charsets;
import jef.tools.resource.FileResource;
import jef.tools.resource.Resource;

/**
 * 通用配置管理器
 * @author Administrator
 *
 */
public class Configuration extends Cfg {
	private static final int MAX_ENTRIES=100;
	private static HashMap<String,Configuration> configurationPool = new HashMap<String,Configuration> ();
	
	private Resource pFile;
	private Map<String,String> cache;

	public Configuration(String filePath,ClassLoader... loader) {
		cache = new LinkedHashMap<String,String>(){
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Entry<String, String> eldest) {
				return size()>MAX_ENTRIES;
			}
		};
		pFile = getFile(filePath,loader);
		pFile.setCharset(Charsets.UTF8);
		Assert.notNull(pFile);
	}

	public Configuration(File file) {
		if(file.exists()){
			if(file.isDirectory())throw new RuntimeException(file.getPath()+" is not properties file but a folder!");
		}
		cache = new HashMap<String,String>();
		pFile = new FileResource(file);
		pFile.setCharset(Charsets.UTF8);
	}

	protected void setInCache(String key,String value){
		cache.put(key, value);
	}
	
	/**
	 * 构造
	 */
	public Configuration() {
		cache = new HashMap<String,String>();
		if(getFileName()!=null){
			this.setFileName(getFileName());
		}
	}
	
	/**
	 * 需要被继承，用于初始化文件名
	 * @return
	 */
	protected String getFileName(){
		return null;
	}

	/**
	 * 设置文件
	 * @param filepath
	 */
	public void setFileName(String filepath) {
		pFile = getFile(filepath);
		if(pFile==null || !pFile.isReadable()){
			LogUtil.error("The configuration file:"+filepath+" is not found.");
			new Throwable().printStackTrace();
		}
	}

	/**
	 * 列出所有选项
	 * @return
	 */
	public Map<String,String> listProperties() {
		try {
			return pFile.loadAsProperties();
		} catch (IOException e) {
			LogUtil.exception(e);
			return Collections.emptyMap();
		}
	}

	/**
	 * 返回实例
	 * @param path
	 * @return
	 */
	public static Configuration getInstance(String path) {
		if (!configurationPool.containsKey(path)) {
			Configuration c = new Configuration(path);
			configurationPool.put(path, c);
		}
		return (Configuration) configurationPool.get(path);
	}

	//从文件读取
	private String loadFromFile(String key,String defaultValue) throws IOException {
		if(pFile==null && !pFile.isReadable()){
			return defaultValue;
		}
		Map<String,String> properties=IOUtils.loadProperties(pFile.openReader());
		String value = properties.get(key);
		if (value == null)
			value = defaultValue;
		return value;
	}

	
	/**
	 * 获取文件
	 * @param fileName
	 * @param loader
	 * @return
	 */
	public static Resource getFile(String fileName, ClassLoader... loaders) {
		URL url=ResourceUtils.getResource(fileName, false, loaders);
		LogUtil.debug("Locate Resource "+url);
		if(url==null){
			return Resource.DUMMY;
		}
		return Resource.getResource(url);
	}

	/**
	 * 更新配置信息
	 * @param itemkey
	 * @param value
	 */
	public synchronized void update(ConfigItem itemkey, String value) {
		if(!pFile.isWritable()){
			LogUtil.warn("Attempt to update a readonly properties: "+pFile.toString());
			return;
		}
		try {
			Map<String,String> properties = IOUtils.loadProperties(pFile.openReader());
			String key = itemkey.toString().replaceAll("_", ".").toLowerCase();
			properties.put(key, value);
			IOUtils.storeProperties(pFile.getWriter(), properties, true);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
	}
	
	/**
	 * 更新配置信息
	 * @param entries
	 */
	public synchronized void update(Map<? extends ConfigItem,String> entries) {
		if(!pFile.isWritable()){
			LogUtil.warn("Attempt to update a readonly properties: "+pFile.toString());
			return;
		}
		try {
			Map<String,String> properties = IOUtils.loadProperties(pFile.openReader());
			
			for(ConfigItem itemKey: entries.keySet()){
				String key = itemKey.toString().replaceAll("_", ".").toLowerCase();
				properties.put(key, entries.get(itemKey));	
			}
			IOUtils.storeProperties(pFile.getWriter(), properties, true);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
	}

	public static interface ConfigItem {
		String name();
	}
	/**
	 * 得到String
	 * @param itemKey
	 * @param defaultValue
	 * @return
	 */
	protected String get(String key, String defaultValue) {
		try {
			String value=System.getProperty(key);
			if(value!=null)return value;
			if (!cache.containsKey(key)) {
				value=loadFromFile(key,defaultValue);
				cache.put(key, value);//取过一次就存下来
			}
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		return (String) cache.get(key);
	}
}
