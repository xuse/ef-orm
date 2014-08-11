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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.rowset.CachedRowSet;

import jef.common.BigDataBuffer;
import jef.common.ByteBufferInputStream;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.DbCfg;
import jef.database.DbFunction;
import jef.database.DbMetaData;
import jef.database.OperateTarget;
import jef.database.datasource.DataSourceInfo;
import jef.database.dialect.ColumnType.Blob;
import jef.database.dialect.ColumnType.Clob;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.type.ATypeMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.FunctionMapping;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.SqlExpression;
import jef.database.query.function.SQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.DateFormats;
import jef.tools.DateUtils;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

/**
 * 数据库方言的抽象类
 * 
 * @author Administrator
 * 
 */
public abstract class DbmsProfile implements DatabaseDialect {
	public void init(OperateTarget asOperateTarget) {
	}

	private static final Map<String, DbmsProfile> ITEMS = new HashMap<String, DbmsProfile>();
	private static final List<FunctionMapping> DEFAULT_FUNCTIONS = new ArrayList<FunctionMapping>();
	private static Map<String, String> dialectMappings;

	protected final Set<String> keywords = new HashSet<String>();

	public static DbmsProfile getProfile(String dbmsName) {
		dbmsName = dbmsName.toLowerCase();
		DbmsProfile profile = ITEMS.get(dbmsName);
		if (profile != null)
			return profile;
		profile = lookupProfile(dbmsName);
		return profile;
	}

	// 缺省的函数注册掉
	public DbmsProfile() {
		for (FunctionMapping m : DEFAULT_FUNCTIONS) {
			this.functions.put(m.getFunction().getName(), m);
			this.functionsIndex.put(m.getStardard(), m);
		}
	}

	private synchronized static DbmsProfile lookupProfile(String dbmsName) {
		if (dialectMappings == null) {
			dialectMappings=initDialectMapping();
		}

		String classname = dialectMappings.remove(dbmsName);
		if (classname == null) {
			throw new IllegalArgumentException("the dbms '" + dbmsName + "' is not supported yet");
		}
		try {
			Class<?> c = Class.forName(classname);
			DbmsProfile result = (DbmsProfile) c.newInstance();
			ITEMS.put(dbmsName, result);
			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			LogUtil.exception(e);
			throw new IllegalArgumentException("the Dialect class can't be created:" + classname);
		}
	}

	private static Map<String, String> initDialectMapping() {
		URL url = DbmsProfile.class.getResource("/META-INF/dialect-mapping.properties");
		if (url == null) {
			LogUtil.fatal("Can not found Dialect Mapping File. /META-INF/dialect-mapping.properties");
		}
		Map<String, String> config = IOUtils.loadProperties(url);
		String file = JefConfiguration.get(DbCfg.DB_DIALECT_CONFIG);
		if (StringUtils.isNotEmpty(file)) {
			url = DbmsProfile.class.getClassLoader().getResource(file);
			if (url == null) {
				LogUtil.warn("The custom dialect mapping file [{}] was not found.", file);
			} else {
				Map<String, String> config1 = IOUtils.loadProperties(url);
				config.putAll(config1);
			}
		}
		return config;
	}

	/**
	 * 从指定的资源文件中加载关键字列表
	 * 
	 * @param path
	 */
	protected void loadKeywords(String path) {
		InputStream in = this.getClass().getResourceAsStream(path);
		if (in == null) {
			throw new NullPointerException("Resource not found:" + path);
		}
		BufferedReader reader = IOUtils.getReader(in, "US-ASCII");
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					keywords.add(line);
				}
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	static {
		// 五个基本统计函数
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("avg"), Func.avg, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("count"), Func.count, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("max"), Func.max, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("min"), Func.min, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("sum"), Func.sum, 0));
		// 六个基本三角函数
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("sin"), Scientific.sin, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("cos"), Scientific.cos, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("tan"), Scientific.tan, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("asin"), Scientific.asin, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("acos"), Scientific.acos, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("atan"), Scientific.atan, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("sqrt"), Scientific.sqrt, 0));
		// Others
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("abs"), Func.abs, 0));
		DEFAULT_FUNCTIONS.add(new FunctionMapping(new StandardSQLFunction("sign"), Func.sign, 0));

	}

	protected static final String QUOT = "'";
	protected Set<Feature> features;
	protected Map<String, FunctionMapping> functions = new HashMap<String, FunctionMapping>();
	protected Map<DbFunction, FunctionMapping> functionsIndex = new HashMap<DbFunction, FunctionMapping>();
	protected Map<DbProperty, String> properties = new IdentityHashMap<DbProperty, String>();

	protected void setProperty(DbProperty key, String value) {
		properties.put(key, value);
	}

	/**
	 * 注册函数，该函数为数据库原生支持的。
	 * @param func 要支持的数据库函数
	 * @param synonyms 其他要支持的别名
	 */
	protected void registerNative(DbFunction func, String... synonyms) {
		registerNative(func, new StandardSQLFunction(func.name()), synonyms);
	}

	/**
	 * 注册函数，该函数为数据库原生支持的
	 * @param func 要支持的数据库函数
	 * @param function 函数实现
	 * @param synonyms 其他要支持的别名
	 */
	protected void registerNative(DbFunction func, SQLFunction function, String... synonyms) {
		FunctionMapping mapping = new FunctionMapping(function, func, FunctionMapping.MATCH_FULL);
		String name = function.getName();
		Assert.notNull(name);
		// 按小写名称索引
		this.functions.put(name, mapping);
		if (func != null) {
			FunctionMapping old = this.functionsIndex.put(func, mapping);
			if (old != null && synonyms.length == 0) {
				throw new IllegalArgumentException("duplicate reg of " + name + " in " + this.getName());
			}
		}
		// 各种别名的注册
		for (String n : synonyms) {
			this.functions.put(n, mapping);
		}
	}

	/**
	 * 注册函数，该函数为数据库原生支持的
	 * @param function 函数实现
	 * @param synonyms 其他要支持的别名
	 */
	protected void registerNative(SQLFunction function, String... synonyms) {
		registerNative(null, function, synonyms);
	}

	/**
	 * 注册虚拟函数，该函数名和数据库本地函数不同，但用法相似（或一样）。
	 * 实际使用时虚拟函数名将被替换为本地函数名
	 * 
	 * @param func        虚拟函数名
	 * @param nativeName  本地函数名
	 */
	protected void registerAlias(DbFunction func, String nativeName) {
		FunctionMapping fm = functions.get(nativeName);
		if (fm == null) {
			throw new IllegalArgumentException("Dialect error! the native function " + nativeName + " in " + getName() + " not found!");
		}
		FunctionMapping mapping = new FunctionMapping(fm.getFunction(), func, FunctionMapping.MATCH_NAME_CHANGE);
		this.functions.put(func.name(), mapping);
		this.functionsIndex.put(func, mapping);
	}

	/**
	 * 注册虚拟函数，该函数名和数据库本地函数不同，但用法相似（或一样）。
	 * 实际使用时虚拟函数名将被替换为本地函数名
	 * @param func       虚拟函数名
	 * @param nativeName 本地函数名
	 */
	protected void registerAlias(String func, String nativeName) {
		FunctionMapping fm = functions.get(nativeName);
		if (fm == null) {
			throw new IllegalArgumentException("Dialect error! the native function " + nativeName + " in " + getName() + " not found!");
		}
		FunctionMapping mapping = new FunctionMapping(fm.getFunction(), null, FunctionMapping.MATCH_NAME_CHANGE);
		this.functions.put(func, mapping);
	}

	/**
	 * 注册一个函数的兼容实现
	 * @param func     要注册的函数
	 * @param function 函数实现
	 * @param synonyms 该实现的其他可用名称
	 * 
	 */
	protected void registerCompatible(DbFunction func, SQLFunction function, String... synonyms) {
		FunctionMapping mapping = new FunctionMapping(function, func, FunctionMapping.MATCH_EMULATION);
		if (func != null) {
			String name = func.name();
			this.functions.put(name, mapping);
			FunctionMapping old = this.functionsIndex.put(func, mapping);
			if (old != null && old.getMatch() < FunctionMapping.MATCH_EMULATION) {
				this.functionsIndex.put(func, old);// 再重新挤回去。即以匹配度最高的函数实现为准
			}
		}
		for (String s : synonyms) {
			this.functions.put(s, mapping);
		}
	}

	public String getFunction(DbFunction func, Object... params) {
		FunctionMapping mapping = this.getFunctionsByEnum().get(func);
		if (mapping == null) {
			throw new IllegalArgumentException("Unknown database function " + func.name());
		}
		if (mapping.getArgCount() == 0) {
			SQLFunction sfunc = mapping.getFunction();
			return sfunc.renderExpression(Collections.<Expression> emptyList()).toString();
		} else {
			List<Expression> exps = new ArrayList<Expression>();
			for (Object s : params) {
//				try {
					if(s instanceof Expression){
						exps.add((Expression)s);	
					}else{
//						StSqlParser parser = new StSqlParser(new StringReader(String.valueOf(s)));
//						exps.add(parser.PrimaryExpression());
						exps.add(new SqlExpression(String.valueOf(s)));
					}
//				} catch (ParseException e) {
//					throw new PersistenceException("The input string [" + s + "] can not be parsed.", e);
//				}
			}
			SQLFunction sfunc = mapping.getFunction();
			Expression ex = sfunc.renderExpression(exps);
			if (ex == null) {
				throw new RuntimeException("函数" + sfunc.getName() + " " + sfunc.getClass() + "没有实现！");
			}
			return ex.toString();
		}
	}

	/**
	 * 四种情况 1、数据库的函数已经实现了所需要的标准函数。无需任何更改 2、数据库的函数和标准函数参数含义（基本）一样，仅需变化一下名称，如 nvl
	 * -> ifnull 3、数据库的函数和标准函数差别较大，通过多个其他函数模拟实现。（参数一致） 4、数据库的无法实现指定的函数。
	 */
	public Map<String, FunctionMapping> getFunctions() {
		return functions;
	}

	public Map<DbFunction, FunctionMapping> getFunctionsByEnum() {
		return functionsIndex;
	}

	public String getDefaultSchema() {
		return null;
	}

	public void processConnectProperties(DataSourceInfo dsw) {
	}

	public boolean containKeyword(String name) {
		return keywords.contains(name);
	}

	public String getProperty(DbProperty key) {
		return properties.get(key);
	}

	public String getProperty(DbProperty key, String defaultValue) {
		String value = properties.get(key);
		return value == null ? defaultValue : value;
	}

	/**
	 * 从ResultSet中的类型转换到合适的Java类型。
	 */
	public Object getJavaValue(ColumnType column, Object object) {
		if (object == null)
			return null;

		// boolean类型的解析
		if (column instanceof ColumnType.Boolean) {
			return "1".equals((object.toString()));
		}
		// 除此之外的基本类型的值都不需要再单独处理
		if (object instanceof String)
			return object;
		if (object instanceof Number)
			return object;
		// 复杂类型的解析
		if (column instanceof ColumnType.Date) {
			return object;
		} else if (column instanceof ColumnType.TimeStamp) {
			return object;
			// }else{
			// System.err.println("Unknown javaField Type: " +
			// object.getClass().getName());
		}
		return object;
	}

	/**
	 * 产生用于建表的SQL语句
	 * 
	 */
	public final String getCreationComment(ColumnType column, boolean flag) {
		if (column instanceof ColumnType.Char) {
			return getComment((ColumnType.Char) column, flag);
		} else if (column instanceof ColumnType.Boolean) {
			return getComment((ColumnType.Boolean) column, flag);
		} else if (column instanceof ColumnType.Varchar) {
			return getComment((ColumnType.Varchar) column, flag);
		} else if (column instanceof ColumnType.Double) {
			return getComment((ColumnType.Double) column, flag);
		} else if (column instanceof ColumnType.Int) {
			return getComment((ColumnType.Int) column, flag);
		} else if (column instanceof ColumnType.Date) {
			return getComment((ColumnType.Date) column, flag);
		} else if (column instanceof ColumnType.TimeStamp) {
			return getComment((ColumnType.TimeStamp) column, flag);
		} else if (column instanceof ColumnType.AutoIncrement) {
			return getComment((ColumnType.AutoIncrement) column, flag);
		} else if (column instanceof ColumnType.Clob) {
			return getComment((ColumnType.Clob) column, flag);
		} else if (column instanceof ColumnType.Blob) {
			return getComment((ColumnType.Blob) column, flag);
		} else if (column instanceof ColumnType.GUID) {
			return getComment((ColumnType.GUID) column, flag);
		} else {
			System.err.println("Unknown javaField Type: " + column.getClass().getName());
			return "";
		}
	}

	protected String getComment(Blob column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		if (flag) {
			sb.append("blob");
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(Clob column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		if (flag) {
			sb.append("clob");
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(ColumnType.Char column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("char(").append(column.length).append(')');
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(ColumnType.Boolean column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("char(1)");
		if (flag) {
			if (column.defaultValue != null) {
				Boolean b = StringUtils.toBoolean(String.valueOf(column.defaultValue), false);
				sb.append(" default ").append(b ? "'1'" : "'0'");
			}
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(Varchar column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("varchar(").append(column.length).append(')');
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(ColumnType.Double column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		if (column.precision + column.scale > 16) {
			sb.append("double");
		} else {
			sb.append("float");
		}
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(column.defaultValue.toString());
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(ColumnType.Int column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		if (column.precision > 13) {
			sb.append("bigint");
		} else {
			sb.append("int");
		}
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(column.defaultValue.toString());
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(ColumnType.Date column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("date");
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(ColumnType.TimeStamp column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("timestamp");
		if (flag) {
			if (column.defaultValue != null) {
				sb.append(" default ").append(toDefaultString(column.defaultValue));
				if (!column.nullable)
					sb.append(" not null");
			}
		}
		return sb.toString();
	}

	/**
	 * 关于自增字段的定义 部分数据库只支持其中一种。但是，有By Default的，要尽量使用BY DEFAULT关键字 本来是hsql不支持by
	 * default,造成自增字段不支持人工设置。/GENERATED ALWAYS AS IDENTITY /GENERATED BY DEFAULT
	 * AS IDENTITY
	 * 
	 * @param column
	 * @param flag
	 * @return
	 */
	protected String getComment(ColumnType.AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("int generated by default as identity ");
		if (!column.nullable)
			sb.append(" not null");
		return sb.toString();
	}

	protected String getComment(ColumnType.GUID column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("char(36)");
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
		}
		return sb.toString();
	}

	public boolean checkPKLength(ColumnType type) {
		return true;
	}

	public String toDefaultString(Object defaultValue) {
		if (defaultValue == null) {
			return null;
		}
		if (defaultValue instanceof Boolean) {
			defaultValue = this.toBooleanSqlParam((java.lang.Boolean) defaultValue);
		}
		if (defaultValue instanceof DbFunction) {
			return this.getFunction((DbFunction) defaultValue);
		} else if (defaultValue instanceof SqlExpression) {
			return defaultValue.toString();
		} else if (defaultValue instanceof Number) {
			return defaultValue.toString();
		} else if (defaultValue instanceof Boolean) {
			return defaultValue.toString();
		} else if (defaultValue instanceof String) {
			if (((String) defaultValue).length() == 0)
				return null;
			return "'" + (String) defaultValue + "'";
		} else {
			return "'" + String.valueOf(defaultValue) + "'";
		}

	}

	/**
	 * 从Java对象得到数据流(供Blob使用)
	 */
	public static InputStream getStreamFromBeanField(Object value) throws IOException {
		if (value == null)
			return null;
		if (value instanceof String) {
			return new ByteArrayInputStream(((String) value).getBytes());
		} else if (value instanceof File) {
			return new FileInputStream((File) value);
		} else if (value instanceof InputStream) {
			return (InputStream) value;
		} else if (value instanceof BigDataBuffer) {
			return ((BigDataBuffer) value).getAsStream();
		} else if (value instanceof byte[]) {
			return new ByteArrayInputStream((byte[]) value);
		} else if (value instanceof ByteBuffer) {
			return new ByteBufferInputStream((ByteBuffer) value);
		} else {
			throw new IOException("Unsupported field type " + value.getClass().getName() + " who is mapping to the Oracle Lob column.");
		}
	}

	/**
	 * 从Java对象得到字符流(供Clob使用)
	 * 
	 * @param value
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getReaderFromBeanField(Object value) throws IOException {
		if (value instanceof String) {
			return new BufferedReader(new StringReader((String) value));
		} else if (value instanceof File) {
			return IOUtils.getReader((File) value, null);
		} else if (value instanceof BufferedReader) {
			return (BufferedReader) value;
		} else if (value instanceof Reader) {
			return new BufferedReader((Reader) value);
		} else if (value instanceof InputStream) {
			return new BufferedReader(new InputStreamReader((InputStream) value));
		} else if (value instanceof BigDataBuffer) {
			return new BufferedReader(new InputStreamReader(((BigDataBuffer) value).getAsStream()));
		} else if (value instanceof byte[]) {
			return new BufferedReader(new StringReader(new String((byte[]) value)));
		} else {
			throw new IOException("Unsupported field type " + value.getClass().getName() + " who is mapping to the Oracle Lob column.");
		}
	}

	public CachedRowSet newCacheRowSetInstance() throws SQLException {
		return new jef.database.rowset.CachedRowSetImpl();
	}

	/**
	 * 将数据库定义的字段类型映射到JEF的字段类型上
	 */
	public ColumnType getProprtMetaFromDbType(jef.database.meta.Column column) {
		int type = column.getDataTypeCode();
		switch (type) {
		case Types.TINYINT:
		case Types.SMALLINT:
		case Types.INTEGER:
		case Types.BIGINT:
			int size = column.getColumnSize();
			if (size > 30)
				size = column.getDecimalDigit();
			if ("AUTOINCREMENT".equalsIgnoreCase(column.getColumnDef()))
				return new ColumnType.AutoIncrement(size);
			if (column.getColumnDef() != null && column.getColumnDef().startsWith("GENERATED")) {
				return new ColumnType.AutoIncrement(size);
			} else {
				return new ColumnType.Int(size);
			}
		case Types.DECIMAL:
		case Types.NUMERIC:
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.REAL:
			if (column.getColumnSize() == 0)
				column.setColumnSize(12);
			if (column.getDecimalDigit() == 0)
				column.setDecimalDigit(4);
			return new ColumnType.Double(column.getColumnSize(), column.getDecimalDigit());
		case Types.BIT:
		case Types.BOOLEAN:
			return new ColumnType.Boolean();
		case Types.VARBINARY:
		case Types.BINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return new ColumnType.Blob();
		case Types.DATE:
			return new ColumnType.Date();
		case Types.TIME:
		case Types.TIMESTAMP:
			return new ColumnType.TimeStamp();
		case Types.CHAR:
		case Types.NCHAR:
			return new ColumnType.Char(column.getColumnSize());
		case Types.VARCHAR:
		case Types.NVARCHAR:
		case Types.LONGVARCHAR:
		case Types.LONGNVARCHAR:
			if (column.getColumnSize() > 4000) {
				return new ColumnType.Clob();
			} else {
				if ("GUID".equals(column.getColumnDef())) {
					return new ColumnType.GUID();
				} else {
					return new Varchar(column.getColumnSize());
				}
			}
		case Types.OTHER: // Varbit in PG and nvarchar2 in oracle returns OTHER,
							// seems they can all mapping to String value in
							// java..
		case Types.SQLXML:
			return new Varchar(column.getColumnSize());
		case Types.CLOB:
		case Types.NCLOB:
			return new ColumnType.Clob();
			//
			// case Types.DISTINCT:
			// case Types.NULL:
			// case Types.ARRAY:
			// case Types.STRUCT:
			// case Types.DATALINK:
			// case Types.JAVA_OBJECT:
			// case Types.REF:
			// case Types.ROWID:
		default:
			throw new RuntimeException("Unknown data type " + column.getDataType() + " " + type + "  " + column);
		}
	}

	/**
	 * 像Oracle，其Catlog是不用的，那么返回null mySQL没有Schema，每个database是一个catlog，那么返回值
	 * 
	 * @param schema
	 * @return
	 */
	public String getCatlog(String schema) {
		return null;
	}

	/**
	 * 对于表名前缀的XX. MYSQL是作为catlog的，不是作为schema的
	 * 
	 * @param schema
	 * @return
	 */
	public String getSchema(String schema) {
		return StringUtils.upperCase(schema);
	}

	public boolean notHas(Feature feature) {
		return !features.contains(feature);
	}

	public boolean has(Feature feature) {
		return features.contains(feature);
	}

	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		sb.append(getName() + ":");
		pathOrName = pathOrName.replace('\\', '/');
		sb.append(pathOrName);
		String url = sb.toString();
		return url;
	}

	public String getObjectNameIfUppercase(String name) {
		return name;
	}

	public String getColumnNameIncase(String name) {
		return name;
	}

	public int calcSequenceStep(OperateTarget conn, String schema, String seqName, int defaultValue) {
		return defaultValue;
	}

	public Object toBooleanSqlParam(java.lang.Boolean bool) {
		// 必然非null
		return String.valueOf(bool.booleanValue());
	}

	public java.sql.Timestamp toTimestampSqlParam(Date timestamp) {
		return DateUtils.toSqlTimeStamp(timestamp);
	}

	public String[] getOtherVersionSql() {
		return ArrayUtils.EMPTY_STRING_ARRAY;
	}

	public boolean isIOError(SQLException se) {
		return false;
	}

	public void processIntervalExpression(BinaryExpression parent, Interval interval) {
	}

	public void processIntervalExpression(Function func, Interval interval) {
	}

	public String getSqlDateExpression(Date value) {
		return ATypeMapping.wrapSqlStr(DateUtils.formatDate(value));
	}

	public String getSqlTimeExpression(Date value) {
		return ATypeMapping.wrapSqlStr(DateFormats.TIME_ONLY.get().format(value));
	}

	public String getSqlTimestampExpression(Date value) {
		return ATypeMapping.wrapSqlStr(DateFormats.DATE_TIME_CS.get().format(value));
	}

	public int getClobDataType() {
		return Types.CLOB;
	}

	public int getBlobDataType() {
		return Types.BLOB;
	}

	public long getColumnAutoIncreamentValue(AutoIncrementMapping<?> mapping, OperateTarget db) {
		throw new UnsupportedOperationException(mapping.getMeta().getName() + "." + mapping.fieldName() + " is auto-increament, but the database '" + this.getName() + "' doesn't support fetching the next AutoIncreament value.");
	}

	public String toPageSQL(String sql, IntRange range, boolean isUnion) {
		return toPageSQL(sql, range);
	}

	public Statement wrap(Statement stmt, boolean isInJpaTx) throws SQLException {
		return stmt;
	}

	public PreparedStatement wrap(PreparedStatement stmt, boolean isInJpaTx) throws SQLException {
		return stmt;
	}

	public void addKeyword(String... keys) {
		for (String s : keys) {
			keywords.add(s);
		}
	}
	
	public static void ensureUserFunction(FunctionMapping mapping,OperateTarget db) throws SQLException {
		DbMetaData meta=db.getMetaData();
		boolean flag=true;
		for(String name:mapping.requiresUserFunction()){
			if(meta.checkedFunctions.contains(name)){
				continue;
			}
			meta.checkedFunctions.add(name);
			if(!meta.existsFunction(null, name)){
				flag=false;
				break;
			}
		}
		if(flag)return;
		SQLFunction sf=mapping.getFunction();
		URL url=sf.getClass().getResource(sf.getClass().getSimpleName()+".sql");
		if(url==null){
			throw new IllegalArgumentException("Can't find user script file for user function "+ sf);
		}
		try{
			meta.executeScriptFile(url);
		}catch(SQLException ex){
			throw ex;
		}
	}

	public void toExtremeInsert(InsertSqlClause sql) {
	}
	
	
}
