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
package jef.database;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import jef.codegen.EnhanceTaskASM;
import jef.common.log.LogUtil;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.resource.ClasspathLoader;

public class JefClassLoader extends URLClassLoader {
	private org.slf4j.Logger log=LoggerFactory.getLogger(this.getClass());

	public JefClassLoader(URL[] urls, ClassLoader cl, URLClassLoader original) {
		super(urls, cl);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (name.startsWith("javassist."))
			return super.findClass(name);
		if (name.startsWith("jef.database"))
			return super.findClass(name);
		if (name.startsWith("org.apache"))
			return super.findClass(name);
		if (name.startsWith("javax."))
			return super.findClass(name);

		URL u1=getResource(name.replace('.', '/')+".class");
		URL u2=getResource(name.replace('.', '/')+"$Field.class");
		
		if(u1==null){
			throw new ClassNotFoundException(name);
		}
		EnhanceTaskASM task=new EnhanceTaskASM(new ClasspathLoader());
		byte[] enhanced;
		try{
			enhanced=task.doEnhance(IOUtils.toByteArray(u1), u2==null?null:IOUtils.toByteArray(u2));
		} catch (Exception e) {
			LogUtil.exception(e);
			throw new ClassNotFoundException(name);
		}
		if(enhanced==null){
			return super.findClass(name); 
		}else if(enhanced.length==0){
			if(log.isDebugEnabled())log.trace("Class " + name + " has already enhanced!");
			return super.findClass(name);
		}
		
		if(log.isDebugEnabled())log.trace("Runtime Enhance Class For Easyframe ORM:" + name);
		return this.defineClass(name,enhanced, 0, enhanced.length);
	}

	public static void main(String[] args) {
		if (args.length >= 1) {
			try {
				ClassLoader base = ClassLoader.getSystemClassLoader();
				URL[] urls;
				if (base instanceof URLClassLoader) {
					urls = ((URLClassLoader) base).getURLs();
				} else {
					urls = new URL[] { new File(".").toURI().toURL() };
				}
				urls=filterTest(urls);
				JefClassLoader loader = new JefClassLoader(urls, base.getParent(), null);
				Class<?> clas = loader.loadClass(args[0]);
				Class<?>[] ptypes = new Class[] { args.getClass() };
				Method main = clas.getDeclaredMethod("main", ptypes);
				String[] pargs = new String[args.length - 1];
				System.arraycopy(args, 1, pargs, 0, pargs.length);
				Thread.currentThread().setContextClassLoader(loader);
				main.invoke(null, new Object[] { pargs });
			} catch (Exception e) {
				LogUtil.exception(e);
			}
		} else {
			System.out.println("Usage: JefClassLoader main-class args...");
		}
	}

	private static URL[] filterTest(URL[] urls) {
		if(StringUtils.isNotEmpty(System.getProperty("include.test"))){
			return urls;
		}
		List<URL> result=new ArrayList<URL>();
		for(URL u:urls){
			String s=u.toString();
			if(s.endsWith("test-classes/") || s.endsWith("test-classes")){
				continue;
			}
			result.add(u);
		}
		return result.toArray(new URL[result.size()]);
	}
}
