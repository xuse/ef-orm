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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jef.common.Configuration;
import jef.common.Configuration.ConfigItem;
import jef.common.log.LogUtil;
import jef.tools.StringUtils;
import jef.tools.ThreadUtils;
import jef.ui.console.AbstractConsoleShell;
import jef.ui.console.ShellResult;

public abstract class ConsoleApplication extends AbstractConsoleShell{
	public static Configuration environment;
//	private boolean debug=false;
	/**
	 * 构造
	 */
	public ConsoleApplication() {
		super(null);
	};
	
	/**
	 * 主要方法，开始一个控制台程序
	 * @param args
	 */
	public void start(String... args) {
		if(getEnvironmentFileName()!=null){
			environment = new Configuration(getEnvironmentFileName());	
		}
		try {
			initApplication(args);
		} catch (Exception e1) {
			LogUtil.exception(e1);
			return;
		}
		System.out.print(getPrompt());
		new InputListener().start();//开启本地键盘输入等待线程
		
		while (keepgoing) {
			ThreadUtils.doWait(lock); //waiting for cmd
			if(cmd!=null){
				lock.lock();
				ShellResult result=perform(cmd, true);
				cmd=null;
				lock.unlock();
				if(result.needProcess()){
					LogUtil.show("ConsoleApplication\u662F\u9876\u5C42Shell\uFF0C\u4E0D\u80FD\u629B\u51FA\u547D\u4EE4\u3002");
					LogUtil.show(result.getCmd());
				}
				if(result==ShellResult.TERMINATE)break;
			}
		}
		try {
			closeApplication();
		} catch (Exception e1) {
			LogUtil.exception(e1);
		}
	}
	
	
	/**
	 * 处理命令
	 * @param str
	 * @param source
	 * @return
	 */
	protected ShellResult performCommand(String str,String... source) {
		ShellResult result=ShellResult.CONTINUE;
		if ("q".equalsIgnoreCase(str) || "exit".equalsIgnoreCase(str)) {
			return ShellResult.TERMINATE;
		} else if (str.equals("set")) {
			LogUtil.show(environment.listProperties());
		} else if (str.startsWith("set ")) {
			str = str.replace('=', ' ');
			String[] text = str.split(" ");
			if (text.length != 3) {
				LogUtil.show("set parameter error.");
			} else {
				ConfigItem ee = getEnvironmentEnum(text[1].replace('.', '_').toUpperCase());
				if (ee != null) {
					environment.update(ee, text[2]);
				} else {
					LogUtil.show("the env param is not exist.");
				}
			}
		} else {
			try {
				result=performMyCommand(str,source);
			} catch (Exception e) {
				LogUtil.exception(e);
			}
		}
		return result;
	}
	
	protected abstract ShellResult performMyCommand(String str,String...args);
	protected abstract String getEnvironmentFileName();
	protected abstract ConfigItem getEnvironmentEnum(String arg0);
	protected abstract void initApplication(String... args) throws Exception;
	protected abstract void closeApplication() throws Exception;

	public void exit() {
		throw new UnsupportedOperationException();
	}
}
