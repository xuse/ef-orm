package jef.database.cache;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.parser.Lexer;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.sql.parser.SQLExprParser;
import com.alibaba.druid.sql.parser.SQLSelectParser;
import com.alibaba.druid.sql.parser.Token;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.parser.TokenMgrError;
import jef.database.jsqlparser.statement.select.Join;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.VisitorSimpleAdapter;
import jef.tools.StringUtils;

public abstract class WhereParser {
	static {
		String a = "A123B";
		if (a.toUpperCase() != a) {// String的实现必须满足大写字符串取大写还是本身的要求
			throw new UnsupportedClassVersionError("The JDK Implementation is too old!");
		}
	}

	abstract String process(String where, DatabaseDialect profile);

	/**
	 * 解析后得到一下内容
	 * 
	 * @param tableDef
	 * @param profile
	 * @return key: 规范话后的table定义 List<String>各个表的名称转大写
	 */
	abstract PairSO<List<String>> parseTables(String tableDef, DatabaseDialect profile);

	public static final class NativeImpl extends WhereParser {
		@Override
		String process(String where, DatabaseDialect profile) {
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

		@Override
		PairSO<List<String>> parseTables(String tableDef, DatabaseDialect profile) {
			// root_cus T1 left join ENUMATIONTABLE T2 ON T1.CODE=T2.CODE and
			// T2.TYPE='1'
			StSqlParser parser = new StSqlParser(new StringReader(tableDef));
			List<String> tables = new ArrayList<String>();
			try {
				FromItem exp = parser.FromItem();
				@SuppressWarnings("unchecked")
				List<Join> joins = parser.JoinsList();
				String result = ast2String(exp, joins, tables);
				return new PairSO<List<String>>(result, tables);
			} catch (ParseException e) {
				throw new PersistenceException("[" + tableDef + "]", e);
			} catch (TokenMgrError e) {
				throw new PersistenceException("[" + tableDef + "]", e);
			}
		}

		// 转换为规范化文本
		private String ast2String(FromItem fromItem, List<Join> joins, List<String> tables) {
			removeAliasAndCase(fromItem);
			StringBuilder sb = new StringBuilder(64);
			fromItem.appendTo(sb);
			tables.add(fromItem.toWholeName());

			if (joins != null) {
				Iterator<Join> it = joins.iterator();
				while (it.hasNext()) {
					Join join = (Join) it.next();
					join.accept(VA);
					tables.add(join.getRightItem().toWholeName());
					if (join.isSimple()) {
						join.appendTo(sb.append(", "));
					} else {
						join.appendTo(sb.append(' '));
					}
				}
			}
			return sb.toString();
		}
	};
	
	public static void removeAliasAndCase(FromItem fromItem) {
		fromItem.accept(VA);
	}
	
	public static void removeAliasAndCase(Expression exp) {
		exp.accept(VA);
	}

	//2018-11-28 BUGFIX：将VisitorAdapter更换为无状态的类，从而避免多线程并发问题. 
	private static final VisitorSimpleAdapter VA = new VisitorSimpleAdapter() {
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

		@Override
		public void visit(Table tableName) {
			tableName.setName(StringUtils.upperCase(tableName.getName()));
			tableName.setAlias(null);
			super.visit(tableName);
		}
	};
	
	

	public static final class DruidImpl extends WhereParser {
		@Override
		String process(String where, DatabaseDialect profile) {
			SQLExprParser parser = profile.getParserFactory().getExprParser(where);
			Lexer lexer = parser.getLexer();
			if (lexer.token() == Token.WHERE) {
				try {
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
				} catch (ParserException e) {
					LogUtil.warn("Druid Parser error:{}\nException:{}", where, e);
					throw e;
				}
			} else {
				throw new PersistenceException("parse where error[" + where + "]");
			}
		}

		@Override
		PairSO<List<String>> parseTables(String tableDef, DatabaseDialect profile) {
			SQLSelectParser parser = profile.getParserFactory().getSelectParser(tableDef);
			Lexer lexer = parser.getLexer();
			final List<String> tables=new ArrayList<String>();
			try {
				SQLTableSource exp=	parser.parseTableSource();
				SQLASTOutputVisitor v = new SQLASTOutputVisitor(new StringBuilder(tableDef.length())) {
					@Override
					public boolean visit(SQLExprTableSource x) {
						x.setAlias(null);
						return super.visit(x);
					}

					@Override
					public boolean visit(SQLIdentifierExpr x) {
						print(x.getName().toUpperCase());
						tables.add(x.getName().toUpperCase());
						return false;
					}

					public boolean visit(SQLPropertyExpr x) {
						print(x.getName().toUpperCase());
						return false;
					}
				};
				v.setPrettyFormat(false);
				exp.accept(v);
				String result=v.getAppender().toString();			
				return new PairSO<List<String>>(result, tables);
			} catch (ParserException e) {
				LogUtil.warn("Druid Parser error:{}\nException:{}", tableDef, e);
				throw e;
			}

		}

	};
}
