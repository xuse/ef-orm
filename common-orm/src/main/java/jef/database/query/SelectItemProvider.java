package jef.database.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;
import jef.database.meta.AliasProvider;
import jef.database.meta.IReferenceAllTable;
import jef.database.meta.IReferenceColumn;
import jef.database.meta.ISelectProvider;
import jef.database.meta.ITableMetadata;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;

public abstract class SelectItemProvider implements ISelectItemProvider {
	//单表请求
	protected Query<?> table;

	//该请求对应的表别名
	protected String schema;
	
	//选择列的内容和装配目的(单个装配)
	protected IReferenceAllTable referenceObj;//可以为null，表示拼装到基本对象上
	protected List<IReferenceColumn> referenceCol;//可以为null，表示拼装到基本对象上
	
	protected SelectItemProvider(String schema,Query<?> table){
		this.schema=schema;
		this.table=table;
	}
	
	public IReferenceAllTable getReferenceObj() {
		return referenceObj;
	}
	
	@SuppressWarnings("unchecked")
	public List<IReferenceColumn> getReferenceCol() {
		return referenceCol==null?Collections.EMPTY_LIST:referenceCol;
	}


	public String getSchema() {
		return schema;
	}
	public void addField(IReferenceAllTable field){
		if(field==null)return;
		this.referenceObj=field;
	}
	
	public void addField(IReferenceColumn field){
		if(referenceCol==null)referenceCol=new ArrayList<IReferenceColumn>();
		this.referenceCol.add(field);
	}
	
	public void setFields(IReferenceAllTable all,List<IReferenceColumn> field){
		this.referenceObj=all;
		this.referenceCol=field;
	}
	public void setFields(IReferenceAllTable all,IReferenceColumn... field){
		this.referenceObj=all;
		if(field.length==0){
			referenceCol=null;	
		}else{
			referenceCol=ArrayUtils.asList(field);	
		}
	}
	public boolean isAllTableColumns(){
		if(referenceCol!=null && referenceCol.size()>1)return false;
		if(referenceObj==null){
			return false;
		}
		return referenceObj.getProjection()==ISelectProvider.PROJECTION_NORMAL;
	}
	
	
	private static final AliasProvider EMPTY=new AliasProvider(){
		public String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema) {
			return null;
		}
	};
	/**
	 *生成查询语句时使用
	 */
	public CommentEntry[] getSelectColumns(DatabaseDialect profile,boolean groupMode,SqlContext context) {
		List<CommentEntry> result=new ArrayList<CommentEntry>();
		List<IReferenceColumn> assignedFields=this.getReferenceCol();
		AliasProvider aliasProvider=context.isMultiTable()? AliasProvider.DEFAULT: EMPTY;
		
		//所有配置为空，采取默认值
		if(assignedFields.isEmpty() && referenceObj==null){
			if(!groupMode){//group模式下全部无效
				ITableMetadata meta=table.getMeta();
				for (MappingType<?> f : meta.getMetaFields()) {
					CommentEntry entry=new CommentEntry();
					entry.setKey(schema.concat(".").concat(f.getColumnName(profile, true)));
					entry.setValue(aliasProvider.getSelectedAliasOf(f.field(), profile, schema));	
					result.add(entry);
				}	
			}
			return result.toArray(new CommentEntry[result.size()]);
		}
		//全表列
		if(referenceObj!=null && referenceObj.getProjection()!=ISelectProvider.PROJECTION_NOT_SELECT){
			String sql=referenceObj.simpleModeSql(schema);
			if(sql!=null){
				String countAlias=((AllTableColumns)referenceObj).getCountAlias();
				result.add(new CommentEntry(sql,countAlias));
			}else{
				if(!groupMode){
					ITableMetadata meta=table.getMeta();
					for (MappingType<?> f : meta.getMetaFields()) {
						if(referenceObj.isLazyLob()){
							if(ArrayUtils.fastContains(meta.getLobFieldNames(), f.field())){
								continue;
							}
						}
						
						CommentEntry entry=new CommentEntry();
						entry.setKey(schema.concat(".").concat(f.getColumnName(profile,true)));
						if(referenceObj==null){
							entry.setValue(DbUtils.getDefaultColumnAlias(f.field(), profile, schema));	
						}else{
							entry.setValue(referenceObj.getSelectedAliasOf(f.field(),profile,schema));	
						}
						result.add(entry);
					}
					return result.toArray(new CommentEntry[result.size()]);
				}
			}
		}
		//其他列
		for(IReferenceColumn f: assignedFields){
			if(groupMode && f.getProjection()==ISelectProvider.PROJECTION_NORMAL)continue;//group模式下，普通字段无效
			CommentEntry entry=new CommentEntry();
			String column=f.getSelectItem(profile, schema,context);
			if(column==null)continue;
			entry.setKey(column);
			entry.setValue(f.getSelectedAlias(schema,profile,true));
			result.add(entry);
		}
		return result.toArray(new CommentEntry[result.size()]);
	}
	public Query<?> getTableDef() {
		return table;
	}
}
