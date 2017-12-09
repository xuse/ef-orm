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

import java.io.File;

import jef.tools.Assert;
import jef.tools.StringUtils;

/**
 * 使用命令运行其他进程的相关工具封装
 * 
 * @author jiyi
 *
 */
public class Commands {

	/**
	 * 接口，用来接收和处理操作过程中产生的控制台输入和输出
	 * 
	 * @author jiyi
	 *
	 */
	public static abstract class Interacts {

		/**
		 * 当控制台标准输出一行时发生
		 * 
		 * @param line
		 */
		protected abstract void onStdOut(String line);

		/**
		 * 当控制台错误输出一行时发生
		 * 
		 * @param line
		 */
		protected abstract void onStdErr(String line);

		ThreadLocal<ProcessHandler> handler = new ThreadLocal<ProcessHandler>();

		/**
		 * 用于在交互过程中向控制台发送命令
		 * 
		 * @param command
		 */
		protected void send(String command) {
			ProcessHandler h = handler.get();
			if (h != null)
				h.sendCommand(command);
		}
	}

	/**
	 * 运行指定的命令，返回ConsoleProcess对象，可以控制运行过程中的交互
	 * 
	 * @param text
	 * @param folders
	 * @return
	 */
	public static ProcessHandler execute(String text, Interacts transact, File folder) {
		if (folder != null) {
			Assert.isTrue(Boolean.valueOf(folder.exists()), "Work Directory not exist.");
			Assert.isTrue(Boolean.valueOf(folder.isDirectory()), "is not a Directory");
		}
		ProcessBuilder rt = new ProcessBuilder(toCmdArray(text, true));
		if(folder!=null){
			rt.directory(folder);
		}
		return new ProcessHandlerImpl(rt, transact);
	}

	/**
	 * @param command
	 *            命令
	 * @param folder
	 *            工作目录
	 * @return
	 */
	public static int execute(String command, File folder) {
		return execute(command, null, folder).waitfor();
	}

	private static String[] toCmdArray(String text, boolean raw) {
		if (raw)
			return StringUtils.tokenizeToStringArray(text, " ");
		String osName = System.getProperty("os.name");
		String cmd[] = new String[3];
		//在Windows上，一大堆命令都是内部命令，由CMD解释器，故不确定要执行的命令是内部命令还是外部命令时，需要带上解释器来执行。
		if ("Windows 98".equals(osName)) {
			cmd[0] = "command.com";
			cmd[1] = "/C";
			cmd[2] = text;
		} else if(osName.startsWith("Windows")){
			cmd[0] = "cmd.exe";
			cmd[1] = "/C";
			cmd[2] = text;
		}else{
			return text.split(" ");	
		}
		return cmd;
	}
}
