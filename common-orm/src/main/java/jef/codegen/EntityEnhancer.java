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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.tools.ClassScanner;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.resource.ClasspathLoader;
import jef.tools.resource.IResource;

/**
 * JEF中的Entity静态增强任务类 <h3>作用</h3> 这个类中提供了{@link #enhance(String...)}
 * 方法，可以对当前classpath下的Entity类进行字节码增强。
 * 
 * 
 * @author jiyi
 * @Date 2011-4-6
 */
public class EntityEnhancer {
	private String includePattern;
	private String[] excludePatter;
	private List<URL> roots;
	PrintStream out = System.out;
	private EnhanceTaskASM enhancer;
	private static final Logger log = LoggerFactory.getLogger(EntityEnhancer.class);

	public void setOut(PrintStream out) {
		this.out = out;
	}

	public EntityEnhancer() {
		enhancer = new EnhanceTaskASM(new ClasspathLoader());
	}
	
	public EntityEnhancer addRoot(URL url){
	    if(url!=null){
	        if(roots==null){
	            roots=new ArrayList<URL>();
	        }
	        roots.add(url);    
	    }
	    return this;
	}
	

	/**
	 * 在当前的classpath目录下扫描Entity类(.clsss文件)，使用字节码增强修改这些class文件。
	 * 
	 * @param pkgNames 要增强的包名
	 */
	public void enhance(final String... pkgNames) {
		int n = 0;
		if (roots == null || roots.size() == 0) {
			IResource[] clss = ClassScanner.listClassNameInPackage(null, pkgNames, true);
			for (IResource cls : clss) {
				if (cls.isFile()) {
					try {
						if (processEnhance(cls)) {
							n++;
						}
					} catch (Exception e) {
						log.error("Enhance error: {}", cls, e);
						continue;
					}
				}
			}
		} else {
			for (URL root : roots) {
				IResource[] clss = ClassScanner.listClassNameInPackage(root, pkgNames, true);
				for (IResource cls : clss) {
					if (!cls.isFile()) {
						continue;
					}
					try {
						if (processEnhance(cls)) {
							n++;
						}
					} catch (Exception e) {
						log.error("Enhance error: {}", cls, e);
						continue;
					}
				}
			}
		}
		out.println(n + " classes enhanced.");
	}

	/**
	 * 增强制定名称的类
	 * @param className 类全名
	 * @return 是否进行增强
	 */
	public boolean enhanceClass(String className) {
		URL url = this.getClass().getClassLoader().getResource(className.replace('.', '/') + ".class");
		if (url == null) {
			throw new IllegalArgumentException("not found " + className);
		}
		try {
			return enhance(IOUtils.urlToFile(url), className);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean enhance(File f, String cls) throws IOException, Exception {
		EnhanceTaskASM enhancer = new EnhanceTaskASM(null);
		File sub = new File(f.getParentFile(), StringUtils.substringAfterLastIfExist(cls, ".").concat("$Field.class"));
		byte[] result = enhancer.doEnhance(IOUtils.toByteArray(f), (sub.exists() ? IOUtils.toByteArray(sub) : null));
		if (result != null) {
			if (result.length == 0) {
				out.println(cls + " is already enhanced.");
			} else {
				IOUtils.saveAsFile(f, result);
				out.println("enhanced class:" + cls);// 增强完成
				return true;
			}
		}
		return false;
	}

	private boolean processEnhance(IResource cls) throws Exception {
		File f = cls.getFile();
		File sub = new File(IOUtils.removeExt(f.getAbsolutePath()).concat("$Field.class"));
		if (!f.exists()) {
			return false;
		}
		byte[] result = enhancer.doEnhance(IOUtils.toByteArray(f), (sub.exists() ? IOUtils.toByteArray(sub) : null));
		if (result != null) {
			if (result.length == 0) {
				out.println(cls + " is already enhanced.");
			} else {
				IOUtils.saveAsFile(f, result);
				out.println("enhanced class:" + cls);// 增强完成
				return true;
			}
		}
		return false;
	}

	/**
	 * 设置类名Pattern
	 * 
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
}
