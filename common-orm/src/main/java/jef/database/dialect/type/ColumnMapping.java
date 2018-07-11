package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.MetadataContainer;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.tools.reflect.Property;

/**
 * 描述一个数据库列 映射到java字段上的模型信息
 * 
 * @author Jiyi
 * 
 * @param <T>
 *            该列在java中映射的数据类型
 *            
 *            
 * @Modify
 * 2014-10-31 为了实现在重构中，内部对于Field对象的表示逐渐过渡为  ColumnMapping对象，暂时先让ColumnMapping实现Field接口。         
 */
public interface ColumnMapping extends ResultSetAccessor,MetadataContainer {
	/**
	 * 得到数据库中的列定义
	 * @return ColumnType
	 */
	ColumnType get();

	/**
	 * 得到默认未设置或修饰过的值
	 * @return
	 */
	boolean isUnsavedValue(Object value);
	
	/**
	 * 是否显式的使用@UnsavedValue注解
	 * @return
	 */
	boolean isUnsavedValueDeclared();
	

	/**
	 * 该字段是否为LOB字段
	 * 
	 * @return true if the column is lob.
	 */
	boolean isLob();
	/**
	 * 是否为自动生成数值
	 * @return
	 */
	boolean isGenerated();
	
	/**
	 * 该字段不参与插入
	 * @return
	 */
	boolean isNotInsert();
	
	/**
	 * 该字段不参与更新
	 * @return
	 */
	boolean isNotUpdate();
	
	/**
	 * 设置绑定变量
	 * 
	 * @param st
	 *            JDBC PreparedStatement
	 * @param value
	 *            绑定变量的值(有可能为null)
	 * @param index
	 *            绑定变量序号
	 * @param dialect
	 *            数据库方言
	 * @return 实际被设置到Statement中的值。<br>
	 *         许多数据类型在设置到JDBC
	 *         Statement中时需要转换类型，例如java.util.Date需要转换为java.sql.Date。
	 * @throws SQLException
	 */
	Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException;

	/**
	 * 为了支持悲观锁中的数据类型转换
	 * @param rs
	 * @param value
	 * @param dialect
	 * @return
	 * @throws SQLException
	 */
	void jdbcUpdate(ResultSet rs, String columnIndex,Object value, DatabaseDialect dialect) throws SQLException;
	
	/**
	 * 得到java类型
	 * 
	 * @return java字段类型
	 */
	Class<?> getFieldType();

	/**
	 * java字段名
	 * 
	 * @return java字段名
	 */
	String fieldName();

	/**
	 * 原生的列名
	 * @return
	 */
	String rawColumnName();
	
	/**
	 * 数据库列名，小写，不转义
	 * 
	 * @return 数据库列名
	 */
	String lowerColumnName();
	
	/**
	 * 数据库列名，大写，不转义
	 * @return
	 */
	String upperColumnName();

	/**
	 * 返回在指定数据库环境下使用的列名（大小写，转义）
	 * 
	 * @param dialect
	 *            数据库方言
	 * @param escape
	 *            是否转义
	 * @return
	 */
	String getColumnName(DatabaseDialect dialect, boolean escape);

	/**
	 * field对象
	 * 
	 * @return FIeld对象
	 */
	Field field();

	/**
	 * get tableMetadata
	 * 
	 * @return 该列所属的表的的模型
	 */
	ITableMetadata getMeta();

	/**
	 * 返回该列在JDBC的数据库类型常量中定义的值。该值参见类{@link java.sql.Types}
	 * 
	 * @return JDBC数据类型
	 * @see java.sql.types
	 */
	int getSqlType();

	/**
	 * Is the column a promary key of table.
	 * 
	 * @return true is is promary key.
	 */
	boolean isPk();

	/**
	 * 返回在非绑定语句下的SQL表达式字符串<br>
	 * 将在后续版本中取消非绑定变量的操作方式，以后此方法可能无需实现。
	 * @param value
	 *            值
	 * @param profile
	 *            方言
	 * @return 在SQL语句中该值的写法。
	 *  
	 */
	String getSqlStr(Object value, DatabaseDialect profile);

	/**
	 * (框架使用)当执行插入时(绑定变量)，该字段的处理拼到InsertSqlResult对象上去，形成SQL语句的逻辑
	 * 
	 * @param obj
	 *            被插入数据库的对象
	 * @param cStr
	 *            column的SQL部分
	 * @param vStr
	 *            value的SQL部分
	 * @param result
	 *            InsertSqlResult,拼凑中的SQL语句描述
	 * @param dynamic
	 *            dynamic
	 *            是否dynamic模式插入，dynamic模式下没有设置过的字段不出现在SQL语句中，从而可以使用数据库中的默认值
	 * @throws SQLException
	 */
	void processPreparedInsert(IQueryableEntity obj, List<String> cStr, List<String> vStr, InsertSqlClause result, boolean dynamic) throws SQLException;

	/**
	 * （框架使用）构造后的初始化
	 * 
	 * @param field
	 *            field元模型
	 * @param columnName
	 *            列名
	 * @param type
	 *            字段类型
	 * @param meta
	 *            所属的表模型
	 *  //@deprecated 倾向于在创建时，传传入AnnotationProvider一次性完成处理逻辑。
	 */
	void init(Field field, String columnName, ColumnType type, ITableMetadata meta);
	
	/**
	 * 设置，是否为主键
	 * @param b
	 */
	void setPk(boolean b);
	
	/**
	 * 获得字段访问器
	 * @return
	 */
	public Property getFieldAccessor();
}
