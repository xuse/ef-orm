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
import java.util.List;

import jef.common.log.LogUtil;
import jef.ui.ConsoleShell;

import  org.apache.commons.lang3.ArrayUtils;

public abstract class DefaultBatchConsoleShell extends AbstractConsoleShell {
	public DefaultBatchConsoleShell(ConsoleShell parent) {
		super(parent);
	}

	protected List<String> commandPool = new ArrayList<String>();

	protected int poolSize() {
		return commandPool.size();
	}

	public final ShellResult performCommand(String str, String... params) {
		int type=appendCommand(str);
		if (type==RETURN_READY) {
			try {
				executeEnd(commandPool.toArray(ArrayUtils.EMPTY_STRING_ARRAY),str);
				commandPool.clear();
			} catch (Throwable e) {
				LogUtil.exception(e);
			}
			if (isMultiBatch())
				return ShellResult.CONTINUE;
			return ShellResult.TERMINATE;
		} else if (type==RETURN_CONTINUE){
			return ShellResult.CONTINUE;	
		} else if (type==RETURN_TERMINATE){
			commandPool.clear();
			return ShellResult.TERMINATE;
		}else{
			commandPool.clear();
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
	
	protected abstract void executeEnd(String[] strings, String str);
}
