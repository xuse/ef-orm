package jef.database.dialect.type;

import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerExprParser;
import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerSelectParser;
import com.alibaba.druid.sql.parser.SQLExprParser;
import com.alibaba.druid.sql.parser.SQLSelectParser;

public abstract class ParserFactory {

	public abstract SQLExprParser getExprParser(String expr);
	public abstract SQLSelectParser getSelectParser(String expr);
	
	public final static class Default extends ParserFactory {
		public SQLExprParser getExprParser(String expr) {
			return new SQLExprParser(expr);
		}

		@Override
		public SQLSelectParser getSelectParser(String expr) {
			return new SQLSelectParser(getExprParser(expr));
		}
	}

	public final static class SQLServer extends ParserFactory {
		public SQLExprParser getExprParser(String expr) {
			return new SQLServerExprParser(expr);
		}

		@Override
		public SQLSelectParser getSelectParser(String expr) {
			return new SQLServerSelectParser(getExprParser(expr));
		}
	}

}
