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
package com.github.geequery.codegen;

import java.io.File;
import java.io.IOException;

import jef.common.log.LogUtil;
import jef.common.wrapper.Holder;
import jef.tools.IOUtils;

import org.apache.commons.lang.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * 对指定源文件夹下的Java文件的包名进行替换。 将package定义替换，将import替换，将内部代码中对类的引用的全称替换。
 * 
 * 使用了基于JavaCC的词法分析工具，能准确的分析Java文件的结构，不会造成误操作。
 * 
 * @author Administrator
 */
public class PackageReplacer {
	private String from;
	private String to;
	private String charset = "UTF-8";
	private boolean isTopCommentRemover= false;
	
	public boolean isTopCommentRemover() {
		return isTopCommentRemover;
	}

	public void setTopCommentRemover(boolean isTopCommentRemover) {
		this.isTopCommentRemover = isTopCommentRemover;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public int doReplace(File... files) {
		int n = 0;
		for (File f : files) {
			n += processSourceFolder(f);
		}
		return n;
	}

	private int processSourceFolder(File root) {
		int n = 0;
		for (File java : IOUtils.listFilesRecursive(root, "java")) {
			try {
				CompilationUnit unit = JavaParser.parse(java, charset);
				if (processUnit(unit)) {
					System.out.println("Modified " + java.getPath());
					IOUtils.saveAsFile(new File(java.getPath()),
							unit.toString());
					n++;
				}
			} catch (ParseException e) {
				LogUtil.error("Analyzing " + java.getPath() + " error!");
				LogUtil.exception(e);
				continue;
			} catch (IOException e) {
				LogUtil.error("Analyzing " + java.getPath() + " error!");
				LogUtil.exception(e);
				continue;
			}
		}
		return n;
	}

	private boolean processUnit(CompilationUnit unit) {
		if(isTopCommentRemover)return true;
		final Holder<Boolean> flag = new Holder<Boolean>(false);
		VoidVisitorAdapter<String> visitor = new VoidVisitorAdapter<String>() {
			@Override
			public void visit(ImportDeclaration n, String arg) {
				String oldName = n.getName().toString();
				if (oldName.startsWith(from)) {
					n.setName(new NameExpr(StringUtils.replaceOnce(oldName,
							from, to)));
					flag.set(true);
				}
			}

			@Override
			public void visit(PackageDeclaration n, String arg) {
				String oldName = n.getName().toString().concat(".");
				if (oldName.startsWith(from)) {
					String newStr = StringUtils.replaceOnce(oldName, from, to);
					n.setName(new NameExpr(newStr.substring(0,
							newStr.length() - 1)));
					flag.set(true);
				}
			}

			@Override
			public void visit(ClassOrInterfaceType n, String arg) {
				if (n.getScope() == null)
					return;
				String oldName = n.getScope().toString().concat(".");
				if (oldName.startsWith(from)) {
					String newStr = StringUtils.replaceOnce(oldName, from, to);
					n.setScope(new ClassOrInterfaceType(newStr.substring(0,
							newStr.length() - 1)));
					flag.set(true);
				}
			}
		};
		visitor.visit(unit, null);
		boolean b = flag.get();
		return b;
	}

	public void setTo(String string) {
		this.to = string + ".";
	}

	public void setFrom(String string) {
		this.from = string + ".";
	}
}
