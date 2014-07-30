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

/**
 * <li>YES 总是覆盖</li>
 * <li>NO 总不覆盖</li>
 * <li>ESCAPE_NAME 自动改名</li>
 * <li>AUTO 判断修改状态，然后确定</li>
 */
public enum OverWrittenMode {
	YES,
	NO,
	ESCAPE_NAME,
	AUTO
}
