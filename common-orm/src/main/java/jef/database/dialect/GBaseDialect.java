package jef.database.dialect;

import java.util.Arrays;

import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.dialect.statement.LimitHandler;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.support.RDBMS;
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
	}
	private final LimitHandler limit=new LimitOffsetLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}
}
