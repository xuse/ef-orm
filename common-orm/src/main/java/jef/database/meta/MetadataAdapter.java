package jef.database.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import jef.common.wrapper.IntRange;
import jef.database.BindVariableDescription;
import jef.database.DbCfg;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.ORMConfig;
import jef.database.annotation.BindDataSource;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKey;
import jef.database.cache.KeyDimension;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;
import jef.database.query.DbTable;
import jef.database.query.JpqlExpression;
import jef.database.query.Query;
import jef.database.routing.sql.SqlAnalyzer;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.OrderClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.SelectPart;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

/**
 * 抽象类用于简化Tablemeta的实现
 * 
 * @author jiyi
 * 
 */
public abstract class MetadataAdapter implements ITableMetadata {
	protected String schema;
	protected String tableName;
	private String bindDsName;
	protected List<MappingType<?>> metaFields;
	final List<jef.database.annotation.Index> indexMap = new ArrayList<jef.database.annotation.Index>(5);//记录对应表的所有索引，当建表时使用可自动创建索引

	protected abstract Collection<MappingType<?>> getColumnSchema();


	protected void initByAnno(Class<?> thisType,javax.persistence.Table table,BindDataSource bindDs) {
		// schema初始化
		if (table != null) {
			if (table.schema().length() > 0) {
				schema = MetaHolder.getMappingSchema(table.schema());// 重定向
			}
			if (table.name().length() > 0) {
				tableName = table.name();
			}
		}
		if (tableName == null) {
			// 表名未指定，缺省生成
			boolean needTranslate = JefConfiguration.getBoolean(DbCfg.TABLE_NAME_TRANSLATE, false);
			if (needTranslate) {
				tableName = DbUtils.upperToUnderline(thisType.getSimpleName());
			} else {
				tableName = thisType.getSimpleName();
			}
		}
		if (bindDs != null) {
			this.bindDsName = MetaHolder.getMappingSite(StringUtils.trimToNull(bindDs.value()));
		}
	}
	
	public String getBindDsName() {
		return bindDsName;
	}

	public void setBindDsName(String bindDsName) {
		this.bindDsName = MetaHolder.getMappingSite(bindDsName);
		this.bindProfile=null;
	}
	
	public List<MappingType<?>> getMetaFields() {
		if (metaFields == null) {
			Collection<MappingType<?>> map = this.getColumnSchema();
			MappingType<?>[] fields = map.toArray(new MappingType[map.size()]);
			Arrays.sort(fields, new Comparator<MappingType<?>>() {
				public int compare(MappingType<?> field1, MappingType<?> field2) {
					Class<? extends ColumnType> type1 = field1.get().getClass();
					Class<? extends ColumnType> type2 = field2.get().getClass();
					Boolean b1 = (type1 == ColumnType.Blob.class || type1 == ColumnType.Clob.class);
					Boolean b2 = (type2 == ColumnType.Blob.class || type2 == ColumnType.Clob.class);
					return b1.compareTo(b2);
				}
			});
			metaFields = Arrays.asList(fields);
		}
		return metaFields;
	}

	public String getSchema() {
		return schema;
	}

	/**
	 * 返回表名
	 * 
	 * @param withSchema
	 *            true要求带schema
	 * @return
	 */
	public String getTableName(boolean withSchema) {
		if (withSchema && schema != null)
			return new StringBuilder(schema.length() + tableName.length() + 1).append(schema).append('.').append(tableName).toString();
		return tableName;
	}

	public String getColumnName(Field fld, String alias, DatabaseDialect profile) {
		if (alias != null) {
			if (fld instanceof JpqlExpression) {
				throw new UnsupportedOperationException();
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(alias).append('.').append(getColumnName(fld, profile, true));
				return sb.toString();
			}
		} else {
			return getColumnName(fld, profile, true);
		}
	}

	public KeyDimension getPkDimension() {
		return null;
	}
	private DbTable cachedTable;
	private DatabaseDialect bindProfile;
	private QueryClause0 loadSql;
	
	public DbTable getBaseTable(DatabaseDialect profile){
		if(bindProfile!=profile){
			synchronized (this) {
				initCache(profile);
			}
		}
		return cachedTable; 
	}

	private void initCache(DatabaseDialect profile) {
		bindProfile=profile;
		cachedTable=new DbTable(bindDsName, profile.getObjectNameIfUppercase(getTableName(true)),false,false);
		loadSql=caclLoadSql(profile);
	}


	public QueryClause0 getLoadByPkSql(DatabaseDialect profile,Query<?> q) {
		if(bindProfile!=profile){
			synchronized (this) {
				initCache(profile);
			}
		}
		return loadSql;
	}

	private QueryClause0 caclLoadSql(DatabaseDialect profile) {
		
		StringBuilder sb=new StringBuilder("select ");
		if(ORMConfig.getInstance().isSpecifyAllColumnName()){
			
		}else{
			sb.append('*');
		}
		sb.append(" from %1$s where ");
		int n=0;
		for(MappingType<?> field: this.getPKFields()){
			if(n>0){
				sb.append(" and ");
			}
			sb.append(field.getColumnName(profile, true)).append("=?");
			n++;
		}
		return new QueryClause0(sb.toString());
	}
	
	static class QueryClause0 implements QueryClause{
		private String sql;
		private List<BindVariableDescription> bind;
		private PartitionResult[] sites;;
		public QueryClause0(String string) {
			this.sql=string;
		}

		@Override
		public CacheKey getCacheKey() {
			//TODO 补充此方法
			return null;
		}

		@Override
		public BindSql getSql(PartitionResult site) {
			if(site.tableSize()==1){
				String s= String.format(sql, site.getAsOneTable());
				return new BindSql(s, bind);
			}else{
				StringBuilder sb=new StringBuilder();
				int n=0;
				for(String table: site.getTables()){
					if(n>0){
						sb.append("\n union all \n");
					}
					sb.append(String.format(sql, table));
				}
				 List<BindVariableDescription> bind = SqlAnalyzer.repeat(this.bind,  site.tableSize());
				 return new BindSql(sb.toString(), bind);
			}
		}

		@Override
		public PartitionResult[] getTables() {
			return sites;
		}

		@Override
		public OrderClause getOrderbyPart() {
			return null;
		}

		@Override
		public SelectPart getSelectPart() {
			return null;
		}

		@Override
		public boolean isGroupBy() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void setOrderbyPart(OrderClause orderClause) {
		}

		@Override
		public void setPageRange(IntRange range) {
		}

		@Override
		public boolean isMultiDatabase() {
			return sites!=null && sites.length>1;
		}

		@Override
		public GroupClause getGrouphavingPart() {
			return null;
		}

		@Override
		public boolean isDistinct() {
			return false;
		}
	}
}
