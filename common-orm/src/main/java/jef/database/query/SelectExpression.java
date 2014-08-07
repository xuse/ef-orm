package jef.database.query;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 原生查询表达式，无需解析和改写
 * @author jiyi
 *
 */
public class SelectExpression extends SingleColumnSelect {
	private String alias;

	protected String text;
	public SelectExpression(String text){
		this.text=text;
	}
	
	@Override
	public String toString() {
		return text;
	}
	
	public String getName() {
		return alias==null?text:alias;
	}

	/**
	 * 指定别名
	 * @param alias
	 * @return
	 */
	public SelectExpression as(String alias) {
		this.alias = alias;
		return this;
	}
	
	public String getSelectItem(DatabaseDialect profile, String tableAlias,SqlContext context) {
		String[] from=new String[context.queries.size()];
		String[] to=new String[context.queries.size()];
		for(int i=0;i<context.queries.size();i++){
			String f="$"+(i+1)+".";
			String t=context.queries.get(i).getSchema()+".";
			from[i]=f;
			to[i]=t;
		}
		return StringUtils.replaceEach(tableAlias, from, to);
	}

	public String getSelectedAlias(String tableAlias, DatabaseDialect profile,boolean isSelecting) {
		if(isSelecting){
			if(alias==null){
				alias="C".concat(RandomStringUtils.randomNumeric(12));
			}else{
				return DbUtils.escapeColumn(profile, profile.getColumnNameIncase(alias));
			}
		}
		return profile.getColumnNameIncase(alias);
	}

	@Override
	public String toHavingClause(DatabaseDialect profile, String tableAlias,SqlContext context) {
		String sql = "(" + getSelectItem(profile,tableAlias,context) + ")";
		return Condition.toSql(sql, havingCondOperator, havingCondValue, profile, null, null);
	}

	/**
	 * 设置having子句条件
	 * 
	 * @param oper
	 * @param value
	 * @return
	 */
	public SelectExpression having(Operator oper, Object value) {
		this.projection = projection | PROJECTION_HAVING;
		this.havingCondOperator = oper;
		this.havingCondValue = value;
		return this;
	}

	/**
	 * 当此SqlExpression作为select 设置having子句条件（该列不作为被选择列）
	 * 
	 * @param oper
	 * @param value
	 * @return
	 */
	public SelectExpression havingOnly(Operator oper, Object value) {
		this.projection = projection | PROJECTION_HAVING_NOT_SELECT;
		this.havingCondOperator = oper;
		this.havingCondValue = value;
		return this;
	}

	public boolean isSingleColumn() {
		return true;
	}

	public MappingType<?> getTargetColumnType() {
		return null;
	}
}
