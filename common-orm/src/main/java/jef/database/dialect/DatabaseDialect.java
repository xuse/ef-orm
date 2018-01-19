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
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.DbMetaData;
import jef.database.datasource.DataSourceInfo;
import jef.database.dialect.handler.LimitHandler;
import jef.database.dialect.type.AColumnMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ParserFactory;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.jdbc.JDBCTarget;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.FunctionMapping;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.SequenceInfo;
import jef.database.support.RDBMS;
import jef.database.wrapper.clause.InsertSqlClause;

import com.querydsl.sql.SQLTemplates;

/**
 * 这个类原本只用于SQL方言的转换，今后将逐渐代替dbmsprofile的作用
 * 
 * @author Administrator
 * 
 */
public interface DatabaseDialect {
	/**
	 * 得到RDBMS的名称
	 * 
	 * @return name of the RDBMS
	 */
	RDBMS getName();

	/**
	 * 在创建数据库连接之前，在datasource中增加一些properties的值。用于指定一些连接属性。
	 * 比如今后可以对MYSQL的fetchSize,隔离级别等进行设置
	 * <p> 
	 * 目前仅对oracle作了处理。
	 * <p>
	 * <h3>未来<h3>
	 * 从目前情况看，连接池和框架的分离是大势所趋，此处主要目的是针对Oracle一类的数据库链接进行参数控制。
	 * Oracle的特殊之处有
	 * remarkFeature
	 * @param dsw
	 */
	void processConnectProperties(DataSourceInfo dsw);

	/**
	 * 得到用于建表的数据类型
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
	int getImplementationSqlType(int sqlType);
	
	/**
	 * 将表达式或值转换为文本形式的缺省值描述
	 * @param defaultValue
	 * @param sqlType
	 * @return
	 */
	String toDefaultString(Object defaultValue, int sqlType,int changeTo);

	/**
	 * 可以将ResultSet缓存在特定的RowSet对象中。
	 * 但是Oracle又要搞特殊化了，需要一个专门的容器。
	 * @return 符合当前数据库要求的结果集缓存对象
	 */
	CachedRowSet newCacheRowSetInstance() throws SQLException;

	/**
	 * 已知数据库中的字段类型，返回JEF对应的meta类型
	 */
	ColumnType getProprtMetaFromDbType(jef.database.meta.object.Column dbTypeName);

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
	 * 生成数据库JDBC连接字串。
	 * 
	 * @param host 主机名 
	 *            
	 * @param port 端口，<=0则会使用默认端口
	 * @param pathOrName 对于一些本地数据库是本地路径，对于Oracle是SID服务名。对于其他数据库是数据库名。 
	 * @return 生成的JDBC字符串
	 */
	String generateUrl(String host, int port, String pathOrName);
	
	
	/**
	 * 得到分页管理器
	 * @return 分页管理器
	 */
	LimitHandler getLimitHandler();

	/**
	 * Oracle会将所有未加引号的数据库对象名称都按照大写对象名来处理，MySQL则对表名一律转小写，列名则保留原来的大小写。
	 * 为了体现这一数据库策略的不同，这里处理大小写的问题。
	 * 
	 * 目前的原则是：凡是涉及
	 * schema/table/view/sequence/dbname等转换的，都是用此方法，凡是设计列名转换，列别名定义的都用
	 * {@link #getColumnNameIncase}方法
	 * 
	 * @param name
	 * @return
	 */
	String getObjectNameToUse(String name);
	
	/**
	 * 获得大小写正确的列名
	 * @param name
	 * @return
	 */
	String getColumnNameToUse(AColumnMapping name);

	/**
	 * 根据数据库特性，从数据库系统表中返回序列信息，如果不支持则返回null。如果不存在返回空列表
	 * @param conn 数据库访问句柄
	 * @param schema 允许为null。可以使用%
	 * @param seqName 允许为null。可以使用%
	 * @return 序列的信息
	 */
	List<SequenceInfo> getSequenceInfo(DbMetaData conn, String schema, String seqName)  throws SQLException;

	/**
	 * 根据数据库特性，从数据库系统表中返回约束信息，如果不支持返回null。如果不存在返回空列表
	 * @param conn 数据库访问句柄
	 * @param schema 允许为null。可以使用%
	 * @param constraintName 允许为null。可以使用%
	 * @return 约束的信息
	 */
	List<Constraint>   getConstraintInfo(DbMetaData conn,String schema,String constraintName) throws SQLException;
	
	
	/**
	 * 当使用Timestamp类型的绑定变量操作时，转换为什么值
	 * <h3>这个功能是干什么用的</h3>
	 * 将java.util.Date转换为java.sql.Timestamp。这个功能本来和数据库方言是无关的，只涉及时间的转换。
	 * 然而问题在于Oracle中，曾经只有Date类型。而且这个Date是年月日时分秒，但不到毫秒。
	 * 也就是说，如果你使用Oracle来存储date（到秒），而当你将Date字段作为查询条件时，尴尬就发生了——
	 * 传入java.sql.Date，会丢失时分秒精度；而传入java.sql.Timestamp作查询条件，有可能是查不到记录的，因为还有毫秒精度，还是查不到数据。
	 * 因此本方言会将映射到Oracle时的精度缩减到秒。从而解决上述问题。
	 * <p>
	 * 
	 * @param timestamp java.util.Date 
	 * @return java.sql.Timestamp
	 */
	java.sql.Timestamp toTimestampSqlParam(Date timestamp);

	/**
	 * 当出现异常时，使用此方法检查这个异常是否因为网络连接异常引起的。
	 * <p>
	 * <h3>用途</h3>
	 * 用于内置连接池的刷新——可见自己实现一个连接池其实多么复杂。
	 * 因为当数据库断开或重启时，往往所有的连接都会失效。此时使用连接池中的连接就会出错。
	 * 比较典型的场景是数据库重启了一次，此时连接池里的连接看上去都是好好的，但实际上真正在上面执行SQL全都会出错。
	 * 
	 * 一种办法是当从连接池获得每个连接都对连接进行检查，这种检查必需是通过网络通信的检查，因为此时如果调用 Connection.isClosed()会正常返回false,
	 * 因此这种检查就显得额外占开销，在高并发的OLTP中并不经济。
	 * 因此试图实现一种更为有效的检查方式，即当任何数据库操作出现错误，并且这个错误是由连接IO引起，那么都要对连接池立刻进行清洗和检查，用这种触发机制来保证连接池的高效。
	 * <p>
	 * <h3>未来</h3>
	 * 上述作为一种面向电信项目过程中设计方面的尝试，目前看来我更希望降低本框架的复杂度，让连接池独立出来，去除和连接池的耦合。并且连接池封装了所有的数据库操作，也能自行完成这一逻辑。
	 * 所以未来版本中可能会去除，连接池应该由更专业的框架去完成。
	 * @param se 异常
	 * @return 如果是IO异常，那么返回true
	 */
	boolean isIOError(SQLException se);

	/**
	 * 根据 JDBC的URL，解析出其中的dbname,host，user等信息。目的是为了在不必连接数据库的情况下，得到数据库名称
	 * 
	 * @param connectInfo
	 */
	public void parseDbInfo(ConnectInfo connectInfo);

	/**
	 * 返回指定的属性(文本)
	 * @param key 特性名称
	 * @return 特性文本
	 */
	public String getProperty(DbProperty key);

	/**
	 * 返回指定的属性(文本)
	 * @param key 特性名称
	 * @param defaultValue 缺省值
	 * @return 特性文本
	 */
	public String getProperty(DbProperty key, String defaultValue);
	
	/**
	 * 返回指定特性（数值），如果无值返回0
	 * @param key 特性名称
	 * @return 数值
	 */
	public int getPropertyInt(DbProperty key);
	
	/**
	 * 返回指定数据库特性(数值)，如果无值返回0
	 * @param key 特性名称
	 * @return 数值
	 */
	public long getPropertyLong(DbProperty key);

	/**
	 * 不同数据库登录后，所在的默认schema是不一样的
	 * <ul>
	 * <li>Oracle是以登录的用户名作为schema的。</li>
	 * <li>mysql是只有catlog不区分schema的。</li>
	 * <li>derby支持匿名访问，此时好像是位于APP这个schema下。</li>
	 * <li>SQL Server默认是在dbo这个schema下</li>
	 * </ul> 
	 * <br>因此对于无法确定当前schema的场合，使用这里提供的schema名称作为当前schema
	 * @return 当前RDBMS的缺省Schema
	 */
	String getDefaultSchema();

	/**
	 * 返回所有支持的SQL函数 四种情况
	 * <ul>
	 * <li>1、数据库的函数已经实现了所需要的标准函数。无需任何更改</li>
	 * <li>2、数据库的函数和标准函数参数含义（基本）一样，仅需变化一下名称，如 nvl -> ifnull</li>
	 * <li>3、数据库的函数和标准函数差别较大，通过多个其他函数、甚至存储过程来模拟实现。（参数一致）</li>
	 * <li>4、数据库的无法实现指定的函数。</li>
	 * </ul>
	 * @return 当前数据库方言中内置的所有支持的函数
	 */
	Map<String, FunctionMapping> getFunctions();

	/**
	 * 返回所有标准函数
	 * @return 当前数据库方言中内置的所有支持的函数
	 */
	Map<DbFunction, FunctionMapping> getFunctionsByEnum();

	/**
	 * 返回指定的函数的SQL表达式(不会查找自定义的函数)
	 * 
	 * @param function 函数
	 * @param params 函数的参数
	 * @return 函数在当前数据库上的表达式
	 */
	String getFunction(DbFunction function, Object... params);

	/**
	 * SQL语法处理
	 * 转换INTERVAL相关的语法。这个语法是SQL语言中用来表示时间差相关表达式的。
	 * @param parent 要处理的SQL AST
	 * @param interval 时间间隔 AST
	 */
	void processIntervalExpression(BinaryExpression parent, Interval interval);

	/**
	 * SQL语法处理
	 * 转换INTERVAL相关的语法。这个语法是SQL语言中用来表示时间差相关表达式的。
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
	 * 一般来说大部分数据库支持以字符串方式写入日期，但正常情况下基于绑定变量操作，完全不需要使用此特性。
	 * 此处主要是为了在生成DDL语句时，为列的Default value设置表达式所设计的。因为在DDL的表达式中肯定是无法使用绑定变量的。
	 * @param value 要转换的日期
	 * @return 日期在数据库中的字符串表达
	 */
	String getSqlDateExpression(Date value);

	/**
	 * 针对非绑定变量SQL，生成SQL语句所用的文本值。 Java -> SQL String
	 * 一般来说大部分数据库支持以字符串方式写入日期，但正常情况下基于绑定变量操作，完全不需要使用此特性。
     * 此处主要是为了在生成DDL语句时，为列的Default value设置表达式所设计的。因为在DDL的表达式中肯定是无法使用绑定变量的。
     * @param value 要转换的时间
     * @return 时间在数据库中的字符串表达
	 */
	String getSqlTimeExpression(Date value);

	/**
	 * 针对非绑定变量SQL，生成SQL语句所用的文本值。 Java -> SQL String
	 * 一般来说大部分数据库支持以字符串方式写入日期，但正常情况下基于绑定变量操作，完全不需要使用此特性。
     * 此处主要是为了在生成DDL语句时，为列的Default value设置表达式所设计的。因为在DDL的表达式中肯定是无法使用绑定变量的。
     * @param value 要转换的日期时间
     * @return 日期时间在数据库中的字符串表达
	 */
	String getSqlTimestampExpression(Date value);

	/**
	 * 当使用了列自增的方式时，尝试在不插入列的情况下消耗一个自增值。
	 * 这个功能默认是不用支持的，目前仅针对一种很小众的特性而设计。
	 * 当使用JDBC 的可写入ResultSet时，用户可以用 <code>
     *  rs.moveToInsertRow();
     *  rs.insertRow();
     *   </code>
     * 方式插入记录（上帝啊，为什么要设计这种麻烦的接口，而且我真是脑抽了才想去支持这个特性。）
     * 现在的问题是，对于好几个数据库，此时不会去调取Sequence来获取下一个自增值。
     * 这样一来，要么无法正常插入；要么人工赋值后、继续操作就会出现主键冲突。
     * 所以当时针对几个麻烦的数据库，写了这个方法来计算自增值并偷偷赋值。
     * 
     * TODO 其实最应该做的是将入口封死，完全就不应该支持在结果集上插入记录的方法。2.0版将这个特性砍掉就好，应该也不会有人想用这个功能吧。
	 * @param mapping 自增字段配置
	 * @param db 数据库操作句柄 
	 * @return 下一个自增值
	 */
	long getColumnAutoIncreamentValue(AutoIncrementMapping mapping, JDBCTarget db);

	/**
	 * 允许数据库方言对Statement再进行一次包装
	 * <P>
	 * 目前看来只用在一个特性上，要从PG的一个特性说起。<br>
	 * PostgreSQL的事务很奇怪，只要在事务中出现一次错误的SQL操作，那么这个事务上就不能再执行任何SQL语句了，只能回滚或提交。
	 * 但别的数据库可不是这样的。所以为了让Postgresql“看起来”和别的数据库一样，就想了个歪招——在执行每句SQL语句之前记录一个SavePoint。
	 * 如果执行成功就释放SavePoint，如果执行失败就回滚到上一个SavePoint。这样看起来就和别的数据库一样了。<br>可能这个特性不是太必要，希望未来可以不要这样的特性。
	 * 
	 * @param stmt {@link Statement}
	 * @param isInJpaTx 是否在一个JPA事务中
	 * @return 包装后的Statement
	 */
	Statement wrap(Statement stmt, boolean isInJpaTx) throws SQLException;

	/**
	 * 允许数据库方言对PreparedStatement再进行一次包装
	 * <P>
     * 目前看来只用在一个特性上，要从PG的一个特性说起。<br>
     * PostgreSQL的事务很奇怪，只要在事务中出现一次错误的SQL操作，那么这个事务上就不能再执行任何SQL语句了，只能回滚或提交。
     * 但别的数据库可不是这样的。所以为了让Postgresql“看起来”和别的数据库一样，就想了个歪招——在执行每句SQL语句之前记录一个SavePoint。
     * 如果执行成功就释放SavePoint，如果执行失败就回滚到上一个SavePoint。这样看起来就和别的数据库一样了。<br>可能这个特性不是太必要，希望未来可以不要这样的特性。
	 * @param stmt
	 * @param isInJpaTx
	 * @return
	 */
	PreparedStatement wrap(PreparedStatement stmt, boolean isInJpaTx) throws SQLException;

	/**
	 * 初始化方言，根据JDBC接口进一步嗅探出数据库版本和JDBC驱动信息，从而让方言更加适配数据库操作。
	 * 当有一个数据库实例连接初次创建时调用. Dialect可以通过直接连接数据库判断版本、函数等，调整Dialect内部的一些配置和数据。
	 * @param db
	 */
	void accept(DbMetaData db);

	/**
	 * 将插入语句转化为最快操作的语句
	 * @param sql
	 */
	void toExtremeInsert(InsertSqlClause sql);
	
	/**
	 * 获得SQL解析器。目前内置了两套解析器，一套是作者基于JavaCC自行编写的，性能较差。
	 * 一套是直接使用Druid的SQL解析器。Druid解析器是分不同的数据库语法的，因此要根据数据库类型来获得对应的解析器
	 * @return
	 */
	ParserFactory getParserFactory();
	
	/**
	 * 从异常信息中解析出约束冲突的信息。此处返回约束冲突的解析器
	 * @return
	 * @see ViolatedConstraintNameExtracter
	 */
	ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter();
	
	/**
	 * 数据库对象是否为大小写敏感的
	 * 一般来说对应引号中的表名列名都是大小写敏感的。此处仅指没有引号的情况下是否大小写敏感
	 * @return
	 */
	boolean isCaseSensitive();
	
	/**
	 * 当支持QueryDSL-SQL时，获得QueryDSL的Dialect
	 */
	SQLTemplates getQueryDslDialect();
}
