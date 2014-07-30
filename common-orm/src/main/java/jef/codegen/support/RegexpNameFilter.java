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
package jef.codegen.support;

import jef.tools.string.RegexpUtils;

/**
 * 使用正则表达式的名称过滤器
 * 
 * @author Administrator
 *
 */
public class RegexpNameFilter implements NameFilter{
	private String includePattern;
	private String[] excludePatter;
	
	public RegexpNameFilter(){};
	
	/**
	 * 构造
	 * @param include  要包含的正则表达式
	 * @param exclude  要排除的正则表达式
	 */
	public RegexpNameFilter(String include,String... exclude){
		this.includePattern=include;
		this.excludePatter=exclude;
	}

	/**
	 * 计算传入的文件名是否匹配正则表达式模板
	 * @param 传入文件名
	 * @return 如果指定了包含正则表达式，不匹配的话就返回false;
	 * 
	 * 如果指定了排除正则表达式，并匹配了的话，返回false。
	 * 
	 * 剩余情况都返回true
	 */
	public boolean accept(String name) {
		if (this.includePattern != null) {
			if (!RegexpUtils.matches(name, includePattern)) {
				return false;
			}
		}
		if (this.excludePatter != null) {
			for (String sp : excludePatter) {
				if (RegexpUtils.matches(name, sp)) {
					return false;
				}
			}
		}
		return true;
	}
}
