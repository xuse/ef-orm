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
package jef.database.query;

/**
 * 级联关系类型，分为
 * <ul>
 * <li>{@link #ONE_TO_ONE}</li>
 * <li>{@link #ONE_TO_MANY}</li>
 * <li>{@link #MANY_TO_ONE}</li>
 * <li>{@link #MANY_TO_MANY}</li>
 * </ul>
 **/
/*
 * 级联行为定义——
不同关系类型下发生的动作 (本表简写作M,级联引用表简写作T。下同)
OneToOne
	Insert
		1 插入 M
		2 检查 T —— 新对象则插入 旧对象且变化则更新 
	Update
		1 更新 M
		2 检查 T —— 新对象则插入 旧对象且变化则更新 不再使用则删除
	Delete	
		1 删除 M
		2 删除 T

OneToMany
	Insert
		1 插入 M
		2 检查 T —— 新对象则插 旧对象且变化则更新
	Update
		1 更新 M
		2 检查 T —— 新对象则插入 旧对象且变化则更新 不再使用则删除
	Delete	
		1 删除 M
		2 删除 T
ManyToOne
	Insert
		1 检查 T —— 新对象则插入 旧对象且变化则更新; 
		2 插入 M
	Update	
		1 检查 T —— 新对象则插入 旧对象且变化则更新。不做级联删除
		2 更新 M
	Delete	
		1 删除 M 不做级联删除。
ManyToMany
	Insert
		1 插入 M
		2 检查 T —— 新对象则插入 旧对象且变化则更新。
	Update
		1 更新 M
		2 检查 T —— 新对象则插入 旧对象且变化则更新。不做级联删除	
	Delete	
		1 删除当前对象 不做级联删除。
 */
public enum ReferenceType {
	/**
	 * 一对一
	 */
	ONE_TO_ONE,
	/**
	 * 一对多
	 */
	ONE_TO_MANY,
	/**
	 * 多对一
	 */
	MANY_TO_ONE,
	/**
	 * 多对多
	 */
	MANY_TO_MANY;

	private ReferenceType reverse = this;
	private boolean toOne;

	private ReferenceType(){
		toOne=(this.ordinal() % 2) ==0;
	}
	
	public ReferenceType reverse() {
		return reverse;
	}
	
	public boolean isToOne(){
		return toOne;
	}

	static {
		ONE_TO_MANY.reverse = MANY_TO_ONE;
		MANY_TO_ONE.reverse = ONE_TO_MANY;
	}
}
