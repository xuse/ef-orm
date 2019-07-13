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
package jef.common.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import jef.common.MessageCollector;
import jef.tools.DateFormats;
import jef.tools.DateUtils;
import jef.tools.Exceptions;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.JefConfiguration.Item;
import jef.tools.StringUtils;
import jef.tools.io.Charsets;

public class FileLogger extends AbstractLogger {
	private static final long serialVersionUID = 1L;

	//缺省的日志
	private static FileLogger DEFAULT_LOG;
	
	//切换大小
	public static final int WARNNING_FILE_LENGTH=1048576*2;
	private boolean logDate=true;
	
	//级别

	private boolean autoRolling;
	
	private static final String DEFAULT_BASE="jef-log";
	private String rollingBase=DEFAULT_BASE;
	private static final String extName=".log";
	
	
	private File file;
	private BufferedWriter out;
	
	private void ensureOpen() throws IOException{
		if(out==null){
			if(file==null)file=this.changeFile();
			out=IOUtils.getWriter(file, Charsets.UTF8, true);
		}
	}
	

	/**
	 * log中自动包含日期
	 * @return
	 */
	public boolean isLogDate() {
		return logDate;
	}
	public void setLogDate(boolean logDate) {
		this.logDate = logDate;
	}
	/**
	 * 获得当前的日志文件
	 * @return
	 */
	public File getFile() {
		return file;
	}
	
	//记录对象
	public void log(Object... msgs){
		if(msgs==null)return;
		try{
			ensureOpen();
			for(Object msg:msgs){
				out.write(toLogMessage(msg));
				out.write(StringUtils.CRLF_STR);
			}
			out.flush();
			if(file.length()>WARNNING_FILE_LENGTH && autoRolling){
				out.close();
				out=null;
				this.file=changeFile();
			}
		}catch(Exception e){
			Exceptions.log(e);
		}
		
		
	}
	//切换文件
	private File changeFile() {
		String sufix=DateFormats.DATE_TIME_SHORT_12.format(new Date());
		if(file==null){
			String log=JefConfiguration.get(Item.LOG_PATH);
			return new File(log,this.rollingBase+sufix+extName);
		}else{
			return new File(file.getParent(),this.rollingBase+sufix+extName);
		}
	}

	/**
	 * 转换为日志格式文本
	 * @param msgObj
	 * @return
	 */
	private String toLogMessage(Object msgObj) {
		String msg=StringUtils.toString(msgObj);
		if(logDate){
			return String.format("%s %s",DateUtils.formatDateTime(new Date()),msg); 
		}else{
			return msg;
		}
	}


	public static final FileLogger getInstance(){
		if(DEFAULT_LOG==null)DEFAULT_LOG=new FileLogger();
		return DEFAULT_LOG;
	}
	public FileLogger(){}
	
	public FileLogger(File file){
		this.file=file;
	}
	
	private void log(Iterable<String> strings,int newLine){
		try{
			if(file==null)file=new File("jef-log.txt");
			if(!file.getParentFile().exists())file.getParentFile().mkdirs();
			if(!file.exists())file.createNewFile();
			FileOutputStream out=new FileOutputStream(file,true);
			for(String msg:strings){
				if(logDate){
					msg=DateUtils.formatDateTime(new Date())+" "+msg;
					out.write(msg.getBytes());
				}else{
					out.write(msg.getBytes());
				}
				out.write(StringUtils.CRLF);
			}
			for(int i=0;i<newLine;i++){
				out.write(StringUtils.CRLF);
			}
			out.close();
		}catch(Exception e){
			Exceptions.log(e);
		}
	}
	
	/**
	 * 记录多条文本
	 * @param strings
	 */
	public void log(Iterable<String> strings) {
		log(strings,0);
	}
	/**
	 * 记录消息
	 * @param mc
	 */
	public void log(MessageCollector mc) {
		log(mc,1);
	}
	/**
	 * 在日志中插入换行
	 * @param n
	 */
	public void logNewLine(int n) {
		log(new ArrayList<String>(),n);		
	}



}
