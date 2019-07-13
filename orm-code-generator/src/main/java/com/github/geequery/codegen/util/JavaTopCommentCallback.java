package com.github.geequery.codegen.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.TextFileCallback;


//用于为每个Java文件处理顶部的声明的小工具
public class JavaTopCommentCallback extends TextFileCallback{
	private int isPkgDeclare=0;
	private String packagePattern[];
	private String comments="";
	
	
	public JavaTopCommentCallback(String file,String... packages){
		this.packagePattern=packages;
		if(file!=null){
			File f=new File(file);
			Assert.fileExist(f);
			try {
				comments=IOUtils.asString(f, null);
			} catch (IOException e) {
				LogUtil.exception(e);
			}	
		}
	}
	
	public String processLine(String line) {
		if(isPkgDeclare>0){
			return line;
		}
		if(line.trim().startsWith("package ")){
			String pp=StringUtils.substringAfter(line, "package ").trim();
			if(GenUtil.isMatchStart(packagePattern,pp)){
				isPkgDeclare=1;
				return comments.concat(line);	
			}else{
				isPkgDeclare=-1;
			}
		}
		return null;
	}
	

	@Override
	protected void beforeProcess(File source, File target, BufferedWriter w) throws IOException {
		 isPkgDeclare=0;
	}
	

	@Override
	protected Dealwith dealwithSourceOnSuccess(File source) {
		return Dealwith.REPLACE;
	}

	@Override
	public boolean isSuccess() {
		return isPkgDeclare>0;
	}

	@Override
	protected boolean breakProcess() {
		return isPkgDeclare<0;
	}
	
	public static void main(String[] args) throws IOException {
		File target=new File("E:/Git/cxf-plus/src/main/java/jef/com/sun");
		IOUtils.processFiles(target, new JavaTopCommentCallback("d:/oracle.txt"), "java");
		
		
	}
}
