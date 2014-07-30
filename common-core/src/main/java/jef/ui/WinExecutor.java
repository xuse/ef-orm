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

import jef.common.log.LogUtil;
import jef.tools.Assert;


public class WinExecutor {
	/**
	 * 运行指定的命令，返回ConsoleProcess对象，可以控制运行过程中的交互
	 * @param text
	 * @param folders
	 * @return
	 */
	public static ConsoleProcess executeConsole(String text,File... folders){
		File folder=(folders.length==0)?null:folders[0];
		if(folder!=null){
			Assert.isTrue(Boolean.valueOf(folder.exists()), "Work Directory not exist.");
			Assert.isTrue(Boolean.valueOf(folder.isDirectory()), "is not a Directory");
		}
		try {
			ProcessBuilder rt = new ProcessBuilder(toCmdArray(text));
			rt.directory(folder);
			rt.redirectErrorStream(true);
			Process proc = rt.start();
			return setStreamBobbler(null, proc);
		} catch (Throwable t) {
			LogUtil.exception(t);
			return null;
		}
	}
	
	/**
	 * 直接执行指定的命令
	 * @param text
	 * @param gobblers
	 * @return
	 */
	public static int execute(String text, StreamGobbler... gobblers) {
		return execute(text,null,gobblers);
	}

	/**
	 * 直接执行指定的命令
	 * @param text
	 * @param folder
	 * @param gobblers
	 * @return
	 */
	public static int execute(String text, File folder, StreamGobbler... gobblers) {
		if(folder!=null){
			Assert.isTrue(Boolean.valueOf(folder.exists()), "Work Directory not exist.");
			Assert.isTrue(Boolean.valueOf(folder.isDirectory()), "is not a Directory");
		}
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(text, null, folder);
			setStreamBobbler(gobblers, proc);
			int exitVal = proc.waitFor();
			if (exitVal != 0)
				LogUtil.show((new StringBuilder("ExitValue: ")).append(exitVal).toString());
			return exitVal;
		} catch (Throwable t) {
			LogUtil.exception(t);
		}
		return -1;
	}
	
	/**
	 * 运行指定的命令，命令将会以cmd.exe/c 执行,即使用外壳解释
	 * @param text
	 * @param gobblers
	 * @return
	 */
	public static int executeWithShell(String text, StreamGobbler... gobblers) {
		return executeWithShell(text,null,gobblers);
	}
	
	/**
	 * 运行指定的命令，命令将会以cmd.exe/c 执行,即使用外壳解释
	 * @param command 命令
	 * @param folder 工作目录
	 * @param gobblers 输出流处理器
	 * @return
	 */
	public static int executeWithShell(String command, File folder,StreamGobbler... gobblers) {
		if(folder!=null){
			Assert.isTrue(Boolean.valueOf(folder.exists()), "Work Directory not exist.");
			Assert.isTrue(Boolean.valueOf(folder.isDirectory()), "is not a Directory");	
		}
		try {
			String[] cmd=toCmdArray(command);
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(cmd,null,folder);
			setStreamBobbler(gobblers, proc);
			int exitVal = proc.waitFor();//等待运行结束
			if (exitVal != 0)
				LogUtil.show((new StringBuilder("ExitValue: ")).append(exitVal).toString());
			return exitVal;
		} catch (Throwable t) {
			LogUtil.exception(t);
		}
		return -1;
	}
	//////////////私有方法
	private static ConsoleProcess setStreamBobbler(StreamGobbler[] gobblers, Process proc) {
		StreamGobbler errorGobbler = null;
		StreamGobbler outputGobbler = null;
		if (gobblers == null || gobblers.length == 0) {
			outputGobbler = new StreamGobbler();
			errorGobbler = new StreamGobbler();
		} else if (gobblers.length == 1) {
			outputGobbler = gobblers[0];
			errorGobbler = new StreamGobbler();
		} else if (gobblers.length == 2) {
			outputGobbler = gobblers[0];
			errorGobbler = gobblers[1];
		} else {
			throw new IllegalArgumentException("Too many StreamGobblers!");
		}
		ConsoleProcess result=new ConsoleProcess(proc);
		result.setGobbler(errorGobbler,outputGobbler);
		return result;
	}
	
	private static String[] toCmdArray(String text) {
		String osName = System.getProperty("os.name");
		String cmd[] = new String[3];
		if (osName.equals("Windows 98")) {
			cmd[0] = "command.com";
			cmd[1] = "/C";
			cmd[2] = text;
		} else {
			cmd[0] = "cmd.exe";
			cmd[1] = "/C";
			cmd[2] = text;
		}
		return cmd;
	}
}
