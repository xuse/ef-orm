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
package jef.codegen;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import jef.codegen.support.RegexpNameFilter;
import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.ClassScanner;
import jef.tools.IOUtils;
import jef.tools.URLFile;

/**
 * JEF中的Entity静态增强任务类
 * <h3>作用</h3>
 * 这个类中提供了{@link #enhance(String...)}方法，可以对当前classpath下的Entity类进行字节码增强。
 * 
 * 
 * @author jiyi 
 * @Date 2011-4-6
 */
public class EntityEnhancer {
	private String includePattern;
	private String[] excludePatter;
	private File[] roots;
	PrintStream out = System.out;


	public void setOut(PrintStream out) {
		this.out = out;
	}

	/**
	 * 在当前的classpath目录下扫描Entity类(.clsss文件)，使用字节码增强修改这些class文件。
	 * @param pkgNames
	 */
	public void enhance(final String... pkgNames) {
		if (roots == null || roots.length == 0) {
				StackTraceElement[] eles = Thread.currentThread().getStackTrace();
			StackTraceElement last = eles[eles.length - 1];
			try {
				Class<?> clz=Class.forName(last.getClassName());
				roots = IOUtils.urlToFile(ArrayUtils.toArray(clz.getClassLoader().getResources("."), URL.class));
			} catch (IOException e) {
				LogUtil.exception(e);
			} catch (ClassNotFoundException e) {
				LogUtil.exception(e);
			}
		}
		if(roots==null){
			return;
		}
		int n = 0;
		for (File root : roots) {
			String[] clss = ClassScanner.listClassNameInPackage(root, pkgNames, true, true, false);
			for (String cls : clss) {
				RegexpNameFilter filter = new RegexpNameFilter(includePattern, excludePatter);
				if (!filter.accept(cls)) {
					continue;
				}
				if (cls.startsWith("org.apache")||cls.startsWith("javax."))
					continue;
				if(cls.endsWith("$Field"))
					continue;
				try {
					if(processEnhance(root,cls)){
						n++;
					}
				} catch (Exception e) {
					LogUtil.exception(e);
					LogUtil.error("Enhance error: " + cls + ": " + e.getMessage());
					continue;
				}
			}
		}

		out.println(n + " classes enhanced.");
	}
	public boolean enhanceClass(String string) {
		URL url=this.getClass().getClassLoader().getResource(string.replace('.', '/')+".class");
		if(url==null){
			throw new IllegalArgumentException("not found "+string);
		}
		URLFile file=new URLFile(url);
		if(!file.isLocalFile()){
			throw new IllegalArgumentException("not a local file."+string);
		}
		try {
			return enhance(file.getLocalFile(),string);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean enhance(File f,String cls) throws IOException, Exception {
		EnhanceTaskASM enhancer=new EnhanceTaskASM(null,roots);
		File sub = new File(f.getParentFile(), cls.replace('.', '/').concat("$Field.class"));
		byte[] result=enhancer.doEnhance(cls, IOUtils.toByteArray(f), (sub.exists()?IOUtils.toByteArray(sub):null));
		if(result!=null){
			if(result.length==0){
				out.println(cls + " is already enhanced.");
			}else{
				IOUtils.saveAsFile(f,result);
				out.println("enhanced class:" + cls);// 增强完成
				return true;
			}
		}
		return false;
	}

	private boolean processEnhance(File root,String cls) throws Exception {
		EnhanceTaskASM enhancer=new EnhanceTaskASM(root,roots);
		File f = new File(root, cls.replace('.', '/').concat(".class"));
		File sub = new File(root, cls.replace('.', '/').concat("$Field.class"));
		if (!f.exists()) {
			out.println("class file " + f.getAbsolutePath() + " is not found");
			return false;
		}
		byte[] result=enhancer.doEnhance(cls, IOUtils.toByteArray(f), (sub.exists()?IOUtils.toByteArray(sub):null));
		if(result!=null){
			if(result.length==0){
				out.println(cls + " is already enhanced.");
			}else{
				IOUtils.saveAsFile(f,result);
				out.println("enhanced class:" + cls);// 增强完成
				return true;
			}
		}
		return false;
	}

	/**
	 * 设置类名Pattern
	 * @return
	 */
	public String getIncludePattern() {
		return includePattern;
	}

	public EntityEnhancer setIncludePattern(String includePattern) {
		this.includePattern = includePattern;
		return this;
	}

	public String[] getExcludePatter() {
		return excludePatter;
	}

	public EntityEnhancer setExcludePatter(String[] excludePatter) {
		this.excludePatter = excludePatter;
		return this;
	}

	public File[] getRoot() {
		return roots;
	}

	public void setRoot(File... roots) {
		this.roots = roots;
	}
}
