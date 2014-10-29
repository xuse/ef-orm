package jef.database.dialect.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

import jef.accelerator.bean.BeanAccessor;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.EntityType;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.tools.Assert;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.GenericUtils;
import jef.tools.reflect.Property;

public abstract class AColumnMapping<T> implements ColumnMapping<T> {
	/**
	 * 原始的ColumnName
	 */
	public String rawColumnName;
	protected transient String cachedEscapeColumnName;
	private transient String lowerColumnName;
	private transient String upperColumnName;

	protected ITableMetadata meta;
	private String fieldName;
	protected Field field;
	protected ColumnType ctype;
	protected Class<T> clz;
	private Class<?> primitiveClz;
	private boolean pk;
	protected transient DatabaseDialect bindedProfile;
	protected Property fieldAccessor;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AColumnMapping() {
		Type[] p =  GenericUtils.getTypeParameters(this.getClass(), ColumnMapping.class);
		if (p[0] instanceof Class) {
			this.clz = (Class<T>) p[0];
		} else if (p[0] instanceof GenericArrayType) {
			GenericArrayType at = (GenericArrayType) p[0];
			Type compType = at.getGenericComponentType();
			if (compType instanceof Class) {
				this.clz = (Class<T>) Array.newInstance((Class) compType, 0).getClass();
			}
		}
		this.primitiveClz = BeanUtils.toPrimitiveClass(this.clz);
		Assert.notNull(clz);
	}

	public boolean isPk() {
		return pk;
	}

	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		this.field = field;
		this.fieldName = field.name();
		this.rawColumnName = columnName;
		this.lowerColumnName = columnName.toLowerCase();
		this.upperColumnName = columnName.toUpperCase();
		this.meta = meta;
		this.ctype = type;

		BeanAccessor ba = meta.getBeanAccessor();
		if (meta.getType() != EntityType.TUPLE) {
			Assert.isTrue(meta.getAllFieldNames().contains(field.name()));
		}
		fieldAccessor = ba.getProperty(field.name());
		Assert.notNull(fieldAccessor, ba.toString() + field.toString());
	}

	public String fieldName() {
		return fieldName;
	}

	public String upperColumnName() {
		return upperColumnName;
	}

	public String lowerColumnName() {
		return lowerColumnName;
	}

	@Override
	public String rawColumnName() {
		return rawColumnName;
	}

	public Field field() {
		return field;
	}

	public ITableMetadata getMeta() {
		return meta;
	}

	public ColumnType get() {
		return ctype;
	}

	public Class<T> getFieldType() {
		Assert.notNull(clz);
		return clz;
	}

	public Class<?> getPrimitiveType() {
		return primitiveClz;
	}

	public String getColumnName(DatabaseDialect profile, boolean escape) {
		if (!escape) {
			return profile.getColumnNameToUse(this);
		}
		if (bindedProfile == profile) {
			return cachedEscapeColumnName;
		}
		String escapedColumn = DbUtils.escapeColumn(profile, profile.getColumnNameToUse(this));
		rebind(escapedColumn, profile);
		return escapedColumn;
	}

	protected void rebind(String escapedColumn, DatabaseDialect profile) {
		bindedProfile = profile;
		cachedEscapeColumnName = escapedColumn;
	}

	public void setPk(boolean b) {
		this.pk = b;
	}

	/**
	 * 用单引号包围字符串，并将其中的单引号按SQL转义
	 * 
	 * @param s
	 * @return
	 */
	public final static String wrapSqlStr(String s) {
		StringBuilder sb = new StringBuilder(s.length() + 16);
		sb.append('\'');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\'') {
				sb.append("''");
			} else {
				sb.append(c);
			}
		}
		sb.append('\'');
		return sb.toString();
	}

	public String getSqlStr(Object value, DatabaseDialect profile) {
		if (value == null) {
			return "null";
		} else if (value instanceof Expression) {
			return value.toString();
		}
		return getSqlExpression(value, profile);
	}

	/**
	 * @param value
	 *            不为null，不为Expression
	 */
	protected abstract String getSqlExpression(Object value, DatabaseDialect profile);

	public void processInsert(Object value, InsertSqlClause result, List<String> cStr, List<String> vStr, boolean smart, IQueryableEntity obj) throws SQLException {
		String columnName = getColumnName(result.profile, true);
		if (value == null) {
			if (result.profile.has(Feature.NOT_SUPPORT_KEYWORD_DEFAULT)) {// 必须使用默认的方法(即不插入)来描述缺省值
			} else {
				cStr.add(columnName);// 为空表示不指定，使用缺省值
				vStr.add("DEFAULT");
			}
			return;
		}
		if (smart && !obj.isUsed(field)) {
			return;
		}
		cStr.add(columnName);
		vStr.add(getSqlStr(value, result.profile));
	}

	public void processPreparedInsert(IQueryableEntity obj, List<String> cStr, List<String> vStr, InsertSqlClause result, boolean dynamic) throws SQLException {
		if (dynamic && !obj.isUsed(field)) {
			return;
		}
		String columnName = getColumnName(result.profile, true);
		cStr.add(columnName);
		vStr.add("?");
		result.addField(this);
	}

	public boolean isLob() {
		return false;
	}

	public boolean applyFor(int type) {
		return type == getSqlType();
	}

	public Property getFieldAccessor() {
		return fieldAccessor;

	}

	@Override
	public String toString() {
		return this.fieldName;
	}

}
