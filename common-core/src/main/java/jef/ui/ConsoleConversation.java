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
package jef.ui;

import jef.common.Callback;
import jef.common.log.LogUtil;
import jef.tools.StringUtils;



public abstract class ConsoleConversation<T> extends Thread{
	private ConsoleShell app;
	private Callback<T,? extends Throwable> callback;
	
	/**
	 * 得到下一个键盘输入项
	 * @param msg
	 * @return
	 */
	protected String getInput(String msg,String... valids){
		return app.getInput(this,msg,valids);
	}
	
	protected String getInputWithDefaultValue(String msg,String defaultValue){
		String s=app.getInput(this,msg+"(default:"+defaultValue+")");
		if(StringUtils.isEmpty(s))return defaultValue;
		return s;
	}
	
	protected long getInputLong(String msg) {
		String str;
		do{
			str=getInput(msg).trim();	
		}while(!StringUtils.isNumericOrMinus(str));
		return StringUtils.toLong(str,0L);
	}
	/**
	 * 要求输入并返回一个数字
	 * @param msg
	 * @return
	 */
	protected int getInputInt(String msg,int... allows) {
		String str;
		String[] all=new String[allows.length];
		for(int i=0;i<allows.length;i++)all[i]=String.valueOf(allows[i]);
		do{
			str=getInput(msg,all).trim();	
		}while(!StringUtils.isNumericOrMinus(str));
		return StringUtils.toInt(str, 0);
	}
	
	/**
	 * 要求输入并返回一个布尔
	 * @param msg
	 * @return
	 */
	protected boolean getInputBoolean(String msg,boolean defaultValue) {
		String str;
		Boolean result=null;
		do{
			str=getInput(msg +"(Default:"+String.valueOf(defaultValue)+")").trim();
			if(StringUtils.isEmpty(str)){
				result=defaultValue;
			}else{
				try{
					result=StringUtils.toBoolean(str, null);		
				}catch(IllegalArgumentException e){
					prompt("not a boolean value.");
				}
			}
		}while(result==null);
		return result.booleanValue();
	}
	
	/**
	 * 返回Conolse对象
	 * @return
	 */
	protected ConsoleShell getApplication(){
		return app;
	}
	//构造
	public ConsoleConversation(ConsoleShell app){
		this.app=app;
	}
	
	public final synchronized void run() {
		T result=executeCall();
		if(callback!=null){
			try {
				callback.call(result);
			} catch (Throwable e) {
				LogUtil.exception(e);
			}
		}
	}
	
	/**
	 * 可继承
	 * 执行会话，返回结果 
	 * @throws
	 */
	protected T executeCall(){
		execute();
		return null;
	}
	/**
	 * 可继承，执行会话，无需返回结果 
	 */
	
	protected void execute(){
	}
	
	protected final void prompt(String msg){
		LogUtil.show(msg);
	}

	public Callback<T, ? extends Throwable> getCallback() {
		return callback;
	}

	public void setCallback(Callback<T, ? extends Throwable> callback) {
		this.callback = callback;
	}
}

