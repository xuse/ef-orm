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

import jef.tools.StringUtils;

public abstract class ConsoleTask implements Comparable<ConsoleTask> {
	protected boolean enable = true;
	protected int executedCount = 0;

	/** 本次调用是启动后的第n次 */

	public ConsoleTask() {
	};

	public int getExecutedCount() {
		return executedCount;
	}

	protected abstract Object start(String... args);

	/**
	 * @param args
	 */
	public Object run(String[] args) {
		Object obj=null;
		if (enable) {
			obj=this.start(args);
			executedCount++;
		}
		return obj;
	}

	public abstract String getName();

	public void setEnable(String str) {
		this.enable = StringUtils.toBoolean(str, true);
	}

	public boolean getEnble() {
		return enable;
	}

	public int compareTo(ConsoleTask arg0) {
		return getName().compareTo(arg0.getName());
	}
}
