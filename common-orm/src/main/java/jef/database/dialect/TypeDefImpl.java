package jef.database.dialect;

import java.util.Map;

import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.UnknownStringMapping;
import jef.database.query.SqlExpression;
import jef.tools.reflect.BeanUtils;


/**
 * 一个数据库类型的定义，将逐渐此类代替原来ColumnType中的子类。
 * @author jiyi
 *
 */
public class TypeDefImpl extends ColumnType implements SqlTypeSized {
	/**
	 * 类型的名称  数据库中的SQL名称。可以为null。
	 * 为null时根据sqlType从方言自动生成
	 */
	private String name;
	/**
	 * JDBC类型
	 */
	private int sqlType;
	/**
	 * 长度
	 */
	private int length;
	/**
	 * 长度（数字）
	 */
	private int precision;
	/**
	 * 精度
	 */
	private int scale;
	/**
	 * 对应的java数据类型
	 */
	private Class<?> javaType = String.class;
	/**
	 * 映射规则类
	 */
	private Class<? extends ColumnMapping> mappingClz;

	/**
	 * 构造
	 * @param name
	 * @param sqlType
	 * @param length
	 * @param p
	 * @param s
	 */
	public TypeDefImpl(String name, int sqlType) {
		this.name = name;
		this.sqlType = sqlType;
	}

	/**
	 * 和数据库中的列进行比较
	 */
	@Override
	protected boolean compare(ColumnType type, DatabaseDialect profile) {
		return true;
	}
	
	@Override
	protected void putAnnonation(Map<String, Object> map) {
		if (!nullable)
			map.put("nullable", java.lang.Boolean.FALSE);
		map.put("columnDefinition", name);
	}

	@Override
	public Class<?> getDefaultJavaType() {
		return javaType;
	}

	@Override
	public ColumnMapping getMappingType(Class<?> fieldType) {
		if (mappingClz != null) {
			return BeanUtils.newInstance(mappingClz);
		} else if (fieldType == String.class) {
			return new UnknownStringMapping(name, sqlType);
		} else {
			throw new UnsupportedOperationException("can not support mapping from [" + fieldType.getName() + " -> " + name + "]");
		}
	}

	public String getName() {
		return name;
	}

	public int getSqlType() {
		return sqlType;
	}

	public int getLength() {
		return length;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public TypeDefImpl setName(String name) {
		this.name = name;
		return this;
	}

	public TypeDefImpl setSqlType(int sqlType) {
		this.sqlType = sqlType;
		return this;
	}

	public TypeDefImpl setLength(int length) {
		this.length = length;
		return this;
	}

	public TypeDefImpl setPrecision(int precision) {
		this.precision = precision;
		return this;
	}

	public TypeDefImpl setScale(int scale) {
		this.scale = scale;
		return this;
	}
	
	public TypeDefImpl javaType(Class<?> javaType) {
		this.javaType=javaType;
		return this;
	}

	public TypeDefImpl mapping(Class<? extends ColumnMapping> mappingClz) {
		this.mappingClz = mappingClz;
		return this;
	}
	
	public TypeDefImpl spec(int length,int p,int s){
		this.length=length;
		this.precision=p;
		this.scale=s;
		return this;
	}

	@Override
	protected void setDefaultByString(String defaultStr) {
		this.defaultValue=new SqlExpression(defaultStr);
	}
}
