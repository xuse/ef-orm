package jef.database.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.QueryAlias;
import jef.database.meta.IReferenceAllTable;
import jef.database.meta.IReferenceColumn;
import jef.database.meta.ISelectProvider;
import jef.database.meta.ITableMetadata;
import jef.database.meta.Reference;

public abstract class AbstractEntityMappingProvider{
	protected List<QueryAlias> queries;// 对于连接的Query.从驱动表向后依次递增
	protected boolean distinct = false;

	public List<ISelectItemProvider> getReference() {
		List<ISelectItemProvider> result = new ArrayList<ISelectItemProvider>();
		result.addAll(queries);
		return result;
	}

	protected QueryAlias findQuery(ITableMetadata clz,Reference ref) {
		QueryAlias found=null;
		for(ISelectItemProvider q: this.queries){
			if(q.getTableDef().getMeta()==clz){
				QueryAlias al=(QueryAlias)q;
				if(ref==null || ref.equals(al.getStaticRef())){
					if(found!=null){
						LogUtil.warn("there's more than one table "+ clz +" in the join query.");
					}
					found=al;	
				}
			}
		}
		return found;
	}

	protected QueryAlias findQuery(Query<?> t2) {
		for (ISelectItemProvider q : this.queries) {
			if (q.getTableDef() == t2) {
				return (QueryAlias) q;
			}
		}
		return null;
	}

	public Entry<String[], ISelectItemProvider[]> getPopulationDesc() {
		List<String> directSchema = new ArrayList<String>(5);
		List<ISelectItemProvider> ref = new ArrayList<ISelectItemProvider>(5);//直接装配到基本对象上
		for (ISelectItemProvider select : queries) {
			if (select.getReferenceObj()==null && select.getReferenceCol().isEmpty()) {
				directSchema.add(select.getSchema());
				continue;
			}
			IReferenceAllTable all=select.getReferenceObj();
			if(all!=null && all.getProjection()==ISelectProvider.PROJECTION_NOT_SELECT){
				all=null;
			}
			
			List<IReferenceColumn> reference = select.getReferenceCol();
			int fieldCount=0;
			for (IReferenceColumn field : reference) {
				int projection=field.getProjection();
				//JIYI 2013, if a select desc, is for no select, then this can be skip on result population
				if(projection==ISelectProvider.PROJECTION_HAVING_NOT_SELECT){
					continue;
				}
				if (field.getName() == null && !directSchema.contains(select.getSchema())) {
					ref.add(select.copyOf(Arrays.asList(field),null)); //单独处理一下 why
				}else{
					fieldCount++;
				}
			}
			if(fieldCount>0 || all!=null){
				ref.add(select);
			}
		}
		Entry<String[], ISelectItemProvider[]> entry = new Entry<String[], ISelectItemProvider[]>();
		entry.setKey(directSchema.toArray(new String[directSchema.size()]));
		entry.setValue(ref.toArray(new ISelectItemProvider[ref.size()]));
		return entry;
	}
	
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isDistinct() {
		return distinct;
	}
	
	public boolean isMultiTable(){
		return getReference().size()>1;
	}
}
