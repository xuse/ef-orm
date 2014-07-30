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

/**
 * 用于描述一个ConsoleShell的运行返回结果
 * @author Administrator
 *
 */
public class ShellResult {
	public static final ShellResult CONTINUE=new ShellResult("CONTINUE");
	public static final ShellResult TERMINATE=new ShellResult("TERMINATE");
//	public static final ShellResult TERMINATE_WITH_ERROR=new ShellResult("TERMINATE_WITH_ERROR");
	private String cmd;
	public ShellResult(String string) {
		this.cmd=string;
	}
	public String getCmd() {
		return cmd;
	}
	public boolean needProcess(){
		return !(this==CONTINUE || this==TERMINATE);
	}
	public String toString(){
		return cmd;
	}
}
