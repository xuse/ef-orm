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

/**
 * Emulation of <tt>locate()</tt> on Sybase
 * @author Nathan Moon
 */
public class EmuLocateOnSybase extends BaseArgumentSqlFunction{
	public String getName() {
		return "locate";
	}

	public Expression renderExpression(List<Expression> args){
//		boolean threeArgs = args.size() > 2;
//		Expression pattern = args.get(0);
//		Expression string = args.get(1);
//		Expression start = threeArgs ? args.get(2) : null;

//		StringBuffer buf = new StringBuffer();
//		buf.append("charindex(").append( pattern ).append(", ");
//		if (threeArgs) buf.append( "right(");
//		buf.append( string );
//		if (threeArgs) buf.append( ", char_length(" ).append( string ).append(")-(").append( start ).append("-1))");
//		buf.append(')');
//		return buf.toString();
		throw new UnsupportedOperationException();
	}

}
