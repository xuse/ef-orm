/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package jef.database.query.function;

import java.util.List;

import jef.common.JefException;
import jef.database.jsqlparser.visitor.Expression;

/**
 * SQL函数的渲染器
 * @author jiyi
 *
 */
public interface SQLFunction {
	/**
	 * 是否需要转义
	 * @return 是否需要转义
	 */
	public boolean needEscape();
	/**
	 * 是否有参数
	 * @return 是否有参数
	 */
	public boolean hasArguments();

	/**
	 * 没有参数的时候要不要括号
	 * @return 没有参数的时候要不要括号
	 */
	public boolean hasParenthesesIfNoArguments();

	/**
	 * 函数名
	 * @return 函数名
	 */
	public String getName();
	
	/**
	 * 渲染表达式，注意一些函数可能是根据其参数个数来确定其功能的，如Oracle中的trunc函数
	 * @param arguments
	 * @return 渲染后的表达式
	 * @throws JefException
	 */
	public Expression renderExpression(List<Expression> arguments);
	
	/**
	 * 返回该函数需要的本地存储过程
	 */
	public String[] requiresUserFunction();
}
