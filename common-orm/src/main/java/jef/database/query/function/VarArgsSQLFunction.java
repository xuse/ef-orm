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
import jef.database.query.SqlExpression;

/**
 * Support for slightly more general templating than {@link StandardSQLFunction}, with an unlimited number of arguments.
 *
 * @author Gavin King
 */
public class VarArgsSQLFunction extends BaseArgumentSqlFunction {
	private final String begin;
	private final String sep;
	private final String end;


	/**
	 * Constructs a VarArgsSQLFunction instance with a 'dynamic' return type.  For a dynamic return type,
	 * the type of the arguments are used to resolve the type.  An example of a function with a
	 * 'dynamic' return would be <tt>MAX</tt> or <tt>MIN</tt> which return a double or an integer etc
	 * based on the types of the arguments.
	 *
	 * @param begin The beginning of the function templating.
	 * @param sep The separator for each individual function argument.
	 * @param end The end of the function templating.
	 *
	 * @see #getReturnType Specifically, the 'firstArgumentType' argument is the 'dynamic' type.
	 */
	public VarArgsSQLFunction(String begin, String sep, String end) {
		this.begin = begin;
		this.sep = sep;
		this.end = end;
	}

	/**
	 * Called from {@link #render} to allow applying a change or transformation
	 * to each individual argument.
	 *
	 * @param argument The argument being processed.
	 * @return The transformed argument; may be the same, though should never be null.
	 */
	protected String transformArgument(Expression argument) {
		return argument.toString();
	}

	public String getName() {
		return sep;
	}

	public Expression renderExpression(List<Expression> arguments){
		StringBuffer buf = new StringBuffer().append( begin );
		for ( int i = 0; i < arguments.size(); i++ ) {
			buf.append( transformArgument(arguments.get( i ) ) );
			if ( i < arguments.size() - 1 ) {
				buf.append( sep );
			}
		}
		String sql=buf.append( end ).toString();
		return new SqlExpression(sql);
	}
}
