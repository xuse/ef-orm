package jef.database.query;

import java.util.ArrayList;
import java.util.List;

import jef.database.DbUtils;
import jef.database.QueryAlias;
import jef.database.SqlProcessor;
import jef.database.annotation.JoinType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.meta.JoinKey;
import jef.database.meta.JoinPath;
import jef.database.meta.MetaHolder;
import jef.tools.StringUtils;

/**
 * 一个Join的补充信息，描述这个Join实例与其左侧的各个表之前的关系
 * @author Administrator
 *
 */
public class JoinCondition {
	private Query<?> left;
	private JoinPath value;
	private JoinType type;
	
	public JoinCondition(Query<?> query,JoinPath joinPaths){
		this.left=query;
		this.value=joinPaths;
		type=joinPaths.getType();
	}

	public JoinType getType() {
		return type;
	}

	public void setType(JoinType type) {
		if(type!=null){
			this.type = type;
		}
	}

	public void toOnExpression(StringBuilder sb, SqlContext sqlContext,QueryAlias right,SqlProcessor processor,DatabaseDialect profile) {

		ITableMetadata meta=MetaHolder.getMeta(right.getQuery().getInstance());
		sqlContext=sqlContext.getContextOf(right.getQuery()); 
		String leftAlias=sqlContext.getAliasOf(left);//判断是左边的那张表和右边连接？//FIXME 如果左边多张表的字段参与Join条件呢
		
		List<String> keys = new ArrayList<String>();
		for(JoinKey key: value.getJoinKeys()){
			String la=leftAlias;
			String ra=sqlContext.getCurrentAlias();
			if(key.getLeftLock()!=null){
				la=sqlContext.getAliasOf(key.getLeftLock());	
			}
			if(key.getRightLock()!=null){
				ra=sqlContext.getAliasOf(key.getRightLock());
			}
			String left=DbUtils.toColumnName(key.getLeft(),profile, la);
			String rightStr=DbUtils.toColumnName(key.getRightAsField(),profile,ra);
			keys.add(StringUtils.concat(left,"=",rightStr));
		}
		for(JoinKey key: value.getJoinExpression()){
			if(key.getField()==null){
				if(key.getValue() instanceof JpqlExpression){
					JpqlExpression jpql=(JpqlExpression) key.getValue();
					String code=jpql.toSqlAndBindAttribs(sqlContext,profile);
					keys.add(code);
				}
			}else{
				keys.add(key.toSqlClause(meta, sqlContext, processor, left==null?null:left.getInstance(),profile));	
			}
		}
		sb.append(StringUtils.join(keys, " and "));
	}
}
