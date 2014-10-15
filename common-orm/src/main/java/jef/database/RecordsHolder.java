package jef.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.GenerationType;
import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoGuidMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.AutoIntMapping;
import jef.database.dialect.type.AutoLongMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleField;
import jef.database.wrapper.result.ResultSetWrapper;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * 一个可以更新数据的结果集，我们可以直接在结果集上更新数据。
 * 代码示例如下:
 * <pre><code>
 * 
 * RecordsHolder<Root> holder = db.selectForUpdate(QB.create(Root.class).getInstance());
 * System.out.println("总数:" + holder.size());
 * int n = 0;
 * for (Root r : holder.get()) {
 * r.setName("更新第" + n + "条。"); // 修改对象中的值
 * n++;
 *	}
 * holder.delete(holder.size() - 1); // 删除结果集中的最后一条记录(序号从0开始)
 * Root root = holder.newRecord(); // 创建一条新纪录
 * root.setName("新插入的记录");
 * holder.commit(); // 提交上述修改并关闭游标（更新、删除、添加）
 * </code></pre>
 * @author jiyi
 *
 * @param <T>
 */
public final class RecordsHolder<T extends IQueryableEntity>{
	public static final int BEFORE_FIRST = -2;
	public static final int AFTER_LAST = -3;
	
	private DatabaseDialect profile;

	private ResultSetWrapper rs;//必须是一个允许前后滚动的结果集
	
	private List<RecordHolder<T>> rhs;
	private List<T> objs;
	private boolean noHoldInsertValues=true;//HSQL has a feature, if you move the cursor to InsertRow, then you can not move it to any other rows. so we must pend inert request to last.
	private boolean supportsNewRec=true;//Derby BUG, 用结果集直接插入记录后，Derby的自增主键不会同步增长，造成后续自增主键出现冲突。
	private ITableMetadata meta;
	private int index=BEFORE_FIRST;
	
	/**
	 * 返回结果的条数
	 * @return
	 */
	public int size(){
		ensureOpen();
		return objs.size();
	}
	/**
	 * 返回结果集
	 * @return
	 */
	public List<T> get(){
		ensureOpen();
		return objs;
	}
	
	RecordHolder<T> get(int i){
		ensureOpen();
		return rhs.get(i);
	}
	
	RecordsHolder(ITableMetadata mm) throws SQLException{
		this.meta=mm;
		Assert.notNull(meta);
	}
	
	void init(ResultSetWrapper holder,List<T> objs,DatabaseDialect profile){
		this.profile= profile;
		this.rs= holder;
		this.objs=objs;
		rhs=new ArrayList<RecordHolder<T>>(); 
		for(int i=0;i<objs.size();i++){
			rhs.add(new RecordHolder<T>(this, i, objs.get(i)));
		}		
		if(holder.getProfile().has(Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD)){
			for(ColumnMapping<?> type:meta.getPKFields()){
				if(type instanceof AutoIntMapping || type instanceof AutoLongMapping){
					supportsNewRec=false;		
				}
			}	
		}
		noHoldInsertValues=holder.getProfile().notHas(Feature.CURSOR_ENDS_ON_INSERT_ROW);
	}
	
	/**
	 * 一些数据库不支持在ResultSet上新建记录，返回该标志
	 * @return
	 */
	public boolean supportsNewRecord(){
		return supportsNewRec;
	}
	
	/**
	 * 创建一个新记录
	 * @return
	 */
	public T newRecord(){
		ensureOpen();
		if(!supportsNewRec){
			throw new UnsupportedOperationException("Current database "+ rs.getProfile().getName()+" not support 'newRecord()'");
		}
		@SuppressWarnings("unchecked")
		T obj= (T) meta.newInstance();
		if(!meta.getPKFields().isEmpty()){
			for(ColumnMapping<?> type:meta.getPKFields()){
				if(type instanceof AutoIncrementMapping<?>){
					AutoIncrementMapping<?> mapping=(AutoIncrementMapping<?>)type;
					mapping.getAccessor().set(obj, getNextAutoIncreament(mapping));
				}else if(type instanceof AutoGuidMapping){
					BeanWrapper bean=BeanWrapper.wrap(obj,BeanWrapper.FAST);
					String value = StringUtils.remove(StringUtils.generateGuid(), '-');
					bean.setPropertyValue(type.fieldName(), value);
				}
			}	
		}
		//准备完成，加入
		RecordHolder<T> r=new RecordHolder<T>(this, -1, obj);
		this.rhs.add(r);
		return obj;
	}
	
	/*
	 * 计算自增值
	 * @param mapping
	 * @return
	 */
	private long getNextAutoIncreament(AutoIncrementMapping<?> mapping){
		GenerationType gtype=mapping.getGenerationType(profile);
		if(gtype==GenerationType.SEQUENCE || gtype==GenerationType.TABLE){
			try{
				Sequence sq=rs.getTarget().getSequence(mapping);
				return sq.next();
			}catch(SQLException e){
				throw new PersistenceException(e);
			}	
		}else{
			return profile.getColumnAutoIncreamentValue(mapping,this.rs.getTarget());
		}
	}
	
	/**
	 * 检查指定的对象是否删除
	 * @param index 序号，从0开始
	 * @return
	 */
	public boolean isDeleted(int index){
		ensureOpen();
		if(index<0 || index>=size()){
			throw new IllegalArgumentException("the object you want to delete is not exist in the list");
		}
		RecordHolder<T> r=rhs.get(index);
		return r.status==RecordHolder.DELETE || r.status==RecordHolder.DELETED;
	}
	
	/**
	 * 删除
	 * @param index 在结果集中的序号
	 */
	public void delete(int index){
		ensureOpen();
		if(index<0 || index>=size()){
			throw new IllegalArgumentException("the object you want to delete is not exist in the list");
		}
		RecordHolder<T> r=rhs.get(index);
		Assert.notNull(r);
		r.status=RecordHolder.DELETE;
	}
	
	/**
	 * 删除指定的记录
	 * @param object 删除结果集当中指定的对象
	 */
	public void delete(T object){
		ensureOpen();
		int index=objs.indexOf(object);
		delete(index);
	}
	
	/**
	 * 将修改提交(更新到数据库)
	 * @param closeit 提交后关闭ResultSet
	 * @throws SQLException
	 */
	public void commit(boolean closeit) throws SQLException{
		ensureOpen();
		moveTo(BEFORE_FIRST);
		List<RecordHolder<T>> toInsert=new ArrayList<RecordHolder<T>>();
		for(RecordHolder<T> r: rhs){
			try{
				switch(r.getStatus()){
				case RecordHolder.CACNLED://不用操作
					break;
				case RecordHolder.DELETE:
					moveTo(r.index);
					rs.deleteRow();
					break;
				case RecordHolder.DELETED://不用操作
					break;
				case RecordHolder.INSERT:
					if(closeit || noHoldInsertValues){
						toInsert.add(r);
					}
					break;
				case RecordHolder.INSERTED://不用操作
					break;
				default:
					if(r.get().needUpdate()){
						update(r.get(),r.index);	
					}
				}
			}catch(SQLException e){
				LogUtil.error("Error update "+ r);
				throw e;
			}
		}
		//Insert最后再操作
		for(RecordHolder<T> r:toInsert){
			insert(r.get());
			r.setInserted();
		}
		if(closeit)close();
	}
	
	/**
	 * 将修改提交(更新到数据库)
	 * @throws SQLException
	 */
	public void commit() throws SQLException{
		commit(true);
	}
	
	private void update(T t, int index2) throws SQLException {
		if(index2<0 || index2>=size()){
			throw new SQLException("the index " + index2 +" is invalid!");
		}
		moveTo(index2);
		boolean flag=false;
		for(Field f:t.getUpdateValueMap().keySet()){
			if(f instanceof Enum<?> || f instanceof TupleField){
				Object value=t.getUpdateValueMap().get(f);
				if(value instanceof Expression){
					throw new SQLException("The expression object not supported in resultSet operation model.");
				}
				String columnName=meta.getColumnName(f, profile,false);
				if(value==null){
					rs.updateNull(columnName);
				}else{
					rs.updateObject(columnName, value);
				}
				
				
				flag=true;	
			}
		}
		if(flag)
			rs.updateRow();
	}

	private void insert(T t) throws SQLException {
		rs.moveToInsertRow();
		Assert.notNull(meta);
		BeanWrapper bw=BeanWrapper.wrap(t);
		for(ColumnMapping<?> mType: meta.getMetaFields()){
			Field f = mType.field();
			String columnName=mType.getColumnName(profile, false);
			if(!bw.isReadableProperty(f.name()))continue;
			Object value= bw.getPropertyValue(f.name());
			if(value==null){
				rs.updateNull(columnName);
			}else{
				rs.updateObject(columnName, value);
			}
		}
		rs.insertRow();
		rs.moveToInsertRow();
	}

	private void moveTo(int to) throws SQLException {
//		rs.moveToInsertRow();
		if(to==index)return;
		if(to==BEFORE_FIRST){
			rs.beforeFirst();
			this.index=to;
		}else if(to==AFTER_LAST){
			rs.afterLast();
			this.index=to;
		}else if(index==AFTER_LAST){
			rs.first();
			this.index=0;
		}else{//to>-1
			if(index==AFTER_LAST || index==BEFORE_FIRST){
				rs.first();
				index=0;
			}
			while(index<to){
				boolean flag=rs.next();
				index++;
				if(!flag)break;
			}
			while(index>to){
				boolean flag=rs.previous();
				index--;
				if(!flag)break;
			}
			if(index==to)return;
			throw new RuntimeException("Can not move to index :"+index+"->" + to);
		}
	}
	
//	@Override
//	protected void finalize() throws Throwable {
//		close();
//	}

	private void ensureOpen(){
		if(rs==null){
			throw new IllegalArgumentException("The result set has been closed.");
		}
	}
	
	/**
	 * 关闭ResultSet和statement
	 * @throws SQLException
	 */
	public void close() throws SQLException{
		if(rs!=null){
			rs.close();
			rs=null;
		}
	}
}
