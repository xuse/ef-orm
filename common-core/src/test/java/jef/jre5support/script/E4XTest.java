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
package jef.jre5support.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import jef.common.log.LogUtil;
import jef.tools.ResourceUtils;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.shell.Global;

public class E4XTest {

	@Test
	public void mainTest(){
		File file = ResourceUtils.getResourceFile("test.js");
		ContextFactory ecf = new ContextFactory();
		System.out.println(ecf.call(new E4XAction(file)).toString());
	}
	
	private static class E4XAction implements ContextAction {
		private File file;

		public E4XAction(File file) {
			this.file = file;
		}

		public Object run(Context context) {
			try {
				Global global = new Global(context); // 参考注意事项2
				InputStream ins = new FileInputStream(file);

				Reader reader = new InputStreamReader(ins);
				Object result = context.evaluateReader(global, reader, file.getName(), 1, null);
				return Context.toString(result);
			} catch (Exception e) {
				LogUtil.exception(e);
			}
			return "!!!ERROR!!!";
		}
	}
}
