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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jef.common.log.LogUtil;
import jef.tools.Assert;

public class StreamGobbler extends Thread {
	InputStream is;
	private StringBuilder sb = new StringBuilder();

	public final void run() {
		Assert.notNull(is);
		doTask();
	}

	protected void doTask() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			int n;
			while ((n = isr.read()) != -1) {
				process((char) n);
			}
			// 结束时如果还有别的字符就提示结束
			if (sb.length() > 0) {
				process('\n');
			}
		} catch (IOException ioe) {
			LogUtil.exception(ioe);
		}
	}

	protected void process(char n) {
		System.out.print(n);
		if (n == '\n') {
			LogUtil.showToOnthers(sb.toString(), true);
			sb = new StringBuilder();
		} else if (n == '\r') {
		} else {
			sb.append(n);
			if (n == '?') {
				LogUtil.showToOnthers(sb.toString(), false);
				sb = new StringBuilder();
			}
		}
	}
}
