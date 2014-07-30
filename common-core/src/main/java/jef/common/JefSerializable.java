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
package jef.common;

import java.io.Serializable;

/**
 * 部分序列化的类中，可能有一些属性是transient的，这些属性在类反序列化时无法设置初始值。
 * 如果实现了<b>JefSerializable</b>接口，则IOUtils.loadObject时会调用init方法，你可以在
 * 这个方法中为transient的属性设置初始值。
 */
public interface JefSerializable extends Serializable{
	public void init();
}
