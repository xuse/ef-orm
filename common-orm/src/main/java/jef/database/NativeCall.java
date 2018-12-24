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

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.Session.PopulateStrategy;
import jef.database.innerpool.WrapableConnection;
import jef.database.jdbc.result.ResultSetImpl;
import jef.database.query.OutParam;
import jef.database.wrapper.populator.Transformer;

/**
 * 存储过程调用对象
 * 
 * @author Jiyi
 * 
 */
public class NativeCall {
	private static final int ORACLE_CURSOR = -10;

	/**
	 * 数据库
	 */
	private WrapableConnection db;
	/**
	 * 结果拼装策略
	 */
	private PopulateStrategy[] strategies = { PopulateStrategy.PLAIN_MODE };
	/**
	 * 存储过程名称
	 */
	private Session parent;
	private String name;
	private CallableStatement st;
	private String sql;
	private Type[] params;
	private Object[] outParamCache;
	private boolean anonymous;

	/**
	 * 是否为匿名存储过程
	 * 
	 * @return true if the procedure is anonymous.
	 */
	public boolean isAnonymousProcedure() {
		return anonymous;
	}

	/*
	 * 构造
	 */
	NativeCall(OperateTarget db, String name, Type[] params, boolean anonymous){
		this.parent=db.getSession();
		this.db = db.get();
		
		this.name = name;
		this.params = params;
		this.anonymous = anonymous;
		if (anonymous) {
			sql = name;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("{call ").append(name).append("(");
			if (params.length > 0) {
				sb.append('?');
				for (int i = 1; i < params.length; i++) {
					sb.append(",?");
				}
			}
			sb.append(")}");
			sql = sb.toString();
		}
		try{
			init();
		}catch(SQLException e){
			throw new PersistenceException(e);
		}
	}

	private void init() throws SQLException {
		st = db.prepareCall(sql);
		// 定义出参
		for (int i = 0; i < params.length; i++) {
			if (params[i] instanceof OutParam) {
				OutParam OutParam = (OutParam) params[i];
				if (OutParam.isList()) {// 游标
					st.registerOutParameter(i + 1, ORACLE_CURSOR);
				} else {// 常量
					st.registerOutParameter(i + 1, getOracleTypeByJava(OutParam.getType()));
				}
			}
		}
	}

	// 根据入参的java类返回出参的定义类型
	private int getOracleTypeByJava(Class<?> type) {
		if (Number.class.isAssignableFrom(type)) {
			return Types.DECIMAL;
		} else if (CharSequence.class.isAssignableFrom(type)) {
			return Types.VARCHAR;
		} else if (type == java.sql.Time.class) {
			return Types.TIME;
		} else if (type == java.sql.Timestamp.class || type == java.util.Date.class) {
			return Types.TIMESTAMP;
		} else if (type == java.sql.Date.class) {
			return Types.DATE;
		} else if (type == java.sql.Blob.class) {
			return Types.BLOB;
		} else if (type == java.sql.Clob.class) {
			return Types.CLOB;
		} else if (type == Boolean.class) {
			return Types.BOOLEAN;
		} else {
			return Types.VARCHAR;
		}
	}

	/**
	 * 判断对象是否已经关闭
	 * @return true if closed
	 */
	public boolean isClosed(){
		return st==null;
	}
	
	private void assertOpen(boolean reopen) {
		if (st == null) {
			if(reopen){
				try{
					init();
				}catch(SQLException e){
					throw new PersistenceException(e);
				}
			}else{
				throw new RuntimeException("The call is closed!");
			}
		}
	}

	/**
	 * 获得调用的存储过程名称，如果是匿名块直接返回SQL语句
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * 执行
	 * @param 存储过程调用参数。此处不指定也可。
	 * @return <code>true</code> if the first result is a <code>ResultSet</code>
	 *         object; <code>false</code> if the first result is an update count
	 *         or there is no result
	 */
	public boolean execute(Object... objs) {
		assertOpen(true);
		for (int i = 0; i < objs.length; i++) {
			Object value = objs[i];
			innerSet(i + 1, value);
		}
		if(ORMConfig.getInstance().isDebugMode()){
			LogUtil.show("Exccuting:"+sql+Arrays.toString(objs));
		}
		this.outParamCache=null;
		try {
			boolean result = st.execute();
			cacheOutParams(true);// If there is no cursor open, automatically close this call.
			return result;
		} catch (SQLException e) {
			close();
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}
	/**
	 * 获得目前设置的结果转换策略
	 * @return
	 */
	public PopulateStrategy[] getStrategies() {
		return strategies;
	}

	/**
	 * 设置结果拼装策略，可以同时使用多个选项策略
	 * 
	 * @param strategies
	 *            拼装策略
	 *            
	 * @see jef.database.Session.PopulateStrategy
	 */
	public void setStrategies(PopulateStrategy... strategies) {
		this.strategies = strategies;
	}

	/**
	 * 关闭结果集(游标等)
	 */
	public void close() {
		if (st != null) {
			try {
				st.close();
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
			db.close();
			st = null;
		}
		parent=null;
	}

	/*
	 * 检查并转换参数类型
	 */
	private Object checkAndConvert(Class<?> clz, Object value) {
		return value;
	}

	private Object checkAndAdjustOut(OutParam outType, Object value) {
		if (outType.isList()) {
			Class<?> clz = outType.getType();
			ResultSet rs = (ResultSet) value;
			try {
				return parent.populateResultSet(new ResultSetImpl(rs, db.getProfile()), null, new Transformer(clz, strategies));
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
			} finally {
				try {
					rs.close();
				} catch (Exception e) {
					LogUtil.exception(e);
				}
			}
		} else {//FIXME 非列表的场合没有返回值啊
			// Class clz=outType.getType();
			return value;
		}
	}

	private void innerSet(int i, Object obj) {
		Type t = params[i - 1];
		if (t instanceof Class) {
			try {
				Object value = checkAndConvert((Class<?>) t, obj);
				st.setObject(i, value);
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
			}
		} else {
			throw new RuntimeException("The param of index " + i + " is not a input param!");
		}
	}

//	/**
//	 * 传入第n个参数，n从1开始
//	 * 
//	 * @param index 序号，从1开始
//	 * @param obj  参数值
//	 */
//	public void setParameter(int i, Object obj) {
//		assertOpen(false);
//		if (params.length < i) {
//			throw new RuntimeException("the index " + i + " is exceed the max param number!");
//		}
//		innerSet(i, obj);
//	}
	
	/**
	 * 设置多个参数，第一个参数对应1，第二个对应2,以此类推
	 * 
	 * @param objs
	 */
	public void setParameters(Object... objs) {
		assertOpen(false);
		for (int i = 0; i < objs.length; i++) {
			Object value = objs[i];
			innerSet(i + 1, value);
		}
	}

	/**
	 * 得到出参，并且转换为指定类型的List，该类型必须和NativeCall对象构造时的类型一致
	 * 
	 * @param i   参数序号，从1开始
	 * @param clz 需要转换的类型
	 * @return  存储过程传出参数（列表）
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getOutParameterAsList(int i, Class<T> clz) {
		Object obj = getOutParameter(i);
		if (obj instanceof List) {
			return (List<T>) obj;
		} else {
			throw new ClassCastException("The output parameter is not a list type!");
		}
	}

	/**
	 * 得到出参，并且转换为指定类型
	 * 
	 * @param i   参数序号，从1开始
	 * @param clz  需要转换的类型，该类型必须和NativeCall对象构造时的类型一致
	 * @return 存储过程传出参数
	 */
	@SuppressWarnings("unchecked")
	public <T> T getOutParameterAs(int i, Class<T> clz) {
		Object obj = getOutParameter(i);
		return (T) obj;
	}

	/**
	 * 获取存储过程的传出参数，
	 * 
	 * @param i
	 *            参数序号，从1开始
	 * @return
	 * 	存储过程的传出参数，参数类型在构造时确定。
	 */
	public Object getOutParameter(int i) {
		if (params.length < i) {
			throw new RuntimeException("the index " + i + " is exceed the max param number!");
		}
		Type t = params[i - 1];
		if (t instanceof OutParam) {
			if (outParamCache != null) {
				return checkAndAdjustOut((OutParam) t, outParamCache[i - 1]);
			}
			assertOpen(false);
			try {
				Object value = st.getObject(i);
				return checkAndAdjustOut((OutParam) t, value);
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
			}
		} else {
			throw new RuntimeException("The param of index " + i + " is not a output param!");
		}
	}

	private void cacheOutParams(boolean close) throws SQLException {
		if (params.length > 0) {
			Object[] cache = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				Type t = params[i];
				if (t instanceof OutParam) {
					if (((OutParam) t).isList()) {
						outParamCache = null;
						return;
					} else {
						cache[i] = st.getObject(i + 1);
					}
				}
			}
			this.outParamCache = cache;
		}
		if(close)
			close();

	}
}
