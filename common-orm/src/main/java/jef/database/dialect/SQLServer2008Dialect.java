package jef.database.dialect;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.querydsl.sql.SQLServer2008Templates;
import com.querydsl.sql.SQLTemplates;

import jef.database.DbMetaData;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.ConstraintType;
import jef.database.meta.object.ForeignKeyAction;
import jef.database.meta.object.ForeignKeyMatchType;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.tools.StringUtils;

/**
 * SQL Server 2008
 * 
 * 2008特性 1、支持稀疏列 2、支持压缩表和压缩索引 3、新排序规则（collations） 4、分区切换 5、宽表、带有列族的表
 * 
 * 6、支持Date time datetime以及时区数据类型? 7支持 WITH ROLLUP, WITH CUBE, and ALL syntax is
 * deprecated. For more information, see Using GROUP BY with ROLLUP, CUBE, and
 * GROUPING SETS. 8 支持Merge语句 9 一个insert语句可以插入多行记录
 * 
 * 
 * @author jiyi
 * 
 */
public class SQLServer2008Dialect extends SQLServer2005Dialect {

	public SQLServer2008Dialect() {
		super();
		typeNames.put(Types.DATE, "date", 0);
		typeNames.put(Types.TIME, "time", 0);
		typeNames.put(Types.TIMESTAMP, "datetime2", 0);
	}
	
	 //to be override
    protected SQLTemplates generateQueryDslTemplates() {
        return new SQLServer2008Templates();
    }

	@Override
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String tablename, String constraintName)
			throws SQLException {

		// 系统约束信息查询
		String sql =  "    SELECT tc.*, ccu.column_name, rc.match_option,  rc.update_rule, rc.delete_rule, "
					+"           rccu.table_schema as ref_table_schema, rccu.table_name AS ref_table_name, "
					+"           rccu.column_name AS ref_column_name, cc.check_clause "
					+"      FROM information_schema.table_constraints tc"
					+" LEFT JOIN information_schema.check_constraints cc"
	                +"        ON cc.constraint_catalog = tc.constraint_catalog"
					+"	     AND cc.constraint_schema = tc.constraint_schema"
					+"	     AND cc.constraint_name = tc.constraint_name"
					+" LEFT JOIN information_schema.constraint_column_usage ccu"
					+"        ON tc.constraint_catalog = ccu.constraint_catalog"
	                +"       AND tc.constraint_schema = ccu.constraint_schema"
	                +"       AND tc.constraint_name = ccu.constraint_name "
					+" LEFT JOIN information_schema.key_column_usage kcu"
					+"        ON ccu.constraint_catalog = kcu.constraint_catalog"
					+"       AND ccu.constraint_schema = kcu.constraint_schema"
					+"       AND ccu.constraint_name = kcu.constraint_name"
					+"       AND ccu.column_name = kcu.column_name"
					+" LEFT JOIN information_schema.referential_constraints rc"
					+"        ON tc.constraint_catalog = rc.constraint_catalog"
					+"       AND tc.constraint_schema = rc.constraint_schema"
					+"       AND tc.constraint_name = rc.constraint_name"
					+" LEFT JOIN information_schema.constraint_column_usage rccu"
					+"        ON rc.unique_constraint_catalog = rccu.constraint_catalog"
					+"       AND rc.unique_constraint_schema = rccu.constraint_schema"
					+"       AND rc.unique_constraint_name = rccu.constraint_name "
					+"     WHERE tc.constraint_schema like ? and tc.table_name like ? and tc.constraint_name like ?"
					+"  ORDER BY tc.constraint_catalog, tc.constraint_schema, tc.constraint_name, kcu.ordinal_position";

		schema = StringUtils.isBlank(schema) ? "%" : schema.toLowerCase();
		tablename = StringUtils.isBlank(tablename) ? "%" : tablename.toLowerCase();
		constraintName = StringUtils.isBlank(constraintName) ? "%" : constraintName.toLowerCase();
		List<Constraint> constraints = conn.selectBySql(sql, new AbstractResultSetTransformer<List<Constraint>>(){

			@Override
			public List<Constraint> transformer(IResultSet rs) throws SQLException {
				
				List<Constraint> constraints = new ArrayList<Constraint>();
				List<String> columns = new ArrayList<String>();
				List<String> refColumns = new ArrayList<String>();
				Constraint preCon = new Constraint(); // 上一条记录
				
				while(rs.next()){
					
					if(constraints.size() > 0){
						preCon = constraints.get(constraints.size() - 1);
					}
					
					boolean isSameConstraint = rs.getString("constraint_catalog").equals(preCon.getCatalog())
							&& rs.getString("constraint_schema").equals(preCon.getSchema())
							&& rs.getString("constraint_name").equals(preCon.getName());

					if(!isSameConstraint){

						columns = new ArrayList<String>();
						refColumns = new ArrayList<String>();

						Constraint c = new Constraint();
						c.setCatalog(rs.getString("constraint_catalog"));
						c.setSchema(rs.getString("constraint_schema"));
						c.setName(rs.getString("constraint_name"));
						c.setType(ConstraintType.parseFullName(rs.getString("constraint_type")));
						c.setDeferrable("YES".equals(rs.getString("is_deferrable")));
						c.setInitiallyDeferred("YES".equals(rs.getString("initially_deferred")));
						c.setTableCatalog(rs.getString("table_catalog"));
						c.setTableSchema(rs.getString("table_schema"));
						c.setTableName(rs.getString("table_name"));
						c.setMatchType(ForeignKeyMatchType.parseName(rs.getString("match_option")));
						c.setRefTableSchema(rs.getString("ref_table_schema"));
						c.setRefTableName(rs.getString("ref_table_name"));
						c.setUpdateRule(ForeignKeyAction.parseName(rs.getString("update_rule")));
						c.setDeleteRule(ForeignKeyAction.parseName(rs.getString("delete_rule")));
						c.setCheckClause(rs.getString("check_clause"));
						c.setEnabled(true); // 默认启用
						c.setColumns(columns);
						c.setRefColumns(refColumns);
						constraints.add(c);
					}
					
					// 有指定列的约束则添加到列表
					if(StringUtils.isNotBlank(rs.getString("column_name"))){
						columns.add(rs.getString("column_name"));
					}
					
					// 是外键约束则添加到参照列表
					if(StringUtils.isNotBlank(rs.getString("ref_column_name"))){
						refColumns.add(rs.getString("ref_column_name"));
					}
				}
				
				return constraints;
			}
		}, Arrays.asList(schema, tablename, constraintName));
		
		return constraints;
	}

}
