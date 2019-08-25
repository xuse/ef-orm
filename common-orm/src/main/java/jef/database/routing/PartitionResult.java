package jef.database.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.tools.StringUtils;

/**
 * 描述分表计算后的结果
 * 每个结果包含两部分数据:<br> 
 * <li>database</li>数据库的逻辑名称，当分布式存储时使用
 * <li>tables</li>是一个List<String>多张表的名称
 * 
 * @author Administrator
 *
 */
public class PartitionResult {
	private String database;
	private List<String> tables;
	
	/**
	 * 空构造
	 */
	public PartitionResult(){
	}
	
	/**
	 * 传入一个或多个表名，构建
	 * @param tables
	 */
	public PartitionResult(String... tables) {
		this.tables=Arrays.asList(tables);
	}

	/**
	 * 得到数据库名。（如果返回null表示当前数据库）
	 * @return
	 */
	public String getDatabase() {
		return database;
	}
	
	/**
	 * 得到多个数据库表名称，如果带schema则表示 schema.tablename
	 */
	@SuppressWarnings("unchecked")
	public List<String> getTables() {
		if(tables==null)return Collections.EMPTY_LIST;
		return tables;
	}
	
	/**
	 * 得到多个数据库表名称，如果带schema则表示 schema.tablename
	 * @param profile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getTablesEscaped(DatabaseDialect profile) {
		if(tables==null)return Collections.EMPTY_LIST;
		String raw=tables.get(0);
		//FIXME if there is schema in table name, how...
		String name=DbUtils.escapeColumn(profile, raw);
		
		if(name==raw){
			return tables; 
		}else{
			List<String> r=new ArrayList<String>(tables);
			r.set(0, name);
			return r;
		}
	}
	
	
	
	public void setTables(List<String> tables) {
		this.tables = tables;
	}
	
	public PartitionResult setDatabase(String database) {
		this.database =StringUtils.trimToNull(database);
		return this;
	}

	@Override
	public String toString() {
		return database==null?StringUtils.join(tables,","):database+":"+StringUtils.join(tables,",");
	}

	/**
	 * 如果确认表只有一张，得到表名。
	 * @return 表名。抛出异常IllegalArgumentException如果表不止一张。
	 */
	public String getAsOneTable() {
		int n=tables.size();
		if(n>1){
			throw new NonUniqueResultException("There's " + n+" tables!");
		}else if(n==0){
			throw new NoResultException("There's no table!");
		}
		return tables.get(0);
	}
	
	/**
	 * 获得表名。Escaped
	 * @param profile
	 * @return
	 */
	public String getAsOneTableEscaped(DatabaseDialect profile) {
		int n=tables.size();
		if(n>1){
			throw new NonUniqueResultException("There's " + n+" tables!");
		}else if(n==0){
			throw new NoResultException("There's no table!");
		}
		return DbUtils.escapeColumn(profile, tables.get(0));
	}

	/**
	 * 获得表的数量
	 * @return
	 */
	public int tableSize() {
		return tables.size();
	}
}
