package jef.database.cache;

import java.io.StringReader;

import javax.persistence.PersistenceException;

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.parser.TokenMgrError;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.VisitorAdapter;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.parser.Lexer;
import com.alibaba.druid.sql.parser.SQLExprParser;
import com.alibaba.druid.sql.parser.Token;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;

public abstract class WhereParser {
	static {
		String a = "A123B";
		if (a.toUpperCase() != a) {// String的实现必须满足大写字符串取大写还是本身的要求
			throw new UnsupportedClassVersionError("The JDK Implementation is too old!");
		}
	}

	abstract String process(String where);

	public static final WhereParser NATIVE = new WhereParser() {
		@Override
		String process(String where) {
			StSqlParser parser = new StSqlParser(new StringReader(where));
			try {
				Expression exp = parser.WhereClause();
				removeAliasAndCase(exp);
				return exp.toString();
			} catch (ParseException e) {
				throw new PersistenceException("[" + where + "]", e);
			} catch (TokenMgrError e) {
				throw new PersistenceException("[" + where + "]", e);
			}
		}
	};

	public static void removeAliasAndCase(Expression exp) {
		exp.accept(new VisitorAdapter() {
			public void visit(Column tableColumn) {
				tableColumn.setTableAlias(null);
				String s = tableColumn.getColumnName();
				String s2 = s.toUpperCase();
				char c1 = s2.charAt(0);
				if (c1 == '"') {
					s2 = s2.substring(1, s2.length() - 1);
				}
				if (s2 != s) {
					tableColumn.setColumnName(s2);
				}
			}
		});
	}

	public static final WhereParser DRUID = new WhereParser() {
		@Override
		String process(String where) {
			SQLExprParser parser = new SQLExprParser(where);
			Lexer lexer = parser.getLexer();
			if (lexer.token() == Token.WHERE) {
				lexer.nextToken();
				SQLExpr exp = parser.expr();
				SQLASTOutputVisitor v = new SQLASTOutputVisitor(new StringBuilder(where.length() - 6)) {
					@Override
					public boolean visit(SQLIdentifierExpr x) {
						print(x.getName().toUpperCase());
						return false;
					}

					public boolean visit(SQLPropertyExpr x) {
						print(x.getName().toUpperCase());
						return false;
					}
				};
				v.setPrettyFormat(false);
				exp.accept(v);
				return v.getAppender().toString();
			} else {
				throw new PersistenceException("parse where error[" + where + "]");
			}
		}

	};
}
