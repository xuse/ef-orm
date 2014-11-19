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
package jef.database.jdbc.result;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.populator.ColumnDescription;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

public class ResultSets {
	private ResultSets() {
	}

	/**
	 * 显示sql的resultset
	 * 
	 * @param rs 结果集
	 * @param limit 限制
	 */
	public static void showResult(ResultSet rs, int limit, DatabaseDialect profile) {
		if(rs==null){
			return;
		}
		showResult(rs, limit, true, profile);
	}

	/**
	 * 以文本显示SQL结果
	 * @param rs 结果集
	 * @param limit　限制
	 * @param closeIt 是否关闭
	 * @param profile 数据库方言
	 */
	public static void showResult(ResultSet rs, int limit, boolean closeIt, DatabaseDialect profile) {
		ResultSetImpl wrapper = new ResultSetImpl(rs, profile);
		String msg = wrapper.getColumns().toString();
		LogUtil.show(msg);
		if (JefConfiguration.getBoolean(JefConfiguration.Item.CONSOLE_SHOW_COLUMN_TYPE, false)) {
			msg = StringUtils.join(wrapper.getColumns(), ",");
			LogUtil.show(msg);
		}
		Writer w = null;
		int shown = 0;
		try {
			w = new OutputStreamWriter(System.out, "UTF-8");
			shown = wrapper.write(w, limit);
		} catch (UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		} finally {
			try {
				w.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}// 这里不能关
		}

		if (LogUtil.otherStream != null) {
			for (Writer out : LogUtil.otherStream) {
				wrapper.reset();
				wrapper.write(out, limit);
			}
		}
		int count = wrapper.getTotal();
		if (count > 0) {
			msg = count + " rows selected." + ((count > shown) ? "(Show top " + shown + " rows only.)" : "");
			LogUtil.show(msg);
		} else {
			msg = "No record found.";
			LogUtil.show(msg);
		}
		if (closeIt) {
			try {
				rs.close();
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
		}
	}
	
	public static List<Map<String,Object>> toMap(ResultSet rs) throws SQLException{
		List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
		ResultSetMetaData meta=rs.getMetaData();
		int cols=meta.getColumnCount();
		while(rs.next()){
			Map<String,Object> map=new HashMap<String,Object>();
			for(int i=1;i<=cols;i++){
				map.put(meta.getColumnName(i), rs.getObject(i));
			}
			result.add(map);
		}
		DbUtils.close(rs);
		return result;
	}
	
	public static List<Object> toObject(ResultSet rs,int column) throws SQLException{
		List<Object> result=new ArrayList<Object>();
		ResultSetMetaData meta=rs.getMetaData();
		result.add("("+meta.getColumnName(column)+")");
		while(rs.next()){
			result.add(rs.getObject(column));
		}
		DbUtils.close(rs);
		return result;
	}

	public static List<Object> toObjectList(ResultSet wrapper, int column, int maxReturn) throws SQLException {
		if(maxReturn==0)maxReturn=Integer.MAX_VALUE;
		int count = 0;
		List<Object> data = new ArrayList<Object>();
		while (wrapper.next() && count < maxReturn) {
			data.add(wrapper.getObject(column));
			count++;
		}
		return data;
	}

	/**
	 * 获取指定列的String数据，整体返回一个List<String>
	 * @param rs
	 * @param column
	 * @param maxReturn
	 * @param profile
	 * @return List<String> 返回类型
	 * @throws SQLException
	 */
	public static List<String> toStringList(ResultSet rs, String column, int maxReturn, DatabaseDialect profile) throws SQLException {
		ResultSetImpl wrapper = new ResultSetImpl(rs, profile);
		ColumnDescription c = wrapper.getColumns().getByUpperName(column);
		if (c == null) {
			throw new SQLException("The column does not exist in the resultset: " + column);
		}
		int count = 0;
		List<String> data = new ArrayList<String>();
		while (rs.next() && count <= maxReturn) {
			data.add(rs.getString(c.getN()));
			count++;
		}
		return data;
	}
}
