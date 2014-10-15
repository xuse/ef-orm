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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.OperateTarget;
import jef.database.datasource.DataSourceInfo;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.FunctionMapping;
import jef.database.support.RDBMS;
import jef.database.wrapper.clause.InsertSqlClause;

/**
 * 这个类原本只用于SQL方言的转换，今后将逐渐代替dbmsprofile的作用
 * 
 * @author Administrator
 * 
 */
public interface DatabaseDialect {
	/**
	 * 得到名称
	 * 
	 * @return
	 */
	RDBMS getName();

	/**
	 * 在创建数据库连接之前，在datasource中增加一些properties的值。用于指定一些连接属性。
	 * 比如今后可以对MYSQL的fetchSize,隔离级别等进行设置
	 * 
	 * 目前仅对oracle作了处理。
	 * 
	 * @param dsw
	 */
	void processConnectProperties(DataSourceInfo dsw);

	/**
	 * 得到用于建表的备注文字
	 * 
	 * @param vType
	 * @param typeStrOnly
	 *            为true时，只返回数据类型，不返回not null default等值
	 */
	String getCreationComment(ColumnType vType, boolean typeStrOnly);
	
	/**
	 * 得到该数据库上该种数据类型的真实实现类型。
	 * 比如，在不支持boolean类型的数据库上，会以char类型代替boolean；在不支持blob的数据库上，会以varbinary类型代替blob
	 * 
	 * @param vType
	 * @return
	 */
	int getImplementationSqlType(ColumnType vType);

	/**
	 * 检查主键字段的长度
	 * @deprecated 此方法过于特殊，重构中要设法去除
	 */
	boolean checkPKLength(ColumnType type);
	/**
	 * @deprecated use {@link #getImplementationSqlType(ColumnType)} instead.
	 * @return
	 */
	int getClobDataType();

	/**
	 * @deprecated use {@link #getImplementationSqlType(ColumnType)} instead. 
	 */
	int getBlobDataType();

	/**
	 * 返回若干用于查询数据库基本信息的SQL语句，如果配置了这些SQL语句，那么启动时在输出数据库版本信息的时候就会
	 * 将这些SQL的执行结果也作为版本信息一起输出
	 * @deprecated 今后重构中去除
	 * 
	 * @return
	 */
	String[] getOtherVersionSql();
	
	/**
	 * 将表达式或值转换为文本形式的缺省值描述
	 * @param defaultValue
	 * @param sqlType
	 * @return
	 */
	String toDefaultString(Object defaultValue, int sqlType);

	/**
	 * 得到用于缓存结果的resultSet的实例(备注，Oracle需要特别的容器)
	 */
	CachedRowSet newCacheRowSetInstance() throws SQLException;

	/**
	 * 已知数据库中的字段类型，返回JEF对应的meta类型
	 */
	ColumnType getProprtMetaFromDbType(jef.database.meta.Column dbTypeName);

	/**
	 * 判断数据库是否不支持某项特性
	 * 
	 * @param feature
	 * @return
	 */
	boolean notHas(Feature feature);

	/**
	 * 判断数据库是否支持某项特性
	 * 
	 * @param feature
	 * @return
	 */
	boolean has(Feature feature);

	/**
	 * 像Oracle，其Catlog是不用的，那么返回null mySQL没有Schema，每个database是一个catlog，那么返回值
	 * 同时修正返回的大小写
	 * 
	 * @param schema
	 * @return
	 */
	String getCatlog(String schema);

	/**
	 * 对于表名前缀的XX. MYSQL是作为catlog的，不是作为schema的 同时修正返回的大小写
	 * 
	 * @param schema
	 * @return
	 */
	String getSchema(String schema);

	/**
	 * 获取数据库的默认驱动类
	 * 
	 * @param url
	 *            Derby根据连接方式的不同，会有两种不同的DriverClass，因此需要传入url
	 * @return 驱动类
	 */
	String getDriverClass(String url);

	/**
	 * 生成数据库连接字串
	 * 
	 * @param host
	 *            <=0则会使用默认端口
	 * @param port
	 * @param filepath
	 * @param dbname
	 * @return
	 */
	String generateUrl(String host, int port, String pathOrName);

	/**
	 * 将SQL转换为分页的语句
	 * 
	 * @param sql
	 * @param range
	 * @return
	 */
	String toPageSQL(String sql, IntRange range);

	/**
	 * 将SQL转换为分页的语句
	 * 
	 * @param sql
	 *            是一个UNION语句
	 * @param range
	 * @return
	 */
	String toPageSQL(String sql, IntRange range, boolean isUnion);

	/**
	 * Oracle会将所有未加引号的数据库对象名称都按照大写对象名来处理，MySQL则对表名一律转小写，列名则保留原来的大小写。
	 * 为了体现这一数据库策略的不同，这里处理大小写的问题。
	 * 
	 * 目前的原则是：凡是涉及
	 * schema/table/view/sequence/dbname等转换的，都是用此方法，凡是设计列名转换，列别名定义的都用
	 * {@link #getColumnNameIncase}方法
	 * 
	 * 注意这个方法接下来要改名为 getObjectNameToUse();in case这个说法歧义，参考Spring的命名，改为xxxxToUse更好一点
	 * 
	 * <p>
	 * TODO 季怡2013-7新增了{@link #getColumnNameIncase}
	 * ,目的是将列名的大小写策略和表/视图/schema名等策略区分开来。因为mysql似乎两者表现并不一致。
	 * 目前几乎所有的代码都还是引用getObjectNameIfUppercase这个方法的。今后在修改中，要逐渐按上述约定分别调用两个方法
	 * 
	 * @param name
	 * @return
	 */
	String getObjectNameIfUppercase(String name);

	/**
	 * @since 3.0 季怡2013-7新增，数据库对于表明和列名的大小写策略并不总是一致。因此今后要将列名处理的场合逐渐由原来的函数移到这里来实现
	 * 
	 *       注意这个方法接下来要改名为 getColumnNameToUse();因为incase这个说法歧义，参考Spring的命名，改为 xxxxToUse更好一点
	 * @param name
	 * @return
	 */
	String getColumnNameIncase(String name);

	/**
	 * 计算Sequence的步长, 在某些数据库上，可以从系统表中获得Sequence的步长。<br>
	 * 如果不支持计算步长，返回defaultValue。
	 */
	int calcSequenceStep(OperateTarget conn, String schema, String seqName, int defaultValue);

	/**
	 * 当使用Timestamp类型的绑定变量操作时，转换为什么值
	 * 
	 * @param timestamp
	 * @return
	 */
	java.sql.Timestamp toTimestampSqlParam(Date timestamp);

	/**
	 * 当出现异常时，使用此方法检查这个异常是否因为网络连接异常引起的。<br>
	 * 如果是那么会触发连接池的检查、清洗和重连等操作。
	 * 
	 * @param se
	 * @return
	 */
	boolean isIOError(SQLException se);

	/**
	 * 根据 JDBC的URL，解析出其中的dbname,host，user等信息。目的是为了在不必连接数据库的情况下，得到数据库名称
	 * 
	 * @param connectInfo
	 */
	public void parseDbInfo(ConnectInfo connectInfo);

	/**
	 * 返回指定的属性
	 * 
	 * @return
	 */
	public String getProperty(DbProperty key);

	/**
	 * 返回指定的属性
	 * 
	 * @return
	 */
	public String getProperty(DbProperty key, String defaultValue);

	/**
	 * 不同数据库登录后，所在的默认schema是不一样的
	 * <ul>
	 * <li>Oracle是以登录的用户名作为schema的。</li>
	 * <li>mysql是只有catlog不区分schema的。</li>
	 * <li>derby支持匿名访问，此时好像是位于APP这个schema下。</li>
	 * <li>SQL Server默认是在dbo这个schema下</li>
	 * </ul> 
	 * <br>因此对于无法确定当前schema的场合，使用这里提供的schema名称作为当前schema
	 * @return
	 */
	String getDefaultSchema();

	/**
	 * 返回所有支持的SQL函数 四种情况
	 * <ul>
	 * <li>1、数据库的函数已经实现了所需要的标准函数。无需任何更改</li>
	 * <li>2、数据库的函数和标准函数参数含义（基本）一样，仅需变化一下名称，如 nvl -> ifnull</li>
	 * <li>3、数据库的函数和标准函数差别较大，通过多个其他函数模拟实现。（参数一致）</li>
	 * <li>4、数据库的无法实现指定的函数。</li>
	 * </ul>
	 * 
	 * @return
	 */
	Map<String, FunctionMapping> getFunctions();

	/**
	 * 返回所有标准函数
	 * 
	 * @return
	 */
	Map<DbFunction, FunctionMapping> getFunctionsByEnum();

	/**
	 * 返回指定的函数的SQL实现(不会查找自定义的函数)
	 * 
	 * @param function
	 * @param params
	 * @return
	 */
	String getFunction(DbFunction function, Object... params);

	/**
	 * SQL语法处理
	 * 
	 * @param parent
	 * @param interval
	 */
	void processIntervalExpression(BinaryExpression parent, Interval interval);

	/**
	 * SQL语法处理
	 * 
	 * @param func
	 * @param interval
	 */
	void processIntervalExpression(Function func, Interval interval);

	/**
	 * 检查数据库是否包含指定的关键字，用来进行检查的对象名称都是按照getColumnNameIncase转换后的，因此对于大小写统一的数据库，
	 * 这里无需考虑传入的大小写问题。
	 * 
	 * @param name
	 * @return
	 */
	boolean containKeyword(String name);

	/**
	 * 针对非绑定变量SQL，生成SQL语句所用的文本值。 Java -> SQL String
	 */
	String getSqlDateExpression(Date value);

	/**
	 * 针对非绑定变量SQL，生成SQL语句所用的文本值。 Java -> SQL String
	 */
	String getSqlTimeExpression(Date value);

	/**
	 * 针对非绑定变量SQL，生成SQL语句所用的文本值。 Java -> SQL String
	 */
	String getSqlTimestampExpression(Date value);

	/**
	 * 当使用了列自增的方式时，尝试在不插入列的情况下消耗一个自增值。
	 * 
	 * @param meta
	 * @return
	 */
	long getColumnAutoIncreamentValue(AutoIncrementMapping<?> mapping, OperateTarget db);

	/**
	 * 允许数据库方言对Statement再进行一次包装
	 * 
	 * @param stmt
	 * @param isInJpaTx
	 * @return
	 */
	Statement wrap(Statement stmt, boolean isInJpaTx) throws SQLException;

	/**
	 * 允许数据库方言对PreparedStatement再进行一次包装
	 * 
	 * @param stmt
	 * @param isInJpaTx
	 * @return
	 */
	PreparedStatement wrap(PreparedStatement stmt, boolean isInJpaTx) throws SQLException;

	/**
	 * 当有一个数据库实例连接初次创建时调用. Dialect可以通过直接连接数据库判断版本、函数等，调整Dialect内部的一些配置和数据。
	 * 
	 * @param db
	 */
	void init(OperateTarget db);

	/**
	 * 将插入语句转化为最快操作的语句
	 * 
	 * @param sql
	 */
	void toExtremeInsert(InsertSqlClause sql);
}
