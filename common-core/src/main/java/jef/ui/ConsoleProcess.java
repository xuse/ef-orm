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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.IOUtils;

/**
 * 对命令进程的封装
 * @author Administrator
 *
 */
public class ConsoleProcess {
	private StreamGobbler errGobl;
	private StreamGobbler outputGob;
	BufferedWriter bw;
	Process p;
	/**
	 * 描述此进程是否可向控制台输出信息
	 */
	private int outputBehavior = BEHAVIOR_ANY_TIME;
	final static int BEHAVIOR_ANY_TIME = 0;// 当该进程非活动时依然在屏幕上打印输出信息
	final static int BEHAVIOR_RESUME = 1;// 当该进程非活动时存储输出信息，一旦切换到该进程就显示存储的所有信息
	final static int BEHAVIOR_DISCARD = 2;// 当该进程非活动时丢弃屏幕显示信息

	/**
	 * 设置进程显示的选项
	 * 
	 * @param behavior
	 */
	public void setBehavior(int behavior) {

	}

	public ConsoleProcess(Process p) {
		this.p = p;
		bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
	}

	/**
	 * 杀掉这个进程
	 */
	public void kill() {
		close();
		p.destroy();
	}

	/**
	 * 向进程发送命令
	 * 
	 * @param command
	 * @return
	 */
	public void sendCommand(String command) {
		if (getExitValue(p) == RUNNING) {
			try {
				bw.write(command + "\n");
				bw.flush();
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
	}

	/**
	 * 关闭命令发送器
	 */
	public void close() {
		IOUtils.closeQuietly(bw);
	}

	public static final int RUNNING = -1000;

	private static int getExitValue(Process ps) {
		try {
			return ps.exitValue();
		} catch (Exception e) {
			return RUNNING;
		}
	}
	
	public int getState(){
		return getExitValue(p);
	}
	protected int getOutputBehavior() {
		return outputBehavior;
	}

	protected void setOutputBehavior(int outputBehavior) {
		this.outputBehavior = outputBehavior;
	}
	public void setGobbler(StreamGobbler errorGobbler,StreamGobbler outputGobbler) {
		Assert.notNull(errorGobbler);
		Assert.notNull(outputGobbler);
		this.errGobl=errorGobbler;
		this.outputGob=outputGobbler;
		errorGobbler.is=p.getErrorStream();
		outputGobbler.is=p.getInputStream();
		errorGobbler.start();
		outputGobbler.start();

	}

	public int waitFor() throws InterruptedException {
		int result=p.waitFor();
		outputGob.join();
		return result;
	}

}
