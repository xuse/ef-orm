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
package com.github.geequery.codegen.util;

import java.io.File;
import java.io.IOException;

import com.github.geequery.codegen.ast.IClass;
import com.github.geequery.codegen.ast.IClassUtil;

import jef.common.wrapper.Holder;
import jef.tools.ArrayUtils;
import jef.tools.IOUtils;
import jef.tools.TextFileCallback;

 public class GenUtil {
	 //判断一个类是否具有 未修改 标签
	public static boolean isModified(File java) throws IOException {
		if (!java.getName().endsWith(".java")){
			throw new IllegalArgumentException();
		}
		final Holder<Integer> result=new Holder<Integer>(-1);
		IOUtils.processFile(java, new TextFileCallback(){
			@Override
			public File getTarget(File source) {
				return null;
			}

			@Override
			public String processLine(String line) {
				if(line.trim().equals("@NotModified")){
					result.set(0);
				}else if(line.trim().startsWith("public class ")){
					result.set(1);
				}
				return null;
			}
			@Override
			protected boolean breakProcess() {
				return result.get()!=-1;
			}
		});
		return result.get()!=0;
	}
	
	public static IClass toWrappedType(String primitive){
		int i=ArrayUtils.indexOf(IClass.PRIMITIVE_TYPES, primitive);
		if(i<0)throw new IllegalArgumentException("The input type "+ primitive+" is not a primitive type!");
		return IClassUtil.parse(IClass.WRAPPED_TYPES[i]);
	}
	
	public static boolean isMatchStart(String[] keywords, String str){
		if(keywords.length==0)return true;
		for(String s:keywords){
			if(str.startsWith(s)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isMatchStart(String keyword,String[] strs){
		for(String str:strs){
			if(str.startsWith(keyword)){
				return true;
			}
		}
		return false;
	}

}
