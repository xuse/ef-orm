/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.dialect;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import jef.database.DbCfg;
import jef.database.annotation.HiloGeneration;
import jef.database.dialect.type.AutoGuidMapping;
import jef.database.dialect.type.AutoIntMapping;
import jef.database.dialect.type.AutoLongMapping;
import jef.database.dialect.type.AutoStringMapping;
import jef.database.dialect.type.BlobByteArrayMapping;
import jef.database.dialect.type.BlobFileMapping;
import jef.database.dialect.type.BlobObjectMapping;
import jef.database.dialect.type.BlobStringMapping;
import jef.database.dialect.type.CharBooleanMapping;
import jef.database.dialect.type.CharCharMapping;
import jef.database.dialect.type.CharDateMapping;
import jef.database.dialect.type.CharEnumMapping;
import jef.database.dialect.type.CharStringMapping;
import jef.database.dialect.type.CharTimestampMapping;
import jef.database.dialect.type.ClobCharArrayMapping;
import jef.database.dialect.type.ClobFileMapping;
import jef.database.dialect.type.ClobStringMapping;
import jef.database.dialect.type.DateDateMapping;
import jef.database.dialect.type.DateSDateMapping;
import jef.database.dialect.type.DateStringMapping;
import jef.database.dialect.type.DelegatorBoolean;
import jef.database.dialect.type.MappingType;
import jef.database.dialect.type.NumBigDateMapping;
import jef.database.dialect.type.NumBigIntMapping;
import jef.database.dialect.type.NumBigLongMapping;
import jef.database.dialect.type.NumBigStringMapping;
import jef.database.dialect.type.NumDoubleDoubleMapping;
import jef.database.dialect.type.NumDoubleFloatMapping;
import jef.database.dialect.type.NumDoubleStringMapping;
import jef.database.dialect.type.NumFloatDoubleMapping;
import jef.database.dialect.type.NumFloatMapping;
import jef.database.dialect.type.NumIntBooleanMapping;
import jef.database.dialect.type.NumIntIntMapping;
import jef.database.dialect.type.NumIntLongMapping;
import jef.database.dialect.type.NumIntStringMapping;
import jef.database.dialect.type.TimestampDateMapping;
import jef.database.dialect.type.TimestampLongMapping;
import jef.database.dialect.type.TimestampTsMapping;
import jef.database.dialect.type.UnknownStringMapping;
import jef.database.dialect.type.VarcharDateMapping;
import jef.database.dialect.type.VarcharEnumMapping;
import jef.database.dialect.type.VarcharFloatMapping;
import jef.database.dialect.type.VarcharIntMapping;
import jef.database.dialect.type.VarcharStringMapping;
import jef.database.dialect.type.VarcharTimestampMapping;
import jef.database.dialect.type.XmlStringMapping;
import jef.database.meta.Column;
import jef.database.meta.ColumnChange;
import jef.database.meta.ColumnChange.Change;
import jef.database.meta.Feature;
import jef.database.support.RDBMS;
import jef.tools.Assert;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

/**
 * 描述一个数据库列的类型
 * 
 * @author Administrator
 * 
 */
public abstract class ColumnType {
	private static boolean IS_NATIVE_AUTO = JefConfiguration.getBoolean(DbCfg.DB_AUTOINCREMENT_NATIVE, true);

	protected boolean nullable = true;
	protected Object defaultValue;

	public String toString() {
		Map<String,Object> map=toJpaAnnonation();
		return String.valueOf(map.get("columnDefinition"));
	}

	/**
	 * 返回字段是否可为空
	 * @return
	 */
	public boolean isNullable() {
		return nullable;
	}

	/**
	 * 指定字段为空或者可空
	 * @param nullAble
	 * @return
	 */
	public ColumnType setNullable(boolean nullAble) {
		this.nullable = nullAble;
		return this;
	}

	/**
	 * 指定某个数据库字段为非空
	 * 
	 * @return
	 */
	public ColumnType notNull() {
		this.nullable = false;
		return this;
	}

	/**
	 * 为某个数据库字段指定缺省值
	 * 
	 * @param obj
	 * @return
	 */
	public ColumnType defaultIs(Object obj) {
		this.defaultValue = obj;
		return this;
	}

	/**
	 * 当代码生成时，转换为需要加注在属性上的Annotation
	 * @return
	 */
	public Map<String, Object> toJpaAnnonation() {
		Map<String, Object> map = new HashMap<String, Object>();
		putAnnonation(map);
		return map;
	}

	/**
	 * 设置列的缺省值
	 * @param obj
	 */
	public void setDefault(Object obj) {
		this.defaultValue = obj;
	}

	/**
	 * 比较列定义， 一样就返回true
	 * 
	 * @param c
	 * @param profile
	 * @return
	 */
	public List<ColumnChange> isEqualTo(Column c, DatabaseDialect profile) {
		ColumnType oldType = toNormal(c.toColumnType(profile));
		ColumnType newType = toNormal(this);
		List<ColumnChange> result = new ArrayList<ColumnChange>();

		// 对自增类型的数据不检查缺省值(兼容PG)
		if (!(this instanceof AutoIncrement)) {
			// 检查缺省值
			String a1 = profile.toDefaultString(oldType.defaultValue);
			String a2 = profile.toDefaultString(newType.defaultValue);
			// 非字符串比较情况下全部按小写处理
			if (a1 != null && !a1.startsWith("'")) {
				a1 = StringUtils.lowerCase(a1);
			}
			if (a2 != null && !a2.startsWith("'")) {
				a2 = StringUtils.lowerCase(a2);
			}
			if (!StringUtils.equals(a1, a2)) {
				ColumnChange chg;
				if (StringUtils.isEmpty(a2)) {
					chg = new ColumnChange(Change.CHG_DROP_DEFAULT);
				} else {
					chg = new ColumnChange(Change.CHG_DEFAULT);
				}
				chg.setFrom(a1);
				chg.setTo(a2);
				result.add(chg);
			}
		}

		// 针对NUll特性检查
		if (oldType.nullable != this.nullable) {
			if (this.nullable) {
				result.add(new ColumnChange(Change.CHG_TO_NULL));
			} else {
				result.add(new ColumnChange(Change.CHG_TO_NOT_NULL));
			}
		}

		// 再检查数据类型
		if (this.getClass() == Boolean.class) {// 长度为1的字符或数字都算匹配
			if (c.getColumnSize() == 1) {
				return result;// 不用再比了。
			}
		}
		if (profile.getName() == RDBMS.oracle) {// 很特殊的情况,Oracle下不映射其Timestamp类型，因此Oracle的Date和TimeStamp即被认为是等效的
			if (oldType.getClass() == Date.class || oldType.getClass() == TimeStamp.class) {
				if (newType.getClass() == Date.class || newType.getClass() == TimeStamp.class) {
					return result;// 不用再比了，认为数据类型一样
				}
			}
		}
		if (oldType.getClass() == newType.getClass()) {
			if (!newType.compare(oldType, profile)) {
				ColumnChange cg = createChange(oldType, newType, profile);
				if (cg != null)
					result.add(cg);
			}
		} else {
			ColumnChange cg = createChange(oldType, newType, profile);
			if (cg != null)
				result.add(cg);
		}
		return result;
	}

	/**
	 * 针对几个非常规类型才进行的比较，目的是比较其长度等定义，最终返回变化
	 * @param type
	 * @param profile
	 * @return
	 */
	protected abstract boolean compare(ColumnType type, DatabaseDialect profile);
	/**
	 * 生成Annotation
	 * @param map
	 */
	protected abstract void putAnnonation(Map<String, Object> map);
	/**
	 * 返回缺省的Java数据类型
	 * @return
	 */
	public abstract Class<?> getDefaultJavaType();
	/**
	 * 生成到特定字段类型的映射对象
	 * @param fieldType
	 * @return
	 */
	public abstract MappingType<?> getMappingType(Class<?> fieldType);

	public final static class Char extends ColumnType {
		protected int length;

		public Char(int length) {
			this.length = length;
		}

		public int getLength() {
			return length;
		}

		public Class<?> getDefaultJavaType() {
			return java.lang.String.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			String def = "char(" + length + ")";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			map.put("length", length);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			Char rhs = (Char) type;
			return rhs.length == this.length;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == String.class || fieldType == Object.class) {
				return new CharStringMapping();
			} else if (fieldType == Character.class || fieldType == Character.TYPE) {
				return new CharCharMapping();
			} else if (fieldType.isEnum()) {
				return new CharEnumMapping(fieldType.asSubclass(Enum.class));
			} else if (fieldType == java.util.Date.class) {
				return new CharDateMapping();
			} else if (fieldType == java.sql.Timestamp.class) {
				return new CharTimestampMapping();
			} else if (length == 1 && (fieldType == java.lang.Boolean.class || fieldType == java.lang.Boolean.TYPE)) {
				return new CharBooleanMapping();
			}
			throw new IllegalArgumentException("Char can not mapping to class " + fieldType.getName());
		}

	}

	public static final class Varchar extends ColumnType {
		protected int length;

		public Varchar(int length) {
			this.length = length;
		}

		public int getLength() {
			return length;
		}

		@Override
		public Class<?> getDefaultJavaType() {
			return java.lang.String.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			String def = "varchar(" + length + ")";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			map.put("length", length);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			Varchar rhs = (Varchar) type;
			return rhs.length == this.length;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == String.class || fieldType == Object.class) {
				return new VarcharStringMapping();
			} else if (fieldType.isEnum()) {
				return new VarcharEnumMapping(fieldType.asSubclass(Enum.class));
			} else if (fieldType == Integer.class || fieldType == Integer.TYPE) {
				return new VarcharIntMapping();
			} else if (fieldType == java.util.Date.class) {
				return new VarcharDateMapping();
			} else if (fieldType == java.sql.Timestamp.class) {
				return new VarcharTimestampMapping();
			} else if (fieldType == Float.class || fieldType == Float.TYPE) {
				return new VarcharFloatMapping();
			}
			throw new IllegalArgumentException("Varchar can not mapping to class " + fieldType.getName());
		}
	}

	/**
	 * 对应Java类型： Boolean/boolean 对应数据库类型: Oracle: number(1) mySql: text Derby:
	 * varchar2 sql server:不支持 其他：不支持
	 */
	public static final class Boolean extends ColumnType {
		@Override
		public Class<?> getDefaultJavaType() {
			return java.lang.Boolean.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			String def = "boolean";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			return true;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if(fieldType == java.lang.Boolean.class || fieldType == java.lang.Boolean.TYPE || fieldType == Object.class){
				return new DelegatorBoolean();
			}else{
				throw new UnsupportedOperationException("can not support mapping from ["+fieldType.getName()+" -> boolean]");
			}
		}
	}

	public static final class Double extends ColumnType {
		int precision = 16;
		int scale = 6;

		public Double(int precision, int scale) {
			if (precision > 0)
				this.precision = precision;
			if (scale > 0)
				this.scale = scale;
		}

		public Class<?> getDefaultJavaType() {
			if (precision >= 12 || precision + scale >= 16) {
				return java.lang.Double.class;
			} else {
				return java.lang.Float.class;
			}
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			String def = "number(" + precision + "," + scale + ")";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			map.put("precision", precision);
			map.put("scale", scale);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			Double rhs = (Double) type;
			if (profile.getName() == RDBMS.oracle) {
				return rhs.precision == this.precision && rhs.scale == this.scale;
			} else {
				int p1 = Math.abs(rhs.precision - this.precision);
				int p2 = Math.abs(rhs.scale - this.scale);
				return p1 < 3 && p2 < 3;
			}
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			boolean isBig = (precision >= 12 || precision + scale >= 16);
			if (isBig) {
				if (fieldType == java.lang.Double.class || fieldType == java.lang.Double.TYPE || fieldType == Object.class) {
					return new NumDoubleDoubleMapping();
				} else if (fieldType == Float.class || fieldType == Float.TYPE) {
					return new NumDoubleFloatMapping();
				} else if (fieldType == String.class) {
					return new NumDoubleStringMapping();
				}
			} else {
				if (fieldType == java.lang.Double.class || fieldType == java.lang.Double.TYPE) {
					return new NumFloatDoubleMapping();
				} else if (fieldType == Float.class || fieldType == Float.TYPE || fieldType == Object.class) {
					return new NumFloatMapping();
				}
			}
			throw new IllegalArgumentException("Double can not mapping to class " + fieldType.getName());
		}
	}

	// Int 和BigInt，BigInt对应Long,默认10
	public static final class Int extends ColumnType {
		int precision = 8;

		public Int(int precision) {
			if (precision > 0) {
				this.precision = precision;
			}
			if (precision > 100) {
				throw new IllegalArgumentException("A type of number(" + precision + ") is too big for database.");
			}
		}

		public Class<?> getDefaultJavaType() {
			if (precision > 10) {
				return nullable ? java.lang.Long.class : java.lang.Long.TYPE;
			} else {
				return nullable ? java.lang.Integer.class : java.lang.Integer.TYPE;
			}
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			String def = "number(" + precision + ")";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
			map.put("precision", precision);
		}

		public void setPrecision(int p) {
			this.precision = p;
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			Int rhs = (Int) type;
			if (profile.getName() == RDBMS.oracle) {
				return rhs.precision == this.precision;
			} else {
				return Math.abs(rhs.precision - this.precision) < 3;// 轻微长度误差不管
			}
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == java.lang.Boolean.class || fieldType == java.lang.Boolean.TYPE) {
				return new NumIntBooleanMapping();
			}
			boolean isBig = precision > 10;
			if (isBig) {
				if (fieldType == Long.class || fieldType == Long.TYPE || fieldType == Object.class) {
					return new NumBigLongMapping();
				} else if (fieldType == Integer.class || fieldType == Integer.TYPE) {
					return new NumBigIntMapping();
				} else if (fieldType == java.util.Date.class) {
					return new NumBigDateMapping();
				} else if (fieldType == String.class) {
					return new NumBigStringMapping();
				}
			} else {
				if (fieldType == Long.class || fieldType == Long.TYPE) {
					return new NumIntLongMapping();
				} else if (fieldType == Integer.class || fieldType == Integer.TYPE || fieldType == Object.class) {
					return new NumIntIntMapping();
				} else if (fieldType == String.class) {
					return new NumIntStringMapping();
				} else if (fieldType == java.util.Date.class) {
					return new NumBigDateMapping();
				}
			}
			throw new IllegalArgumentException("Int can not mapping to class " + fieldType.getName());
		}
	}

	public static final class Date extends ColumnType {
		//0 不自动生成 1 创建时生成为sysdate 2更新时生成为sysdate 3创建时设置为为java系统时间  4为更新时设置为java系统时间
		private int generateType;

		public int getGenerateType() {
			return generateType;
		}

		public void setGenerateType(int generateType) {
			this.generateType = generateType;
		}

		public Class<?> getDefaultJavaType() {
			return java.util.Date.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			String def = "date";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			return true;
		}

		public TimeStamp toTimeStamp() {
			TimeStamp t = new TimeStamp();
			t.defaultValue = this.defaultValue;
			t.nullable = this.nullable;
			t.generateType = this.generateType;
			return t;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == java.sql.Date.class) {
				return new DateSDateMapping();
			} else if (fieldType == java.util.Date.class || fieldType == Object.class) {
				return new DateDateMapping();
			} else if (fieldType == String.class) {
				return new DateStringMapping();
			}
			throw new IllegalArgumentException("Date can not mapping to class " + fieldType.getName());
		}
	}

	public static final class TimeStamp extends ColumnType {
		//0 不自动生成 1 创建时生成为sysdate 2更新时生成为sysdate 3创建时设置为为java系统时间  4为更新时设置为java系统时间
		private int generateType;

		public int getGenerateType() {
			return generateType;
		}

		public void setGenerateType(int generated) {
			this.generateType = generated;
		}

		public Class<?> getDefaultJavaType() {
			return java.util.Date.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			String def = "timestamp";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			return true;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == java.sql.Timestamp.class) {
				return new TimestampTsMapping();
			} else if (fieldType == java.util.Date.class || fieldType == Object.class) {
				return new TimestampDateMapping();
			} else if (fieldType == java.lang.Long.class || fieldType == java.lang.Long.TYPE) {
				return new TimestampLongMapping();
			}
			throw new IllegalArgumentException("TimeStamp can not mapping to class " + fieldType.getName());
		}
	}

	/**
	 * 对应Java数据类型:int/Integer 对应数据库类型：oracle:Clob mySql: text Derby: varchar2
	 * sql server:不支持 其他：不支持 其他：自增
	 * 
	 * @author Administrator
	 */
	public static final class AutoIncrement extends ColumnType {
		int length = 10;
		private GenerationType type;
		private TableGenerator tableGenerator;
		private SequenceGenerator seqGenerator;
		private HiloGeneration hilo;

		public TableGenerator getTableGenerator() {
			return tableGenerator;
		}

		public SequenceGenerator getSeqGenerator() {
			return seqGenerator;
		}

		public GenerationType getType() {
			return type;
		}

		public int getLength() {
			return length;
		}

		public Int toNormalType() {
			Int i = new ColumnType.Int(length);
			i.notNull();
			return i;
		}

		public AutoIncrement(int i) {
			this(i, GenerationType.AUTO, null, null, null);
		}

		public HiloGeneration getHiloConfig() {
			return hilo;
		}

		public AutoIncrement(int i, GenerationType type, TableGenerator tg, SequenceGenerator sg, HiloGeneration hilo) {
			if (i > 0)
				this.length = i;
			this.nullable = false;
			if (IS_NATIVE_AUTO) {
				if (type == GenerationType.IDENTITY || type == GenerationType.SEQUENCE) {
					type = GenerationType.AUTO;
				}
			}
			this.type = type;
			this.tableGenerator = tg;
			this.seqGenerator = sg;
		}

		public Class<?> getDefaultJavaType() {
			return length > 10 ? java.lang.Long.TYPE : java.lang.Integer.TYPE;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			map.put("columnDefinition", "number(" + length + ")");
			map.put("nullable", java.lang.Boolean.FALSE);
			map.put("precision", length);
			map.put("@Id", null);
			map.put("@GeneratedValue", "strategy=GenerationType.SEQUENCE");
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == Integer.class || fieldType == Integer.TYPE) {
				return new AutoIntMapping();
			} else if (fieldType == Long.class || fieldType == Long.TYPE) {
				return new AutoLongMapping();
			} else if (fieldType == String.class) {
				return new AutoStringMapping();
			} else if (fieldType == Object.class) {
				return length > 10 ? new AutoLongMapping() : new AutoIntMapping();

			}
			throw new IllegalArgumentException("AutoIncrement can not mapping to class " + fieldType.getName());
		}

		public GenerationType getGenerationType(DatabaseDialect profile, boolean allowIdentity) {
			boolean supportSequence=profile.has(Feature.SUPPORT_SEQUENCE);
			//如果配置为自动的
			if (type == GenerationType.AUTO) {
				if (allowIdentity) {
					if (profile.notHas(Feature.AUTOINCREMENT_NEED_SEQUENCE))
						return GenerationType.IDENTITY;
				}
				if (supportSequence)
					return GenerationType.SEQUENCE;
				return GenerationType.TABLE;
			}
			//配置为自增，但不允许自增的情况
			if (type == GenerationType.IDENTITY && !allowIdentity) {
				return supportSequence? GenerationType.SEQUENCE : GenerationType.TABLE;
			}
			//配置为序列，但不支持序列时
			if(type==GenerationType.SEQUENCE && !supportSequence){
				return allowIdentity?GenerationType.IDENTITY:GenerationType.TABLE;
			}
			return type;
		}
	}

	/**
	 * 对应Java数据类型:String 对应数据库类型：oracle:Clob mySql: text Derby: varchar2 sql
	 * server:不支持 其他：不支持 其他：第一次插入时自动生成
	 */
	public static final class GUID extends ColumnType {
		private boolean removeDash;

		public boolean isRemoveDash() {
			return removeDash;
		}

		public void setRemoveDash(boolean removeDash) {
			this.removeDash = removeDash;
		}

		public ColumnType toNormalType() {
			return new ColumnType.Varchar(36).notNull();
		}

		public Class<?> getDefaultJavaType() {
			return java.lang.String.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			String def = "varchar(36)";
			if (defaultValue != null) {
				def = def + " default " + String.valueOf(defaultValue);
			}
			map.put("columnDefinition", def);
			map.put("length", 36);
			map.put("nullable", java.lang.Boolean.FALSE);
			map.put("@Id", null);
			map.put("@GeneratedValue", "strategy=GenerationType.IDENTITY");
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (!CharSequence.class.isAssignableFrom(fieldType)) {
				throw new IllegalArgumentException();
			}
			return new AutoGuidMapping();
		}
	}

	/**
	 * 对应Java数据类型：String/char[]/File/CharBuffer/BigDataBuffer
	 * 对应数据库类型：oracle:Clob mySql: text Derby: varchar2 sql server:不支持 其他：不支持
	 * 
	 * @author Administrator
	 * 
	 */
	public static class Clob extends ColumnType {
		public Class<?> getDefaultJavaType() {
			return java.lang.String.class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			map.put("columnDefinition", "clob");
			map.put("@Lob", null);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			return true;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == char[].class) {
				return new ClobCharArrayMapping();
			} else if (fieldType == File.class) {
				return new ClobFileMapping();
			} else if (fieldType == String.class) {
				return new ClobStringMapping();
			} else if (fieldType == Object.class) {
				return new ClobStringMapping();
			}
			throw new IllegalArgumentException("Clob can not mapping to class " + fieldType.getName());
		}
	}

	/**
	 * 对应Java数据类型:String/char[]/byte[]/File/Image/BigDataBuffer
	 * 对应数据库类型：oracle:Clob mySql: text Derby: varchar2 sql server:不支持 其他：不支持
	 * 
	 * @author Administrator
	 */
	public static class Blob extends ColumnType {
		public Class<?> getDefaultJavaType() {
			return byte[].class;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			map.put("columnDefinition", "blob");
			map.put("@Lob", null);
		}

		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			return true;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if (fieldType == byte[].class) {
				return new BlobByteArrayMapping();
			} else if (fieldType == File.class) {
				return new BlobFileMapping();
			} else if (fieldType == String.class) {
				return new BlobStringMapping();
			} else if (fieldType == Object.class) {
				return new BlobObjectMapping();
			}
			throw new IllegalArgumentException("Blob can not mapping to class " + fieldType.getName());
		}
	}
	public static class XML extends ColumnType {
		@Override
		protected boolean compare(ColumnType type, DatabaseDialect profile) {
			return true;
		}

		@Override
		protected void putAnnonation(Map<String, Object> map) {
			if (!nullable)
				map.put("nullable", java.lang.Boolean.FALSE);
			map.put("columnDefinition", "xml");
		}

		@Override
		public Class<?> getDefaultJavaType() {
			return String.class;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			return new XmlStringMapping();
		}
	}

	public static class Unknown extends ColumnType {
		private String name;
		public Unknown(String name){
			this.name=name;
		}
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
			return String.class;
		}

		@Override
		public MappingType<?> getMappingType(Class<?> fieldType) {
			if(fieldType==String.class){
				return new UnknownStringMapping(name);
			}else{
				throw new UnsupportedOperationException("can not support mapping from ["+fieldType.getName()+" -> "+name+"]");
			}
			
			
		}
	}
	
	// 这是比较用的，因此会有一些特殊逻辑
	static ColumnType toNormal(ColumnType type) {
		if (type instanceof AutoIncrement) {
			Int i = ((AutoIncrement) type).toNormalType();
//			i.precision = 8;// 退化时抹去精度差异.
			return i;
		} else if (type instanceof GUID) {
			return ((GUID) type).toNormalType();
		}
		return type;
	}

	static ColumnChange createChange(ColumnType oldType, ColumnType newType, DatabaseDialect profile) {
		ColumnChange change = new ColumnChange(Change.CHG_DATATYPE);
		change.setFrom(profile.getCreationComment(oldType, false));// 这里要注意不要生成带有null
																	// 和default
																	// 值的文字，即只单纯的类型
		change.setTo(profile.getCreationComment(newType, false));
		if (change.getFrom().equals(change.getTo()))
			return null;
		return change;
	}
}
