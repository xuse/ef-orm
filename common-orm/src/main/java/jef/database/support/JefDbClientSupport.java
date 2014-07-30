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
package jef.database.support;

import jef.database.Session;

/**
 * 任何对象实现此接口后，JEF-IOC在初始化BeanFactory时都会将DbClient对象自动注入到这个Bean当中。
 * <br>如果BeanFactory中有多个DbClient实例，请不要使用这种方法注入，
 * 因为无法知道是哪个DbClient被注入了。
 * @author jiyi
 */
public interface JefDbClientSupport {
	void setClient(Session client);
}
