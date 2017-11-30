package jef.database.query;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.ColumnAliasApplier;
import jef.database.jsqlparser.SqlFunctionlocalization;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.visitor.Expression;
/**
 * 支持方言改写的表达式，需要解析
 * @author jiyi
 *
 */
final class SelectExpression2 extends SelectExpression {
	private Map<DatabaseDialect,Expression> localExpressions=new IdentityHashMap<DatabaseDialect,Expression>();
	private Expression defaultEx;
	
	public SelectExpression2(Expression defaultEx) {
		super(defaultEx.toString());
		this.defaultEx = defaultEx;
	}

	public String getSelectItem(DatabaseDialect dialect, String tableAlias,SqlContext context) {
		Expression localex=localExpressions.get(dialect);
		if(localex==null){
			localex=createLocalEx(dialect);
			ColumnAliasApplier al=new ColumnAliasApplier(tableAlias,dialect,context);
			localex.accept(al);
		}
		return super.applyProjection(localex.toString(),Collections.emptyList(),dialect);
	}

	private Expression createLocalEx(DatabaseDialect dialect) {
		Expression result;
		if(defaultEx!=null){
			result=defaultEx;
			defaultEx=null;
		}else{
			try {
				result=DbUtils.parseExpression(super.text);
			} catch (ParseException e) {
				throw new IllegalArgumentException("Can not parser expression"+text);
			}
		}
		result.accept(new SqlFunctionlocalization(dialect,null)); //TODO，无法检查存储过程
		localExpressions.put(dialect, result);
		return result;
	}
}
