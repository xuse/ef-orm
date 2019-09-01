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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jef.tools.Exceptions;
import jef.ui.ConsoleShell;

public abstract class DefaultBatchConsoleShell extends AbstractConsoleShell {
	protected static final ScheduledExecutorService pool=Executors.newScheduledThreadPool(3);
	
	
	public DefaultBatchConsoleShell(ConsoleShell parent) {
		super(parent);
	}
	
	protected int lazyMinutes=0;
	

	protected List<String> commandQueue = new ArrayList<String>();

	protected int size() {
		return commandQueue.size();
	}

	public final ShellResult performCommand(String str, String... params) {
		int type=appendCommand(str);
		if (type==RETURN_READY) {
			try {
				executeCmds(commandQueue,str);
				commandQueue.clear();
			} catch (Throwable e) {
				Exceptions.log(e);
			}
			if (isMultiBatch())
				return ShellResult.CONTINUE;
			return ShellResult.TERMINATE;
		} else if (type==RETURN_CONTINUE){
			return ShellResult.CONTINUE;	
		} else if (type==RETURN_TERMINATE){
			commandQueue.clear();
			return ShellResult.TERMINATE;
		}else{
			commandQueue.clear();
			if (isMultiBatch())
				return ShellResult.CONTINUE;
			return ShellResult.TERMINATE;
		}
	}

	/**
	 * 描述一个Shell可以执行多个批次的命令，还是只处理一个批次
	 * 
	 * @return
	 */
	protected boolean isMultiBatch() {
		return false;
	}

	public static final int RETURN_READY = -1;
	public static final int RETURN_CANCEL = 1;
	public static final int RETURN_CONTINUE = 0;
	public static final int RETURN_TERMINATE = -2;
	
	/**
	 * 试图添加命令
	 * @param str
	 * @return
	 * RETURN_READY batch完成，开始运行
	 * RETURN_CANCEL batch完成，停止运行
	 * RETURN_CONTINUE batch进行中，继续等待输入
	 * RETURN_TERMINATE batch丢弃，同时当前shell退出
	 */
	protected abstract int appendCommand(String str);
	
	protected final void executeCmds(Collection<String> commandQueue, String str) {
		if(lazyMinutes==0) {
			executeEnd(commandQueue, str);
		}else {
			final List<String> backCmds=new ArrayList<>(commandQueue);
			pool.schedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					executeEnd(backCmds,str);
					return null;
				}
			},lazyMinutes,TimeUnit.MINUTES);
		}
	};
	
	protected abstract void executeEnd(Collection<String> commandQueue, String str);
}
