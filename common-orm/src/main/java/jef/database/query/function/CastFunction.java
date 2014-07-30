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

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.query.SqlExpression;

/**
 * Cast渲染器，会将函数渲染为 cast(?1 as ?2)的格式
 * @author jiyi
 *
 */
public class CastFunction extends BaseArgumentSqlFunction {
	private SqlExpression fixedType;
	private String name;
	
	/**
	 * 构造成原生的cast
	 */
	public CastFunction(){
		this.name="cast";
	}
	
	/**
	 * 构造成模拟函数，如(year)等
	 * @param name
	 * @param toType fixed cast type.
	 */
	public CastFunction(String name,String toType){
		
		this.fixedType=new SqlExpression(toType);
	}

	public String getName() {
		return name;
	}

	public Expression renderExpression(List<Expression> arguments) {
		Function func;
		if(fixedType!=null){
			assertParam(arguments, 1);
			func=new Function("cast",arguments.get(0),fixedType);
			func.getParameters().setBetween(" as ");
		}else{
			assertParam(arguments, 2);
			func=new Function("cast");
			func.setParameters(new ExpressionList(arguments).setBetween(" as "));
		}
		return func;
	}

}
