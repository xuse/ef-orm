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
package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.Property;

/**
 * 描述各种用于在插入后获取自增量字段值并更新到Bean中的回调方法
 * @author Administrator
 *
 */
public interface AutoIncreatmentCallBack{
	void callBefore(List<? extends IQueryableEntity> data, Session db) throws SQLException;
	
	void callAfter(List<? extends IQueryableEntity> data)throws SQLException;
	
	PreparedStatement doPrepareStatement(OperateTarget conn,String sql,String dbName) throws SQLException;
	
	int executeUpdate(Statement st,String sql) throws SQLException;

	

	
	
	/**
	 * 自生成主键处理策略：
	 * 值已经预先生成，只需要要插入后调用此类更新到Bean中
	 * 
	 * 此接口
	 * @author Administrator
	 */
	final static class SingleKeySetCallback implements AutoIncreatmentCallBack {
		private Property fieldName;
		private long key;
		private String sKey=null;

		//必须传入Long型的Property
		public SingleKeySetCallback(Property fieldName, long key) {
			this.fieldName = fieldName;
			this.key = key;
		}
		
		//必须传入Long型的Property
		public SingleKeySetCallback(Property fieldName, String key) {
			this.fieldName = fieldName;
			this.sKey = key;
		}

		public void callAfter(List<? extends IQueryableEntity> data) throws SQLException {
			Assert.isTrue(data.size() == 1);
			if(sKey==null){
				fieldName.set(data, key);
			}else{
				fieldName.set(data, sKey);
			}
		}
		public PreparedStatement doPrepareStatement(OperateTarget conn, String sql,String dbName) throws SQLException {
			return conn.prepareStatement(sql);
		}
		public int executeUpdate(Statement st, String sql) throws SQLException {
			return st.executeUpdate(sql);
		}
		public void callBefore(List<? extends IQueryableEntity> data, Session dbName) throws SQLException {
		}
	}

	/**
	 * 使用数据库提供的函数来尝试获得刚刚插入的对象的自增主键，只能支持单个对象
	 * 不太推荐，只有当驱动程序不支持JDBC 3的自动获取自增主键的接口，且没有更新的驱动程序时，可以使用此回调方法
	 * @author Administrator
	 */
	final static class DbmsFuncToFetchSingleKeyCallback implements AutoIncreatmentCallBack {
		private Property fieldName;
		private String functionSql;
		private Statement st;
		
		public DbmsFuncToFetchSingleKeyCallback(Property fieldName,String functionSql){
			Assert.isNotEmpty(functionSql);
			this.functionSql=functionSql;
			this.fieldName=fieldName;
		}
		public void callAfter(List<? extends IQueryableEntity> data) throws SQLException {
			Assert.isTrue(data.size() == 1);
			long generatedKey = -1;
			ResultSet rs = st.executeQuery(functionSql);
			try{
				rs.next();
				generatedKey = rs.getLong(1);
				if (generatedKey != -1) {
					fieldName.set(data.get(0), generatedKey);
				}
			}finally{
				rs.close();
			}
		}
		public PreparedStatement doPrepareStatement(OperateTarget conn, String sql,String dbName) throws SQLException {
			PreparedStatement pst= conn.prepareStatement(sql);
			this.st=pst;
			return pst;
		}
		public int executeUpdate(Statement st, String sql) throws SQLException {
			this.st=st;
			return st.executeUpdate(sql);
		}
		public void callBefore(List<? extends IQueryableEntity> data, Session dbName) throws SQLException {
		}
	}

	/**
	 * 对批量的Entity对象生成Sequence的主键，并赋值
	 * 这个回调方法要求在插入到数据库之前运行，直接更新Bean中的值。然后再用更新后的Bean插入数据库
	 * @author Administrator
	 */
	final static class SequenceGenerateCallback implements AutoIncreatmentCallBack {
		private Property field;
		private Sequence holder;
		
		public SequenceGenerateCallback(Property fieldName, Sequence holder) {
			Assert.notNull(holder);
			this.field = fieldName;
			this.holder = holder;
		}

		public void callBefore(List<? extends IQueryableEntity> data,Session db) throws SQLException {
			for (IQueryableEntity o : data) {
				long key=-1;
				key = holder.next();
				if(key>-1){
					field.set(o, key);
				}else{
					throw new SQLException("AutoIncreatment generate error.");
				}
			}	
		}
		public PreparedStatement doPrepareStatement(OperateTarget conn, String sql,String dbName) throws SQLException {
			return conn.prepareStatement(sql);
		}
		public int executeUpdate(Statement st, String sql) throws SQLException {
			return st.executeUpdate(sql);
		}
		public void callAfter(List<? extends IQueryableEntity> data) throws SQLException {
		}
	}
	
	/**
	 * 对批量的DO对象生成Sequence的主键，并赋值
	 * 这个回调方法要求在插入到数据库之前运行，直接更新Bean中的值。然后再用更新后的Bean插入数据库
	 * @author Administrator
	 */
	 static class GUIDGenerateCallback implements AutoIncreatmentCallBack {
		private Property field;
		private boolean removeDash;
		
		public GUIDGenerateCallback(Property fieldName,boolean b) {
			this.field = fieldName;
			this.removeDash=b;
		}
		
		public void callBefore(List<? extends IQueryableEntity> data, Session dbName) throws SQLException {
			for (IQueryableEntity o : data) {
				String key = UUID.randomUUID().toString();
				if(removeDash)key=StringUtils.remove(key, '-');
				field.set(o, key);
			}
		}
		public PreparedStatement doPrepareStatement(OperateTarget conn, String sql,String dbName) throws SQLException {
			return conn.prepareStatement(sql);
		}
		public int executeUpdate(Statement st, String sql) throws SQLException {
			return st.executeUpdate(sql);
		}

		public void callAfter(List<? extends IQueryableEntity> data) throws SQLException {
		}
	}
	
	/**
	 * 用于在插入时返回Oracle Rowid的回调
	 * @author jiyi
	 *
	 */
	static class OracleRowidKeyCallback implements AutoIncreatmentCallBack {
		private Statement st;
		private AutoIncreatmentCallBack parent;
		public OracleRowidKeyCallback(AutoIncreatmentCallBack parent){
			this.parent=parent;
		}
		public void callAfter(List<? extends IQueryableEntity> data) throws SQLException {
			if(data.isEmpty())return;
			Assert.isTrue(data.size()==1,"The function do not support batch insert mode.");
			ResultSet rs=st.getGeneratedKeys();
			if(rs==null)
				throw new SQLException("getGeneratedKeys() returns null from the " + st+".");
			try{
				IQueryableEntity entity=data.get(0);
				Assert.isTrue(rs.next(),"The JDBC Driver may not support you operation.");
				entity.bindRowid(rs.getString(1));
			}finally{
				rs.close();
			}
			if(parent!=null)parent.callAfter(data);
		}
		public PreparedStatement doPrepareStatement(OperateTarget conn, String sql,String dbName) throws SQLException {
			PreparedStatement pst=conn.prepareStatement(sql, 1);
			this.st=pst;
			return pst;
		}
		public int executeUpdate(Statement st, String sql) throws SQLException {
			this.st=st;
			return st.executeUpdate(sql,1);
		}
		public void callBefore(List<? extends IQueryableEntity> data, Session dbName) throws SQLException {
			if(parent!=null)parent.callBefore(data, dbName);
		}
	}

	/**
	 * 对批量或单个对象，赋予从JDBC得到的数据库所生成的主键值(需要驱动支持)
	 * 
	 * @author Administrator
	 */
	static class JdbcAutoGeneratedKeyCallback implements AutoIncreatmentCallBack {
		private Property fieldName;
		private boolean isGuessMode;
		private String[] columnName;
		private Statement st;
		
		/**
		 * 
		 * @param fieldName 必须是Long型号的Property，如果不是请先在外部包装
		 * @param guessMode
		 * @param columnName
		 */
		public JdbcAutoGeneratedKeyCallback(Property fieldName,boolean guessMode,String columnName) {
			this.fieldName = fieldName;
			this.isGuessMode=guessMode;
			this.columnName=new String[]{columnName};
		}

		public void callAfter(List<? extends IQueryableEntity> data) throws SQLException {
			if(data.size()==0)return;
			ResultSet rs = st.getGeneratedKeys();
			if(rs==null)
				throw new SQLException("getGeneratedKeys() returns null from the " + st+".");
			try{
				if(isGuessMode){
					Assert.isTrue(rs.next(),"The JDBC Driver may not support getGeneratedKeys() operation.");
					long max=rs.getLong(1);
					for (int i=data.size()-1;i>=0;i--) {
						IQueryableEntity o=data.get(i); 
						fieldName.set(o, max--);
					}	
				}else{
					for (IQueryableEntity o : data) {
						if (rs.next()) {
							fieldName.set(o, rs.getLong(1));
						} else {
							throw new SQLException("The count of generated key from statement is not match to required.");	
						}
					}	
					if (rs.next()) {
						throw new SQLException("The count of generated key from statement is greater than the size.");
					}	
				}				
			}finally{
				rs.close();
			}
		}
		public PreparedStatement doPrepareStatement(OperateTarget conn, String sql,String dbName) throws SQLException {
			PreparedStatement pst=conn.prepareStatement(sql,columnName);
			this.st=pst;
			return pst;
		}
		public int executeUpdate(Statement st, String sql) throws SQLException {
			this.st=st;
			return st.executeUpdate(sql,columnName);
		}
		public void callBefore(List<? extends IQueryableEntity> data, Session dbName) throws SQLException {
		}
	}
	
	
}
