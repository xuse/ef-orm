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
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jef.tools.IOUtils;
import jef.tools.StringUtils;

/**
 * 用于存放固定大小缓存的Map，线程安全（仅限get,set两个方法）
 * @author Administrator
 * @param <K>
 * @param <V>
 */
public class StringCacheMap extends LinkedHashMap<String, String> {
	private static final long serialVersionUID = 2428383992533927687L;
	private static final float DEFAULT_LOAD_FACTOR = 1f;
	
	private final int maxCapacity;
	private final Lock lock = new ReentrantLock();
	private final File folder;
	
	
	/**
	 * 构造一个高级缓存Map
	 * @param size
	 * @param name
	 */
	public StringCacheMap(int size,String name) {
		super(size, DEFAULT_LOAD_FACTOR, true);
		if(name==null)throw new NullPointerException("The input name must not null!");
		this.maxCapacity = size;
		this.folder=IOUtils.createTempDirectory(name);
	}
	
	protected final boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest) {
		boolean flag=size() > maxCapacity;
		if(flag){
			saveEntry(eldest);
		}
		return flag;
	}

	
	@Override
	protected void finalize() throws Throwable {
		close();
	}

	private void close() {
		if(folder!=null){
			IOUtils.deleteAllChildren(folder);
		}
		this.clear();
	}

	private void saveEntry(java.util.Map.Entry<String, String> eldest) {
		String fileName=StringUtils.toFilename(eldest.getKey(), "_");
		fileName=fileName.replace(' ', '.');
		try{
			File file=new File(folder,fileName);
			if(file.exists())return;
			IOUtils.saveAsFile(file, Charset.defaultCharset(),eldest.getValue());
		}catch(IOException e){
			throw new IllegalStateException(e);
		}
	}
	
	private String loadEntry(Object keyObj) {
		String key=String.valueOf(keyObj);
		String fileName=StringUtils.toFilename(key, "_");
		fileName=fileName.replace(' ', '.');
		File file=new File(folder,fileName);
		if(file.exists()){
			try {
				return IOUtils.asString(file, Charset.defaultCharset().name());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

	public String get(Object key) {
		try {
			lock.lock();
			String v= super.get(key);
			if(v==null){
				v=loadEntry(key);
				if(v!=null){
					put(String.valueOf(key),v);
				}
			}
			return v;
		} finally {
			lock.unlock();
		}
	}
	
	public String put(String key, String value) {
		try {
			lock.lock();
			if(value==null){//不允许插入null值
				throw new NullPointerException("The value of "+ String.valueOf(key)+" can not be null!");
			}
			return super.put(key, value);
		} finally {
			lock.unlock();
		}
	}
}
