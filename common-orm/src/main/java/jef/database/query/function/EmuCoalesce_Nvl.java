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

/**
 * Emulation of <tt>coalesce()</tt> on Oracle, using multiple <tt>nvl()</tt> calls
 *
 * @author Jiyi
 */
public class EmuCoalesce_Nvl extends BaseArgumentSqlFunction {
	public String getName() {
		return "coalesce";
	}

	public Expression renderExpression(List<Expression> args){
		int lastIndex = args.size()-1;
		Expression last = args.remove(lastIndex);
		if ( lastIndex==0 ) {
			return last;
		}
		Expression secondLast = args.get(lastIndex-1);
		Function nvl=new Function("nvl",secondLast,last);
		args.set(lastIndex-1, nvl);
		return renderExpression( args);
	}
}
