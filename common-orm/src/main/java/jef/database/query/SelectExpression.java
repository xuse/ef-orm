package jef.database.query;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.wrapper.clause.HavingEle;

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

	public String getSelectedAlias(String tableAlias, DatabaseDialect profile) {
		if(alias==null){
			alias="C".concat(RandomStringUtils.randomNumeric(12));
			return profile.getColumnNameToUse(alias);
		}else{
			return DbUtils.escapeColumn(profile, profile.getColumnNameToUse(alias));
		}
	}

	@Override
	public HavingEle toHavingClause(DatabaseDialect profile, String tableAlias,SqlContext context) {
		String sql = "(" + getSelectItem(profile,tableAlias,context) + ")";
		HavingEle h=new HavingEle();
		h.column=sql;
		h.sql=Condition.toSql(sql, havingCondOperator, havingCondValue, profile, null, null);
		h.havingCondOperator=this.havingCondOperator;
		h.havingCondValue=this.havingCondValue;
		return h;
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

	public ColumnMapping getTargetColumnType() {
		return null;
	}

	@Override
	public String getResultAlias(String tableAlias, DatabaseDialect profile) {
		return StringUtils.upperCase(alias);
	}
}
