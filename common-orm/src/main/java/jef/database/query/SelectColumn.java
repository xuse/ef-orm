package jef.database.query;

import java.util.ArrayList;
import java.util.List;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbFunction;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.HavingEle;

public final class SelectColumn extends SingleColumnSelect {
	private String populateTo;// 拼装路径，默认应该和alias一致
	private String alias;
	private Field targetField;
	private ITableMetadata meta;
	protected List<DbFunctionCall> func; // 附加的函数

	// 缓存一个不带别名的列，以提高速度
	private String columnSimpleName;// 不带别名的参数

	/**
	 * 转换为Having子句
	 */
	public HavingEle toHavingClause(DatabaseDialect profile, String tableAlias, SqlContext context) {
		HavingEle h = new HavingEle();
		String column = innerGetColumn(profile, tableAlias);
		h.column = column;
		h.sql = Condition.toSql(column, havingCondOperator, havingCondValue, profile, null, null);
		h.havingCondOperator = this.havingCondOperator;
		h.havingCondValue = this.havingCondValue;
		return h;
	}

	/**
	 * 设置having子句条件
	 * 
	 * @param oper
	 * @param value
	 * @return
	 */
	public SelectColumn having(Operator oper, Object value) {
		this.projection = projection | PROJECTION_HAVING;
		this.havingCondOperator = oper;
		this.havingCondValue = value;
		return this;
	}

	/**
	 * 设置having子句条件（该表达式不作为被选择列）。默认情况下 .column().sum().having(">",100);时，
	 * column().sum()也会作为查询列之一被查出。 使用havingOnly方法添加的having则是纯作为条件，不会作为查询列。
	 * 
	 * @param oper
	 * @param value
	 * @return
	 */
	public SelectColumn havingOnly(Operator oper, Object value) {
		this.projection = projection | PROJECTION_HAVING_NOT_SELECT;
		this.havingCondOperator = oper;
		this.havingCondValue = value;
		return this;
	}

	/**
	 * count(xxx)
	 * 
	 * @return
	 */
	public SelectColumn count() {
		this.projection = (projection & 0xFF00) | PROJECTION_COUNT;
		return this;
	}

	/**
	 * 求和
	 * 
	 * @return
	 */
	public SelectColumn sum() {
		this.projection = (projection & 0xFF00) | PROJECTION_SUM;
		return this;
	}

	/**
	 * 求平均数
	 * 
	 * @return
	 */
	public SelectColumn avg() {
		this.projection = (projection & 0xFF00) | PROJECTION_AVG;
		return this;
	}

	/**
	 * 取最大值
	 * 
	 * @return
	 */
	public SelectColumn max() {
		this.projection = (projection & 0xFF00) | PROJECTION_MAX;
		return this;
	}

	/**
	 * 取最小值
	 * 
	 * @return
	 */
	public SelectColumn min() {
		this.projection = (projection & 0xFF00) | PROJECTION_MIN;
		return this;
	}

	/**
	 * 对应count(distinct xx)
	 * 
	 * @return
	 */
	public SelectColumn countDistinct() {
		this.projection = (projection & 0xFF00) | PROJECTION_COUNT_DISTINCT;
		return this;
	}

	/**
	 * 指定按照此列进行 group by操作，同时选出此列
	 * 
	 * @return
	 */
	public SelectColumn group() {
		this.projection = (projection & 0xFF00) | PROJECTION_GROUP;
		return this;
	}

	/**
	 * 将此列的定义添加到一个表达式中。
	 * 在表达式中可以使用 ? 占位符表示当前的列。
	 * 
	 * @param template 表达式
	 * @return
	 */
	public SelectColumn expression(String template) {
		this.projection = projection | PROJECTION_CUST_FUNC;
		if (this.func == null) {
			this.func = new ArrayList<DbFunctionCall>();
		}
		this.func.add(new DbFunctionCall(template));
		return this;
	}

	/**
	 * 指定按此列进行函数计算
	 * 
	 * @param func
	 *            指定的函数
	 * @param params
	 *            函数的参数
	 * @return
	 */
	public SelectColumn func(DbFunction func, String... params) {
		this.projection = projection | PROJECTION_CUST_FUNC;
		if (this.func == null) {
			this.func = new ArrayList<DbFunctionCall>();
		}
		this.func.add(new DbFunctionCall(func, params));
		return this;
	}

	SelectColumn(Field field, String populateTo) {
		this.targetField = field;
		if (populateTo == null)
			populateTo = field.name();
		this.populateTo = populateTo;
		this.meta = DbUtils.getTableMeta(field);
	}

	void setProjection(int projection) {
		this.projection = projection;
	}

	/**
	 * 将Projection重置
	 */
	public void clearProjection() {
		this.projection = PROJECTION_NORMAL;
		this.func = null;
	}

	/**
	 * 指定别名。这个方法不仅仅指定该查询列的别名，还指定该列将写入到指定对象字段中去
	 * 
	 * @param alias
	 * @return
	 */
	public SelectColumn as(String alias) {
		this.alias = alias;
		if (alias != null) {
			this.populateTo = alias;
		}
		return this;
	}

	public SelectColumn toField(String property) {
		this.populateTo = property;
		return this;
	}

	public String getName() {
		return populateTo;
	}

	// 生成选择语句时生成列名别，null表示无别名
	public String getSelectedAlias(String tableAlias, DatabaseDialect profile) {
		return DbUtils.escapeColumn(profile, profile.getObjectNameToUse(alias));
	}

	@Override
	public String getResultAlias(String tableAlias, DatabaseDialect profile) {
		if (alias == null) {
			if (columnSimpleName == null) {
				throw new IllegalArgumentException();
			}
			return columnSimpleName.toUpperCase();
		} else {
			return alias.toUpperCase();
		}
	}

	private String innerGetColumn(DatabaseDialect profile, String tableAlias) {
		// 因为第一次操作列名改为额大写，造成第二次Parse出错
		columnSimpleName = DbUtils.toColumnName(targetField, profile, null);// 不带别名的列名

		String name = DbUtils.toColumnName(targetField, profile, tableAlias);
		return applyProjection(name, this.func, profile);
	}

	// 当生成选择语句时计算列名称
	public String getSelectItem(DatabaseDialect profile, String tableAlias, SqlContext context) {
		if (targetField == null)
			return null;
		if ((projection & PROJECTION_HAVING_NOT_SELECT) > 0) {
			return null;// 纯having的列，不进行select,也不进行group
		}
		if (targetField instanceof JpqlExpression) {
			return columnSimpleName = ((JpqlExpression) targetField).toSqlAndBindAttribs(context, profile);
		}
		return innerGetColumn(profile, tableAlias);
	}

	public boolean isSingleColumn() {
		return true;
	}

	public ColumnMapping getTargetColumnType() {
		return meta.getColumnDef(targetField);
	}

	@Override
	public String toString() {
		return populateTo + ":" + targetField + (alias == null ? "" : " as " + alias);
	}
}
