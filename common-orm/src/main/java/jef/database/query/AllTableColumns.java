package jef.database.query;

import java.util.IdentityHashMap;
import java.util.Map;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AliasProvider;
import jef.database.meta.IReferenceAllTable;
import jef.database.meta.ISelectProvider;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

import  org.apache.commons.lang3.RandomStringUtils;

public final class AllTableColumns implements IReferenceAllTable {

	/**
	 * 表中列的别名规则
	 */
	public enum AliasMode {
		/**
		 * SQL语句中的列别名：t.* 省略模式
		 */
		OMIT,
		/**
		 * SQL语句中的列别名：直接用列名，不修改
		 */
		RAWNAME,
		/**
		 * SQL语句中的列别名：缺省，即 T1__XXXXX的模式
		 */
		DEFAULT,
		/**
		 * SQL语句中的列别名：使用随机数做别名
		 */
		RANDOM,
		/**
		 * SQL语句中的列别名：将COlUMN名称转换为默认的JavaField名称
		 */
		JAVA_NAME
	}

	private String name; // 要装配到的字段，默认没有的情况下装配到根对象上
	private Query<?> table;// 对应的表
	private AliasMode aliasType;
	private ITableMetadata customType;
	private final Map<Field, String> aliasMap = new IdentityHashMap<Field, String>(16);// 别名索引

	private int projection; // 只有 count 和 normal两种
	private String countAlias; // 只有当count时才有用.描述count后的别名
	private boolean lazyLob;

	/**
	 * 设置查询列为count(*) as alias
	 * 
	 * @param alias
	 * @return
	 */
	public AllTableColumns countAs(String alias) {
		this.projection = PROJECTION_COUNT;
		this.countAlias = alias;
		return this;
	}

	public void setLazyLob(boolean lazyLob) {
		if (lazyLob) {
			if (table.getMeta().getLobFieldNames() == null) {// 如果没有LOB字段，不能设置为true
				return;
			}
		}
		this.lazyLob = lazyLob;
	}

	public boolean isLazyLob() {
		return lazyLob;
	}

	/**
	 * 禁止从数据库中查询rownum / rowid等虚拟列
	 * 
	 * @return
	 */
	public AllTableColumns noVirtualColumn() {
		this.projection = PROJECTION_NORMAL_NOVIRTUAL;
		return this;
	}

	/**
	 * 获取当count时的count列的别名
	 * 
	 * @return
	 */
	protected String getCountAlias() {
		return countAlias;
	}

	private String getAlias(Field f) {
		return aliasMap.get(f);
	}

	AllTableColumns(Query<?> table) {
		this.table = table;
		this.aliasType = ORMConfig.getInstance().isSpecifyAllColumnName() ? AliasMode.RAWNAME : AliasMode.OMIT;
	}

	/**
	 * 获取别名的方案
	 * 
	 * @return
	 */
	public AliasMode getAliasType() {
		return aliasType;
	}

	/**
	 * 设置在select语句中对列名的描述，可取 <li>ALIAS_MODE_SIMPLE</li> //SQL语句中的列别名：t.* 省略模式
	 * <li>ALIAS_MODE_RAWNAME</li> //SQL语句中的列别名：直接用列名，不修改 <li>ALIAS_MODE_DEFAULT
	 * </li> //SQL语句中的列别名：缺省，即 T1__XXXXX的模式 <li>ALIAS_MODE_RANDOM</li>;
	 * //SQL语句中的列别名：使用随机数做别名
	 * 
	 * @param aliasType
	 */
	public void setAliasType(AliasMode aliasType) {
		this.aliasType = aliasType;
	}

	/**
	 * 设置这个查询不要选择任何列出来
	 */
	protected void notSelectAnyColumn() {
		this.projection = ISelectProvider.PROJECTION_NOT_SELECT;
	}

	public String getSelectedAliasOf(ColumnMapping f, DatabaseDialect profile, String schema) {
		String value;
		Field field = f.field();
		switch (aliasType) {
		case OMIT:
			// 这个地方不可抛出异常.因为对于AllColumn来说，还是有可能位于join当中的。
			// 此外，如果LOB字段延迟加载，也会出现OMIT参数无效的情况
			// throw new
			// IllegalArgumentException("the type should use 't.*' on select columns. ");
		case RAWNAME:
			value = f.getColumnName(profile, false);
			aliasMap.put(field, value);
			return null;// 无需转义
		case DEFAULT: {
			String alias = getAlias(field);
			if (alias != null)
				return alias;
			value = AliasProvider.DEFAULT.getSelectedAliasOf(f, profile, schema);
			aliasMap.put(field, value);
			return value;
		}
		case RANDOM: {
			String alias = getAlias(field);
			if (alias != null)
				return alias;
			value = "C".concat(RandomStringUtils.randomNumeric(12));
			aliasMap.put(field, value);
			return value;
		}
		case JAVA_NAME: {
			String alias = getAlias(f.field());
			if (alias != null)
				return DbUtils.escapeColumn(profile, alias);
			value = field.name();
			aliasMap.put(field, value);
			return value;
		}
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public String getResultAliasOf(ColumnMapping f, String schema) {
		String alias = getAlias(f.field());
		if (alias != null)
			return alias.toUpperCase();
		return f.upperColumnName();
	}

	protected void setName(String name) {
		this.name = name;
	}

	/**
	 * 可以设置一个自定义的结果拼装类型
	 * 
	 * @param customType
	 */
	public void setCustomType(ITableMetadata customType) {
		this.customType = customType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.meta.IReferenceField#getFullModeTargetType()
	 */
	public ITableMetadata getFullModeTargetType() {
		return customType == null ? MetaHolder.getMeta(table.getInstance()) : customType;
	}

	public String getName() {
		return name;
	}

	public int getProjection() {
		return projection;
	}

	/**
	 * 内部使用获取，返回以非每列显式指定的SQL select子句项。 即如果使用了PROJECTION_COUNT，或者使用了省略模式(*)，则返回值
	 * 否则返回null (表示要显式指定每个列)
	 */
	public String simpleModeSql(String tableAlias) {
		if (lazyLob) {
			return null;
		}
		// TODO distinct的情况下的处理
		if ((projection & PROJECTION_COUNT) > 0) {
			return "count(" + tableAlias + ".*)";
		}
		if (aliasType == AliasMode.OMIT) {
			return tableAlias.concat(".*");
		}
		return null;
	}

	public boolean isSingleColumn() {
		return false;
	}
}
