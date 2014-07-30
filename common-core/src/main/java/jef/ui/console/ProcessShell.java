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

import java.io.File;

import jef.common.log.LogUtil;
import jef.ui.ConsoleConversation;
import jef.ui.ConsoleProcess;
import jef.ui.ConsoleShell;
import jef.ui.WinExecutor;

public class ProcessShell implements ConsoleShell{
	ConsoleProcess process;
	AbstractConsoleShell parent;

	public ProcessShell(AbstractConsoleShell console,final String showitem, final File currentFolder) {
		this.parent=console;
		final Thread mainThread=Thread.currentThread();
		Thread t=new Thread(){
			
			public void run() {
				process=WinExecutor.executeConsole(showitem, currentFolder);
				int flag=-1;
				try {
					flag=process.waitFor();
				} catch (InterruptedException e) {
					LogUtil.exception(e);
				}
				unshell(flag,mainThread);
			}
		};
		t.start();
	}

	protected void unshell(int flag,Thread t) {
		if(flag!=0 && flag!=1)LogUtil.show("Exit value:" + flag);
		parent.endSubShell(t);
	}

	
	public void exit() {
	}

	
	public String getInput(ConsoleConversation<?> consoleConversation, String msg,	String... validValues) {
		throw new UnsupportedOperationException();
	}

	
	public String getPrompt() {
		return "cmd>";
	}

	
	public ShellResult perform(String str, boolean showPrompt, String... params) {
		if(process!=null)process.sendCommand(str);
		return ShellResult.CONTINUE;
	}

}
