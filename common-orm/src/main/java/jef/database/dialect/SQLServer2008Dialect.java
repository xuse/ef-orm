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
import jef.database.meta.object.Column;
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
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String constraintName)
			throws SQLException {

		StringBuilder sb = new StringBuilder(); // PG系统约束信息查询
		sb.append("    SELECT tc.*, kcu.column_name, rc.match_option,  rc.update_rule, rc.delete_rule, ");
		sb.append("           ccu.table_name AS ref_table, ccu.column_name AS ref_column ");
		sb.append("      FROM information_schema.table_constraints tc");
		sb.append(" LEFT JOIN information_schema.key_column_usage kcu");
		sb.append("        ON tc.constraint_catalog = kcu.constraint_catalog");
		sb.append("       AND tc.constraint_schema = kcu.constraint_schema");
		sb.append("       AND tc.constraint_name = kcu.constraint_name");
		sb.append(" LEFT JOIN information_schema.referential_constraints rc");
		sb.append("        ON tc.constraint_catalog = rc.constraint_catalog");
		sb.append("       AND tc.constraint_schema = rc.constraint_schema");
		sb.append("       AND tc.constraint_name = rc.constraint_name");
		sb.append(" LEFT JOIN information_schema.constraint_column_usage ccu");
		sb.append("        ON rc.unique_constraint_catalog = ccu.constraint_catalog");
		sb.append("       AND rc.unique_constraint_schema = ccu.constraint_schema");
		sb.append("       AND rc.unique_constraint_name = ccu.constraint_name ");
		sb.append("     WHERE tc.constraint_schema like ? and tc.constraint_name like ?");
		sb.append("  ORDER BY tc.constraint_catalog, tc.constraint_schema, tc.constraint_name");

		schema = StringUtils.isBlank(schema) ? "%" : schema.toLowerCase();
		constraintName = StringUtils.isBlank(constraintName) ? "%" : constraintName.toLowerCase();
		List<Constraint> constraints = conn.selectBySql(sb.toString(), new AbstractResultSetTransformer<List<Constraint>>(){

			@Override
			public List<Constraint> transformer(IResultSet rs) throws SQLException {
				
				List<Constraint> constraints = new ArrayList<Constraint>();
				List<Column> columns = new ArrayList<Column>();
				List<Column> refColumns = new ArrayList<Column>();
				Constraint preCon = new Constraint(); // 上一条记录
				
				while(rs.next()){
					
					if(constraints.size() > 0){
						preCon = constraints.get(constraints.size() - 1);
					}
					
					boolean isSameConstraint = rs.getString("constraint_catalog").equals(preCon.getCatalog())
							&& rs.getString("constraint_schema").equals(preCon.getSchema())
							&& rs.getString("constraint_name").equals(preCon.getName());

					if(!isSameConstraint){

						columns = new ArrayList<Column>();
						refColumns = new ArrayList<Column>();

						Constraint c = new Constraint();
						c.setCatalog(rs.getString("constraint_catalog"));
						c.setSchema(rs.getString("constraint_schema"));
						c.setName(rs.getString("constraint_name"));
						c.setType(ConstraintType.parseFullName(rs.getString("constraint_type")));
						c.setDeferrable("YES".equals(rs.getString("is_deferrable")));
						c.setInitiallyDeferrable("YES".equals(rs.getString("initially_deferred")));
						c.setTableCatalog(rs.getString("table_catalog"));
						c.setTableSchema(rs.getString("table_schema"));
						c.setTableName(rs.getString("table_name"));
						c.setMatchType(ForeignKeyMatchType.parseName(rs.getString("match_option")));
						c.setRefTableName(rs.getString("ref_table"));
						c.setUpdateRule(ForeignKeyAction.parseName(rs.getString("update_rule")));
						c.setDeleteRule(ForeignKeyAction.parseName(rs.getString("delete_rule")));
						c.setEnabled(true); // 默认启用
						c.setColumns(columns);
						c.setRefColumns(refColumns);
						constraints.add(c);
					}
					
					// 有指定列的约束则添加到列表
					if(StringUtils.isNotBlank(rs.getString("column_name"))){
						Column column = new Column();
						column.setColumnName(rs.getString("column_name"));
						columns.add(column);
					}
					
					// 是外键约束则添加到参照列表
					if(StringUtils.isNotBlank(rs.getString("ref_column"))){
						Column column = new Column();
						column.setColumnName(rs.getString("ref_column"));
						refColumns.add(column);
					}
				}
				
				return constraints;
			}
		}, Arrays.asList(schema, constraintName));
		
		return constraints;
	}

}
