package jef.database.dialect;

import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.DbUtils;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.support.RDBMS;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;

/**
 * GBase的dialect
 * <p>
 * TODO Gbase方言待测试
 * <ul>
 * <li>1) 数据插入后自增列的值获取；</li>
 * <li>2) 批量删除时，异常：can't lock file</li>
 * <li>3) 批量更新时，异常：refreshRow() called on row that has been deleted or had primary key changed.</li>
 * </ul>
 * </p> 
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author luolp@asiainfo-linkage.com
 * @Date 2013-1-28
 */
public class GBaseDialect extends AbstractDialect {

	protected static final String DRIVER_CLASS = "com.gbase.jdbc.Driver";
	protected static final int DEFAULT_PORT = 5258;

	protected static final String GBASE_PAGE = " limit %next% offset %start%";

	public GBaseDialect() {
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(
				Feature.AUTOINCREMENT_MUSTBE_PK
				));
		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "ALTER");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL,"select 1");
		setProperty(DbProperty.INDEX_USING_HASH," USING HASH");
		
	}

	public RDBMS getName() {
		return RDBMS.gbase;
	}

	public String getDriverClass(String url) {
		return DRIVER_CLASS;
	}
	@Override
	public String getCatlog(String schema) {
		return schema;
	}

//	@Override
//	public void processAutoIncrement(List<String> cStr, List<String> vStr, InsertSqlResult result,
//			String columnName, Field field, TableMetadata meta, AbstractDbClient parent)
//			throws SQLException {
//		cStr.add(columnName);
//		vStr.add("DEFAULT");
//	}

	@Override
	protected String getComment(ColumnType.AutoIncrement column,boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("INT AUTO_INCREMENT ");
		if(flag){
			if (!column.nullable) {
				sb.append(" NOT NULL");
			}	
		}
		return sb.toString();
	}

	public String toPageSQL(String sql, IntRange range) {
		boolean isUnion=false;
		try {
			Select select=DbUtils.parseNativeSelect(sql);
			if(select.getSelectBody() instanceof Union){
				isUnion=true;
			}
			select.getSelectBody();
		} catch (ParseException e) {
			LogUtil.exception("SqlParse Error:",e);
		}
		
		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(GBASE_PAGE,
				new String[] { "%start%", "%next%" }, new String[] { start, next });
		return isUnion ?
				StringUtils.concat("select * from (", sql, ") tb", limit) : sql.concat(limit);
	}
	
	public String toPageSQL(String sql, IntRange range,boolean isUnion) {
		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(GBASE_PAGE,
				new String[] { "%start%", "%next%" }, new String[] { start, next });
		return isUnion ?
				StringUtils.concat("select * from (", sql, ") tb", limit) : sql.concat(limit);
	}


	public String getFunction(DbFunction func, Object... params) {
		if(func instanceof jef.database.query.Func){
			switch ((jef.database.query.Func)func) {
			case current_date:
				return "CURRENT_DATE";
			case current_time:
				return "CURRENT_TIME";
			case current_timestamp:
				return "NOW()";
			default:
				throw new IllegalArgumentException("Unknown database function " + func.name());
			}	
		}else{
			throw new IllegalArgumentException("Unknown database function " + func.name());
		}
	}

	public void parseDbInfo(ConnectInfo connectInfo) {
		// TODO Auto-generated method stub
		
	}
}
