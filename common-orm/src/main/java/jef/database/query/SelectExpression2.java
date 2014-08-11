package jef.database.query;

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

	public String getSelectItem(DatabaseDialect profile, String tableAlias,SqlContext context) {
		Expression localex=localExpressions.get(profile);
		if(localex==null){
			localex=createLocalEx(profile);
			ColumnAliasApplier al=new ColumnAliasApplier(tableAlias,profile,context);
			localex.accept(al);
		}
		return localex.toString();
	}

	private Expression createLocalEx(DatabaseDialect profile) {
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
		result.accept(new SqlFunctionlocalization(profile,null)); //TODO，无法检查存储过程
		localExpressions.put(profile, result);
		return result;
	}
}
