package jef.database.dialect.type;

import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerExprParser;
import com.alibaba.druid.sql.parser.SQLExprParser;

public abstract class ParserFactory {

	public abstract SQLExprParser getExprParser(String expr);

	public final static class Default extends ParserFactory {
		public SQLExprParser getExprParser(String expr) {
			return new SQLExprParser(expr);
		}
	}

	public final static class SQLServer extends ParserFactory {
		public SQLExprParser getExprParser(String expr) {
			return new SQLServerExprParser(expr);
		}
	}
	
	

}
