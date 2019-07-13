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
package jef.ui.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.ReentrantLock;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.Exceptions;
import jef.tools.StringUtils;
import jef.tools.ThreadUtils;
import jef.ui.ConsoleConversation;
import jef.ui.ConsoleShell;

public abstract class AbstractConsoleShell implements ConsoleShell {
	protected jef.tools.support.ThreadLocal<ConsoleShell> sub=new jef.tools.support.ThreadLocal<ConsoleShell>();
	protected ConsoleShell parent;
	private ConsoleWaiter waiter;
	protected boolean keepgoing = true;
	protected String cmd=null;
	protected final ReentrantLock lock=new ReentrantLock();//线程同步锁
	/**
	 * 会话将获取Console的下一个输入。如果一直没输入则将阻塞等待
	 * @param conv
	 * @param msg
	 * @param validValues
	 * @return
	 */
	public String getInput(ConsoleConversation<?> conv,String msg,String... validValues){
		ConsoleWaiter oldWaiter=waiter;
		//如果有某个对话正在等待状态
		while(oldWaiter!=null){
			ThreadUtils.doWait(oldWaiter);
			oldWaiter=waiter;
		}
		//将当前线程注册进等待状态
		ConsoleWaiter myWaiter=new ConsoleWaiter(validValues);
		LogUtil.show(msg);
		waiter=myWaiter;
		ThreadUtils.doWait(myWaiter);
		return myWaiter.get();
	}
	
	protected  ConsoleShell getSubShell(){
		return sub.get();
	}
	
	public AbstractConsoleShell(ConsoleShell parent){
		this.parent=parent;
	}
	
	public final ShellResult perform(String str,boolean show,String... params) {
		//process Waiter
		if(waiter!=null && waiter.get()==null){//有会话正在等待输入。
			ConsoleWaiter theWaiter=waiter;
			if(theWaiter.set(str)){
				waiter=null;
				ThreadUtils.doNotifyAll(theWaiter);
				System.out.print(getPrompt());
			}else{
				LogUtil.show("Invalid Input, you must input:"+StringUtils.join(theWaiter.validValues,"/"));
			}
			return ShellResult.CONTINUE;
		}
		//trim处理
		str=StringUtils.trimToNull(str);
		if (str == null) {
			ShellResult r=performNull();
			if(!r.needProcess()){
				return r;
			}else{
				str=r.getCmd();
			}
		}
		//process sub shell
		if(sub.get()!=null){
			ShellResult rst=sub.get().perform(str, show,params);
			if(rst==ShellResult.TERMINATE){
				endSubShell(null);
				return ShellResult.CONTINUE;
			}else if(rst==ShellResult.CONTINUE){
				return rst;
			}else{
				str=rst.getCmd();
			}
		}
		
		//process my custom;
		ShellResult result=performCommand(str,params);
		if(show)System.out.print(getPrompt());
		return result;
	}
	
	/**
	 * 当直接敲回车时的处理方式
	 * @return
	 */
	protected ShellResult performNull() {
		System.out.print(getPrompt());
		return ShellResult.CONTINUE;
	}

	protected void startNewShell(ConsoleShell newShell) {
		Assert.isNull(sub.get());
		sub.set(newShell);
	}
	protected void endSubShell(Thread t){
		if(t==null){
			ConsoleShell shell=sub.get();
			if(shell==null)return;
			shell.exit();
			sub.set(null);
		}else{
			ConsoleShell shell=sub.getExistValue(t);
			if(shell==null)return;
			shell.exit();
			sub.set(t, null);	
		}
		System.out.print(getPrompt());
	}
	
	protected abstract ShellResult performCommand(String str, String... params);

	static class ConsoleWaiter{
		String input = null;
		String[] validValues;
		ConsoleWaiter(String[] validValues){
			this.validValues=validValues;
		}
		String get(){
			return input;
		}
		boolean set(String str){
			if(validValues.length==0 || ArrayUtils.containsIgnoreCase(validValues, str)){
				this.input=str;
				return true;
			}
			return false;
		}
	}
	
	/**
	 * 向控制台发送命令
	 * @param str
	 */
	public void sendCommand(String str,String user){
		if(user==null){ //本地命令，由键盘监听线程调用
			lock.lock(); //等待解锁，即等待上一条命令执行完毕
			lock.unlock();
			cmd=str;
			ThreadUtils.doNotify(lock);//告诉主线程开始工作。
		}else{//来自网络服务器发来的远程命令，当前线程为网络服务线程，直接在当前线程运行。
			if(perform(str, true,user)==ShellResult.TERMINATE)stop();
		}	
	}
	
	/**
	 * 终止控制台
	 */
	public void stop(){
		this.keepgoing=false;
		if(!lock.isLocked())ThreadUtils.doNotify(lock);
	}
	/**
	 * 监听本地的键盘输入
	 * @author Administrator
	 */
	protected class InputListener extends Thread{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		public InputListener(){
			super();
			setDaemon(true);//为守护线程
		}
	
		
		public void run() {
			while(true){
				String str=readline();
				sendCommand(str,null);
			}
		}
		
		private String readline() {
			try {
				String str = br.readLine();
				if (str == null)
					str = "q";
				return str;
			} catch (IOException e) {
				Exceptions.log(e);
				return null;
			}
		}
	}
}
