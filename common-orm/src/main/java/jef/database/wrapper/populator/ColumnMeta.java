package jef.database.wrapper.populator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.database.query.SqlContext;
import jef.tools.StringUtils;

public class ColumnMeta{
	ColumnDescription[] columns;
	/**
	 * 基于原始的NAME到备注的索引(KEY全大写)
	 */
	Map<String,ColumnDescription> nameIndex;
	/**
	 * 基于多个解析后的schema
	 */
	private Map<String,ColumnDescription[]> schemaIndex;
	
	/**
	 *构造
	 * <p>Title: </p>
	 * <p>Description:</p>
	 * @param columnNames
	 */
	public ColumnMeta(List<ColumnDescription> columnList) {
		this.columns=columnList.toArray(new ColumnDescription[columnList.size()]);
		initName();
	}
	
	private void initName(){
		nameIndex=new HashMap<String,ColumnDescription>(16,0.6f);
		for(ColumnDescription c: columns){
			nameIndex.put(c.getName().toUpperCase(), c);
		}
	}
	
	/**
	 * 按序号返回ColumnDescription
	 * @param n
	 * @return
	 */
	public ColumnDescription getN(int n){
		return columns[n];
	}

	//初始化Schema
	public void initSchemas(Transformer transformers){
		if(schemaIndex!=null)return;
		transformers.prepareTransform(nameIndex);//注意这个方法必须在ignoreSchema操作之前进行计算，否则会造成自定义Mapper找不到需要的列。
		
		Map<String,List<ColumnDescription>> main=new HashMap<String,List<ColumnDescription>>();
		for(ColumnDescription c:columns){
			String s=c.getName();
			if(transformers.hasIgnoreColumn(s.toUpperCase())){
				continue;
			}
			int n=s.indexOf(SqlContext.DIVEDER);
			String schema=(n>-1)?s.substring(0,n):"";
			if(transformers.hasIgnoreSchema(schema.toUpperCase())){
				continue;
			}
			
			c.setSimpleName((n>-1)?s.substring(n+SqlContext.DIVEDER.length()):s);
			List<ColumnDescription> list=main.get(schema);
			if(list==null){
				list=new ArrayList<ColumnDescription>();
				list.add(c);
				main.put(schema, list);
			}else{
				list.add(c);
			}
		}
		schemaIndex=new HashMap<String,ColumnDescription[]>();
		for(String key: main.keySet()){
			List<ColumnDescription> list=main.get(key);
			schemaIndex.put(key, list.toArray(new ColumnDescription[list.size()]));
		}
	}
	
	
	public ColumnDescription[] getColumns(String schema){
		return schemaIndex.get(schema);
	}
//	/**
//	 * 在所有的字段中查找SimpleName符合的
//	 * 
//	 * 2014/1 这个接口忽略大小写
//	 * @param fieldName
//	 * @return
//	 */
//	@Deprecated
//	public ColumnDescription findBySimpleName(String fieldName){
//		ColumnDescription[] columnsWithoutSchema=schemaIndex.get("");
//		if(columnsWithoutSchema!=null && columnsWithoutSchema.length>0){
//			 return nameIndex.get(fieldName.toUpperCase());
//		}else{
//			for(String key:schemaIndex.keySet()){
//				for(ColumnDescription value:schemaIndex.get(key)){
//					if(fieldName.equals(value.getSimpleName())){
//						return value;
//					}
//				}
//			}	
//			return null;
//		}
//	}
	
	public ColumnDescription getByFullName(String fieldName){
		return this.nameIndex.get(fieldName.toUpperCase());
	}
	
	public Set<String> getSchemas(){
		return schemaIndex.keySet();
	}
	
	@Override
	public String toString() {
		if(this.schemaIndex==null){
			StringBuilder sb=new StringBuilder();
			for(ColumnDescription c: this.columns){
				sb.append(c.getName()).append(',');
			}
			sb.setLength(sb.length()-1);
			return sb.toString();
		}else{
			StringBuilder sb=new StringBuilder();
			for(String key:schemaIndex.keySet()){
				sb.append(key).append(":{").append(StringUtils.join(schemaIndex.get(key), ",")).append('}');
				sb.append('\n');
			}
			return sb.toString();	
		}
	}
	
	public ColumnDescription[] getColumns() {
		return columns;
	}

	public int length() {
		return columns.length;
	}
}
