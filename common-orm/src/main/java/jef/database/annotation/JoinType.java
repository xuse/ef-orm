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
package jef.database.annotation;

/**
 * SQL的四种Join类型<p>
 * <tt>
 * 因为JPA的javax.persistence.criteria.JoinType类型中没有全外连接，所以自行定义了一个JoinType
 * </tt>
 * <ul>
 * <li>{@link #INNER}</li>
 * <li>{@link #LEFT}</li>
 * <li>{@link #RIGHT}</li>
 * <li>{@link #FULL}</li>
 * </ul>
 * 
 * @author Jiyi
 * @Date 2011-4-12
 */
public enum JoinType {
	/**
	 * 内连接
	 */
	INNER,
	/**
	 * 左外连接
	 */
	LEFT,
	/**
	 * 右外连接
	 */
	RIGHT,
	/**
	 * 全外连接
	 */
	FULL;
	
	private String lowerName;
	private JoinType(){
		lowerName=this.name().toLowerCase();
	}
	

	/**
	 * 返回翻转后的连接方式
	 * @param source
	 * @return
	 */
	public static JoinType flip(JoinType source){
		switch(source){
		case LEFT:
			return RIGHT;
		case RIGHT:
			return LEFT;
		default:
			return source;
		}
	}

	/**
	 * 返回小写的名称
	 * @return
	 */
	public String nameLower() {
		return lowerName;
	}
}
