package jef.database.meta;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;

import jef.accelerator.bean.BeanAccessor;
import jef.common.Entry;
import jef.database.Field;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.VersionSupportColumn;
import jef.database.meta.def.IndexDef;
import jef.database.meta.def.UniqueConstraintDef;
import jef.tools.reflect.BooleanProperty;
import jef.tools.reflect.Property;

/**
 * 表的元模型
 * 
 * <h3>什么是元模型</h3> 元模型<meta-model>是一张数据库表的结构在ORM中的描述。<br>
 * 元模型是一个Java对象，这个对象中存放了数据库表的各种字段、类型等结构定义。 我们可以通过元模型来指定一张表和这张表对应的Java映射类型。
 * 
 * <h3>元模型的获得</h3> 一般来说，我们会使用一个类对应数据库中的一张表。比如类 jef.orm.test.Person对应数据库中的
 * PERSON表。 我们可以用以下方法来获得这个表的元模型。
 * 
 * <pre>
 * <tt>
 *  ITableMetadata metaModel=MetaHolder.getMeta(jef.orm.test.Person.class)
 *  </tt>
 * </pre>
 * 
 * <h3>元模型的种类</h3> 在EF-ORM中，有三种不同的元模型。 {@link #getType()}
 * <li>{@link EntityType#NATIVE}<br>
 * 基础映射，一个class对应一张表。<br>
 * 这个class必须实现jef.database.IQueryableEntity接口。这也是EF-ORM中标准的O-R映射方式<br>
 * </li>
 * <li>{@link EntityType#TUPLE}<br>
 * 动态的映射，表没有对应的java类对应，而是用一个类似于Map的结构（{@link jef.database.VarObject}）来对应。</li>
 * <li>{@link EntityType#POJO}<br>
 * 扩展功能，为了支持一些简单的单表CRUD操作，<br>
 * EF-ORM也可以让一些POJO类映射到数据库表，提供基本的对象操作功能。</li>
 * 
 * @author Jiyi
 * 
 */
@SuppressWarnings("rawtypes")
public interface ITableMetadata {
	/**
	 * 得到此表对应的模型类。对于基本类型来说，模型类型和容器类型都是一致的。
	 * 
	 * @return 对应的模型类。
	 */
	Class<?> getThisType();

	/**
	 * 返回class名称
	 * 
	 * @return class名称
	 */
	String getName();

	/**
	 * 返回class名称Simple
	 * 
	 * @return class Simple名称
	 */
	String getSimpleName();
	

	/**
	 * 得到所有数据库字段的名称。（注意，不包含各种映射字段等非数据库字段的名称）
	 * 
	 * @return 所有数据库字段的名称
	 */
	Set<String> getAllFieldNames();

	/**
	 * 得到实体的类型，有三种值
	 * 
	 * @return 实体的类型
	 */
	EntityType getType();

	/**
	 * 得到该对象绑定的数据源名（重定向后）<br>
	 * ORM允许用户使用Annotation @BindDataSource("db2") 添加在实体类上，指定该实体操作绑定特定的数据源。<br>
	 * 建模时的数据源，在实际运行环境中经过重定向后变为实际部署的数据源名称。<br>
	 * ORM会将对这张表的操作全部在这个数据源上执行。
	 * 
	 * @return 重定向后的数据源名称。
	 * @see jef.database.annotation.BindDataSource
	 */
	String getBindDsName();

	/**
	 * 得到schema名称。 ORM允许用户使用Annotation @Table(schema="S1")
	 * 添加在实体类上，指定该实体操作位于特定的schema上。<br>
	 * 建模时的schema，在实际运行环境中经过重定向后变为实际部署的数据源名称。<br>
	 * 
	 * @return 重定向后的schema
	 */
	String getSchema();

	/**
	 * 返回表名
	 * 
	 * @param withSchema
	 *            true要求带schema，schema是已经经过了重定向的名称
	 * @return 返回表名，如果实体绑定了schema，并且withSchema为true,那么返回形如
	 *         <code>schema.table</code>的名称
	 */
	String getTableName(boolean withSchema);

	/**
	 * 返回所有的元模型字段和类型。这些字段的顺序会进行调整，Clob和Blob将会被放在最后。 这些字段的顺序一旦确定那么就是固定的。
	 * 
	 * 注意当元数据未初始化完成前，不要调用这个方法。
	 * 
	 * @return
	 */
	Collection<ColumnMapping> getColumns();
	

	/**
	 * 根据名称得到一个Field对象（大小写敏感）
	 * 
	 * @param name
	 *            字段名
	 * @return Field对象(字段元模型)
	 * 
	 */
	ColumnMapping getColumnDef(String fieldname);


	/**
	 * 获取字段的元数据定义
	 * 
	 * @return MappingType,包含了该字段的数据库列名、java字段名、类型等各种信息。
	 * @see ColumnMapping
	 */
	ColumnMapping getColumnDef(Field field);

	/**
	 * 根据字段序获得字段定义
	 * @param i 字段序
	 * @return ColumnMapping
	 */
	ColumnMapping getColumnDef(int i);
	

	/**
	 * 根据数据库列名（小写的）获得field对象 注意传入的列名必须保持小写。
	 * 
	 * @param columnInLowerCase
	 *            数据库列名的小写
	 * @return FIeld对象(字段元模型)
	 */
	ColumnMapping getFieldByLowerColumn(String columnInLowerCase);
	
	///////////////////////////////////////////
	/**
	 * 得到扩展属性的存放表结构
	 * 
	 * @return
	 */
	TupleMetadata getExtendsTable();

	/**
	 * 得到所有的扩展属性字段
	 * 
	 * @return
	 */
	Collection<ColumnMapping> getExtendedColumns();

	/**
	 * 得到指定名称的扩展属性字段
	 * 
	 * @param field
	 * @return
	 */
	ColumnMapping getExtendedColumnDef(String field);
	
	//////////////////////////////////////

	/**
	 * 返回所有自增字段的定义，如果没有则返回空数组
	 * 
	 * @return 所有自增字段的定义
	 */
	AutoIncrementMapping[] getAutoincrementDef();

	/**
	 * 返回第一个自增字段的定义，如果没有则返回null
	 * 
	 * @return 返回第一个自增字段的定义
	 */
	AutoIncrementMapping getFirstAutoincrementDef();

	/**
	 * 需要自动维护数据的列定义（每次更新时自动刷新的列，例如版本或修改时间）
	 * 
	 * @return 需要自动维护记录更新的列定义
	 */
	VersionSupportColumn[] getAutoUpdateColumnDef();

	/**
	 * 获得用于进行版本控制（防止并发修改冲突）的列定义
	 * 
	 * @return the VersionSupportColumn with version usage
	 */
	VersionSupportColumn getVersionColumn();

	////////////////////////////////////////////////////////////////
	/**
	 * 获取被设置为主键的字段
	 * 
	 * @return
	 */
	List<ColumnMapping> getPKFields();

	/**
	 * 获取索引的元数据定义
	 * 
	 * @return 索引的定义
	 */
	List<IndexDef> getIndexDefinition();

	/**
	 * 得到所有的unique约束
	 * 
	 * @return
	 */
	List<UniqueConstraintDef> getUniqueDefinitions();

	// ///////////////////////引用关联查询相关////////////////////
	/**
	 * 按照引用的关系获取所有关联字段
	 * 
	 * @return 关联关系字段
	 */
	Map<Reference, List<AbstractRefField>> getRefFieldsByRef();

	/**
	 * 按照名称获得所有关联字段
	 * 
	 * @return 关联关系字段
	 */
	Map<String, jef.database.meta.AbstractRefField> getRefFieldsByName();


	/**
	 * 根据名称得到一个Field对象（大小写不敏感） 如果找不到将返回null(不抛出异常)
	 * 
	 * @param name
	 * @return Field对象
	 */
	ColumnMapping findField(String left);


	// /////////////////////////分区分库分表相关////////////////////

	/**
	 * 获得分表定义
	 */
	PartitionTable getPartition();

	/**
	 * 获取当前生效的分区策略
	 * 注意生效的策略默认等同于Annotation上的策略，但是实际上如果配置了/partition-conf.properties后
	 * ，生效字段受改配置影响 {@link #partitPolicy}
	 * 
	 * @return 当前生效的分区策略
	 */
	Entry<PartitionKey, PartitionFunction>[] getEffectPartitionKeys();

	/**
	 * 获得每个字段上，最小单位的分表函数。 也就是说，其实一个字段上可以对应多个Key，例如有一个Date birthDay 然后KeyA
	 * function=YEAR KeyB function=MONTH 此时在计算枚举时按月计算即可。前一个条件可以忽略。
	 * 
	 * @return 最小单位的分表函数
	 */
	Multimap<String, PartitionFunction> getMinUnitFuncForEachPartitionKey();


	// /////////////////////////////// 反射与访问 ///////////////////////////

	/**
	 * 内部使用，得到Bean访问器
	 * 
	 * @return
	 */
	BeanAccessor getContainerAccessor();

	/**
	 * 创建一个实例
	 * 在某些情况下个getContainerAccessor().newInstance()不同，因此getContainerAccessor应当仅用于存取字段值，不应用于创建实例
	 * 
	 * @return 创建的实例
	 */
	Object newInstance();

	// ///////////////////////////// 其他行为 //////////////////////////////
	/**
	 * 返回所有的Lob字段
	 * 
	 * @return 所有Lob Field
	 */
	Field[] getLobFieldNames();

	/**
	 * 由于Entity可以互相继承，引起了metadata也可以继承。
	 * 
	 * @param meta
	 *            要检测的meta
	 * @return 如果当前模型等于meta，或者当前模型继承了meta，返回true
	 */
	boolean containsMeta(ITableMetadata meta);

	/////////////// 基于KV表扩展的设计//////////////

	boolean isCacheable();

	boolean isUseOuterJoin();

	Map<String, String> getColumnComments();

	///============================================================
	/**
	 * 获得TouchRecord的FieldAccessor。 a BitSet to record wheather a field was set
	 * or not.
	 */
	Property getTouchRecord();

	/**
	 * 获得Touch的FieldAccessor a boolean flag to describe wheather touch have to
	 * recorded or not
	 */
	BooleanProperty getTouchIgnoreFlag();
	/**
	 * 获得LazyContext的FieldAccessor a LazyLoadContext
	 */
	Property getLazyAccessor();
	


	/**
	 * 判断指定需要的字段是否被Touch过。 序号是字段是顺序，该顺序在Entity初始化的时候被定义。
	 */
	default boolean isTouched(Object obj, int index) {
		Property accessor = getTouchRecord();
		BitSet result = (BitSet) accessor.get(obj);
		return result == null ? false : result.get(index);
	}
}
