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

import java.util.Collections;
import java.util.List;

import javax.persistence.PersistenceException;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.visitor.Expression;
import jef.tools.ArrayUtils;

/**
 * A function which takes no arguments
 * 
 * @author Michi
 */
public class NoArgSQLFunction implements SQLFunction {
    private boolean hasParenthesesIfNoArguments;
    private String name;

    public NoArgSQLFunction(String name) {
        this(name, true);
    }

    public NoArgSQLFunction(String name, boolean hasParenthesesIfNoArguments) {
        this.hasParenthesesIfNoArguments = hasParenthesesIfNoArguments;
        this.name = name;
    }

	public boolean hasArguments() {
		return false;
	}

	public boolean hasParenthesesIfNoArguments() {
		return hasParenthesesIfNoArguments;
	}

	public String getName() {
		return name;
	}

	@SuppressWarnings("unchecked")
	public Expression renderExpression(List<Expression> args){
		if ( args.size()>0 ) {
    		throw new PersistenceException("function takes no arguments: " + name);
    	}
		Function func=new Function(name);
		if(hasParenthesesIfNoArguments){
			func.setParameters(new ExpressionList(Collections.EMPTY_LIST));
		}
    	return func;
	}

	public boolean needEscape() {
		return false;
	}

	public String[] requiresUserFunction() {
		return ArrayUtils.EMPTY_STRING_ARRAY;
	}
}
