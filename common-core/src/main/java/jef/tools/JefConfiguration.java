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
package jef.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import jef.common.Configuration.ConfigItem;
import jef.common.log.LogUtil;
import jef.jre5support.Properties;
import jef.tools.resource.Resource;

import org.slf4j.LoggerFactory;



public class JefConfiguration {
	private static String fileName = "jef.properties";
	private static Map<String, String> cache = new HashMap<String, String>();
	private static File file;
	static org.slf4j.Logger log=LoggerFactory.getLogger(JefConfiguration.class);
	
	
	public static String get(ConfigItem itemkey) {
		return get(itemkey, "");
	}
	

	public static long getLong(ConfigItem itemkey, long defaultValue) {
		String s = get(itemkey);
		if(s==null || s.length()==0)return defaultValue;
		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			LogUtil.warn("the jef config ["+itemkey.name()+"] has invalid value:"+ s);
			return defaultValue;
		}
	}
	

	public static int getInt(ConfigItem itemkey, int defaultValue) {
		String s = get(itemkey);
		if(s==null || s.length()==0)return defaultValue;
		try {
			int n = Integer.parseInt(s);
			return n;
		} catch (Exception e) {
			LogUtil.warn("the jef config ["+itemkey.name()+"] has invalid value:"+ s);
			return defaultValue;
		}
	}

	public static double getDouble(ConfigItem itemkey, double defaultValue) {
		String s = get(itemkey);
		if(s==null || s.length()==0)return defaultValue;
		try {
			double n = Double.parseDouble(s);
			return n;
		} catch (Exception e) {
			LogUtil.warn("the jef config ["+itemkey.name()+"] has invalid value:"+ s);
			return defaultValue;
		}
	}

	public static String get(ConfigItem itemKey, String defaultValue) {
		String key = StringUtils.replaceChars(itemKey.toString(), "_$", ".-").toLowerCase();
		String value=System.getProperty(key);
		if(value!=null)return value;
		try {
			if("schema.mapping".equals(key)){
				value=System.getenv(key);
				if(value!=null)return value;	
			}
			if (cache.containsKey(key)) {
				value=cache.get(key);
			}else{
				if(file==null){
					getFile();
				}
				if(file!=DUMMY_FILE){
					Map<String,String> map=IOUtils.loadProperties(IOUtils.getReader(file, "UTF-8"));
					cache.putAll(map);
					value = cache.get(key);	
				}
			}
			return value == null?defaultValue:value;
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		return defaultValue;
	}


	private static final File DUMMY_FILE=new File("");
	
	private synchronized static void getFile() {
		try{
			if (file != null && file.exists())
				return;
			String filename=System.getProperty("jef.properties");
			if(StringUtils.isEmpty(filename))filename=fileName;
			List<URL> urls = ResourceUtils.getResources(filename);
			if(urls.size()>1){
				log.warn("Found "+ urls.size()+" 'jef.properties' files...");
				LogUtil.warn(urls);
			}
			if(urls.isEmpty()){
				file=DUMMY_FILE;
			}else{
				file=Resource.getFileResource(urls.get(0)).getFile();
			}
		}catch(Exception e){
			LogUtil.exception(e);
			file=DUMMY_FILE;
		}
		log.info("JEF is using config file:" + (DUMMY_FILE==file?"Default":file.getAbsolutePath()));
		int timeZone=TimeZone.getDefault().getRawOffset()/3600000;
		if(getBoolean(Item.DB_DEBUG, true))
			log.info("Current Locale:" + Locale.getDefault().toString()+"\tTimeZone:"+ (timeZone<0?String.valueOf(timeZone):"+"+String.valueOf(timeZone))+"\tEncoding:" + Charset.defaultCharset());
	}

	public static boolean getBoolean(ConfigItem itemkey, boolean defaultValue) {
		String s = get(itemkey);
		return StringUtils.toBoolean(s, defaultValue);
	}

	public static boolean update(Item itemkey, String value) {
		if(file==DUMMY_FILE)return false;
		try {
			String key = itemkey.toString().replaceAll("_", ".").toLowerCase();
			Properties properties = new Properties();
			properties.load(IOUtils.getReader(file, "UTF-8"));
			properties.setProperty(key, value);
			OutputStream out=null;
			try{
				out = new FileOutputStream(file.getPath());
				properties.store(out, null);	
			}finally{
				IOUtils.closeQuietly(out);
			}
			return true;
		} catch (IOException e) {
			LogUtil.exception(e);
			return false;
		}
	}

	public enum Item implements ConfigItem {
		//////////////数据库基本操作设置/////////////////
		/**
		 * SQL调试开关，默认false，开启后输出各种日志
		 */
		DB_DEBUG,
		
		///////////////////其他HttpClient选项//////////////////
		/**
		 * 启用HTTP客户端调试
		 */
		HTTP_DEBUG, 			
		/**
		 * 全局禁用代理服务器
		 */
		HTTP_DISABLE_PROXY,
		/**
		 * HTTP选项，默认下载路径
		 */
		HTTP_DOWNLOAD_PATH, 	
		/**
		 * HTTP选项，全局超时
		 */
		HTTP_TIMEOUT,
		/**
		 * HTTP选项 超时、重试次数
		 */
		HTTP_RETRY,
		
		/////////////////其他选项//////////////////
		/**
		 * 使用标准日志输出，默认直接print到控制台。设置为true时，日志写入到slf4j，false时则直接输出到标准控制台
		 */
		COMMON_DEBUG_ADAPTER,
		/**
		 * 当启用了COMMON_DEBUG_ADAPTER后，再开启本选项，可以将System.out和System.err流也重定向
		 */
		SYSOUT_REDIRECT,			// 
		//////////////////其他不常用属性///////////////////
		/**
		 * 当使用ServletExchange返回json时，带上json头
		 */
		HTTP_SEND_JSON_HEADER,
		/**
		 * 在控制台输出列的时候显示列的数值类型
		 */
		CONSOLE_SHOW_COLUMN_TYPE,	//
		/**
		 * SQLplus显示选项
		 */
		CONSOLE_SHOW_RESULT_LIMIT, 	//
		/**
		 * 默认日志路径
		 * @deprecated 旧功能，不建议使用
		 */
		LOG_PATH
	}
}
