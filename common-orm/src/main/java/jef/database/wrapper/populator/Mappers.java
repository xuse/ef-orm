package jef.database.wrapper.populator;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.DbmsProfile;
import jef.database.dialect.type.ColumnMappings;
import jef.database.dialect.type.MappingType;
import jef.database.dialect.type.ResultSetAccessor;
import jef.database.innerpool.ArrayElementPopulator;
import jef.database.innerpool.NestedObjectPopulator;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.meta.AliasProvider;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.support.RDBMS;
import jef.database.wrapper.result.IResultSet;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * Mapper工具类，用于生成一些默认的Mapper对象。
 * 用户可以提供一个Class，生成这个class和数据库列的映射关系。
 * 个别字段不一致的，用户可以微调。
 * 
 * 
 * <p>
 * 提供了这三类的自动映射关系的生成器
 * <p>
 * <li>{@link #toResultBean} 将字段映射后直接赋值到对象里</li> <li>{@link #toResultProperty}
 * 将映射后的对象放入返回对象的某个字段里（比如父子关系的表）</li> <li>{@link #toArrayElement}
 * 将映射后的对象放入数组中（一次返回多个对象时使用数组）</li>
 * 
 * 
 * @author jiyi
 * @see BeanMapper
 */
public final class Mappers {
	private Mappers() {
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据填入对象中，该对象是最终结果的成员变量。<br />
	 * 
	 * @param fieldName
	 *            The fieldname where the transformed bean will set into.
	 * @param clz
	 *            the type of transformed bean
	 * @param schema
	 *            the prefix of the alias in SQL 'select ' items. such as in
	 *            'select id as T1__ID ...', here 'T1' is the schema. we use
	 *            "__" as a divider between schema and column names.
	 *            <p>
	 * @return The Mapper
	 * 
	 * @Example <pre>
	 * 
	 *          <code> public class Student{ private Teacher teacher;
	 * 
	 *          ... getters and setters ... }
	 * 
	 *          //query SQL NativeQuery<Student>
	 *          query=db.createNativeQuery("select s.*," +
	 *          "teacher_id as T2__TEACHER_ID," + "teacher_name as T2__TEACHER"
	 *          + "from Student s, Teacher t2 where s.teacher_id=t2.thacher_id",
	 *          Student.class);
	 * 
	 *          //use 'ResultTransformer' and 'Mappings' to transform tacher
	 *          info into student..
	 *          //NOTE: teacher's info will transform to the field 'teacher'
	 *          into Student.
	 *          query.getResultTransformer().addMapper(Mappers.toResultProperty(
	 *          "teacher", Teacher.class, "T2")); </code>
	 * 
	 *          </pre>
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toResultProperty(String fieldName, Class<T> clz, String schema) {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		return toResultProperty(fieldName, meta, schema);
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据填入对象中，该对象是最终结果的成员变量。<br />
	 * 
	 * @param fieldName
	 *            The fieldname where the transformed bean will set into.
	 * @param meta
	 *            the table metadata of transformed bean
	 * @param schema
	 *            the prefix of the alias in SQL 'select ' items. such as in
	 *            'select id as T1__ID ...', here 'T1' is the schema. we use
	 *            "__" as a divider between the schema and column name.
	 *            <p>
	 * @return The Mapper
	 * 
	 * @Example <pre>
	 * 
	 *          <code> public class Student{ private Teacher teacher;
	 * 
	 *          ... getters and setters ... }
	 * 
	 *          //query SQL NativeQuery<Student>
	 *          query=db.createNativeQuery("select s.*," +
	 *          "teacher_id as T2__TEACHER_ID," + "teacher_name as T2__TEACHER"
	 *          + "from Student s, Teacher t2 where s.teacher_id=t2.thacher_id",
	 *          Student.class);
	 * 
	 *          //use 'ResultTransformer' and 'Mappings' to transform tacher
	 *          info into student..
	 *          //NOTE: teacher's info will transform to the field 'teacher'
	 *          into Student. ITableMetadata meta=MetaHolder.get(Teacher.class);
	 *          query.getResultTransformer().addMapper(Mappers.toResultProperty(
	 *          "teacher", meta, "T2")); </code>
	 * 
	 *          </pre>
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toResultProperty(String fieldName, ITableMetadata meta, String schema) {
		BeanMapper<T> result = new BeanMapper<T>(meta);
		result.schema = schema;
		result.toField = fieldName;
		return result;
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据填入对象中，该对象是最终结果的成员变量。<br />
	 * <h3>Eg.</h3>
	 * 
	 * <pre>
	 * <code>// The Object Model
	 *   public class Student{
	 *       private Teacher teacher;   //Note: there is a field named 'teacher' in class Student.
	 * 
	 *       ... getters and setters ...
	 *   }
	 * 
	 *  //query SQL —— Note: all column in result set are mapping to class student except column 'teacher_id' and 'teacher_name'. 
	 *   NativeQuery<Student> query=db.createNativeQuery("select s.*,t.teacher_id,t.teacher_name from Student s, Teacher t where s.teacher_id=t.thacher_id", Student.class);
	 *   
	 *   
	 *  //use 'ResultTransformer' and 'Mappings' to transform tacher info into student..
	 *  //The ORM will fetch 'teacher_id' and 'teacher_name' column into a Teacher Object.
	 *  //NOTE: There is a field named 'teacher' in class 'Student', the teacher's info will be set to this field. 
	 *  query.getResultTransformer().addMapper(Mappers.toResultProperty("teacher", Teacher.class));  
	 * </code>
	 * </pre>
	 * <h3>使用举例（中文版）</h3>
	 *  <pre>
	 * <code>// 对象模型
	 *   public class Student{
	 *       private Teacher teacher;   //Note: there is a field named 'teacher' in class Student.
	 * 
	 *       ... getters and setters ...
	 *   }
	 * 
	 *  //查询 SQL —— Note: all column in result set are mapping to class student except column 'teacher_id' and 'teacher_name'. 
	 *   NativeQuery<Student> query=db.createNativeQuery("select s.*,t.teacher_id,t.teacher_name from Student s, Teacher t where s.teacher_id=t.thacher_id", Student.class);
	 *   
	 *   
	 *  //use 'ResultTransformer' and 'Mappings' to transform tacher info into student..
	 *  //The ORM will fetch 'teacher_id' and 'teacher_name' column into a Teacher Object.
	 *  //NOTE: There is a field named 'teacher' in class 'Student', the teacher's info will be set to this field. 
	 *  query.getResultTransformer().addMapper(Mappers.toResultProperty("teacher", Teacher.class));  
	 * </code>
	 * </pre>
	 * 
	 * Above is the usega of #toResultProperty
	 * 
	 * @param fieldName
	 * @param clz
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toResultProperty(String fieldName, Class<T> clz) {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		return toResultProperty(fieldName, meta, null);
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据填入对象中<br />
	 * 
	 * @param clz
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toResultBean(Class<T> clz) {
		return new BeanMapper<T>(MetaHolder.getMeta(clz));
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据填入对象中<br />
	 * 
	 * @param clz
	 * @param schema
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toResultBean(Class<T> clz, String schema) {
		BeanMapper<T> result = new BeanMapper<T>(MetaHolder.getMeta(clz));
		result.schema = schema;
		return result;
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据填入对象中<br />
	 * 
	 * @param meta
	 * @param schema
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toResultBean(ITableMetadata meta, String schema) {
		BeanMapper<T> result = new BeanMapper<T>(meta);
		result.schema = schema;
		return result;
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据拼装为Object后填入数组中<br />
	 * <h3>使用场景</h3> 使用自定义的查询查出多个对象，并将这些对象用数组的形式成对返回。
	 * 
	 * <h3>用法</h3> ...
	 * 
	 * @param index
	 *            数组中的序号，从0开始
	 * @param clz
	 *            数据类型
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toArrayElement(int index, Class<T> clz) {
		return toResultProperty(String.valueOf(index), clz, null);
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据拼装为Object后填入数组中<br />
	 * 
	 * @param index
	 *            对象数组的序号，从0开始
	 * @param clz
	 *            对象类型
	 * @param schema
	 *            对象所在的表alias
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toArrayElement(int index, Class<T> clz, String schema) {
		return toResultProperty(String.valueOf(index), clz, schema);
	}

	/**
	 * 转换器：将ResultSet中名称和clz的列一致的数据拼装为Object后填入数组中<br />
	 * <h3>使用场景</h3> 使用自定义的查询查出多个对象，并将这些对象用数组的形式成对返回。
	 * 
	 * <h3>用法</h3> ...
	 * 
	 * @param index
	 *            对象数组的序号，从0开始
	 * @param meta
	 *            对象类型
	 * @param schema
	 *            对象所在的表alias
	 * @return Mapper对象
	 */
	public static final <T extends IQueryableEntity> BeanMapper<T> toArrayElement(int index, ITableMetadata meta, String schema) {
		return toResultProperty(String.valueOf(index), meta, schema);
	}

	/**
	 * 将一个java数据模型和数据库中的列自动映射起来。然后允许用户通过{@link #adjust(String, String)}方法微调映射关系。
	 * @author jiyi
	 *
	 * @param <T>
	 */
	public final static class BeanMapper<T extends IQueryableEntity> extends Mapper<T> {
		String toField;
		private ITableMetadata meta;
		private String schema;
		private boolean skipColumnAnnotation;
		private IPopulator populator;
		private Map<String,String> customMap=Collections.EMPTY_MAP; //指定对象属性 -> 数据库列定制 (字段属性全大写。数据库列全大写)

		/**
		 * 映射关系微调
		 * @param field 在java中的属性名
		 * @param column 在数据库中的列名
		 * @return BeanMapper本身
		 */
		public BeanMapper<T> adjust(String field,String column){
			Assert.notNull(field);
			Assert.notNull(column);
			if(customMap==Collections.EMPTY_MAP){
				customMap=new HashMap<String,String>();
			}
			customMap.put(field.toUpperCase(), column.toUpperCase());
			return this;
		}
		
		public BeanMapper(ITableMetadata meta) {
			this.meta = meta;
		}

		@Override
		public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
			populator.process(wrapper, rs);
		}

		@Override
		protected void prepare(Map<String, ColumnDescription> nameIndex) {
			HashMap<String, ColumnDescription> data = new HashMap<String, ColumnDescription>();
			if (meta == null) {
				throw new IllegalArgumentException("the table metadata is null!");
			}
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(meta.getThisType());
			for (MappingType<?> ft : meta.getMetaFields()) {
				Field f = ft.field();
				String columnName=customMap.get(f.name().toUpperCase());
				if(columnName==null){
					if (schema == null) {
						columnName = skipColumnAnnotation ? f.name().toUpperCase() : meta.getColumnName(f, UPPER_COLUMNS, false);
					} else {
						columnName = AliasProvider.DEFAULT.getSelectedAliasOf(f, UPPER_COLUMNS, schema);
					}	
				}
				if (columnName != null) {
					ColumnDescription columnDesc = nameIndex.get(columnName);
					if (columnDesc == null) {
						if (schema == "")
							System.err.println("Warnning: populating object " + meta.getThisType() + " error," + schema + ":" + columnName + " not found in the selected columns");
					} else {
						ResultSetAccessor accessor = ColumnMappings.getAccessor(ba.getPropertyType(f.name()), ft, columnDesc, false);
						columnDesc.setAccessor(accessor);
						data.put(f.name(), columnDesc);
					}
				}
			}
			if (StringUtils.isNotEmpty(toField)) {
				if (StringUtils.isNumeric(toField)) {
					int index = StringUtils.toInt(toField, null);
					this.populator = new ArrayElementPopulator(index, new ObjectPopulator(meta, data));
				} else {
					this.populator = new NestedObjectPopulator(toField, new ObjectPopulator(meta, data));
				}
			} else {
				this.populator = new ObjectPopulator(meta, data);
			}
		}

		protected void transform(T obj, IResultSet rs) throws SQLException {
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * 内部使用，用于将列名一律转为大写
	 */
	public static DatabaseDialect UPPER_COLUMNS = new DbmsProfile() {
		public RDBMS getName() {
			return null;
		}

		public String getGeneratedFetchFunction() {
			throw new UnsupportedOperationException();
		}

		public String getDriverClass(String url) {
			throw new UnsupportedOperationException();
		}

		public String toPageSQL(String sql, IntRange range) {
			throw new UnsupportedOperationException();
		}

		public void parseDbInfo(ConnectInfo connectInfo) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getColumnNameIncase(String name) {
			return name == null ? null : name.toUpperCase();
		}

		public void addKeyword(String... keys) {
		}

		public Select toPageSQL(Select select, IntRange range) {
			// TODO Auto-generated method stub
			return null;
		}
	};
}
