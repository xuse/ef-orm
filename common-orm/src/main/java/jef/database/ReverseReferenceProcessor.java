package jef.database;

import java.util.List;

import jef.database.meta.ISelectProvider;
import jef.database.meta.Reference;
import jef.database.meta.ReferenceField;
import jef.database.meta.ReferenceObject;
import jef.tools.reflect.BeanWrapper;

public class ReverseReferenceProcessor {

	List<Reference> refs;
	
	//应该说只剩下一种关系，那就是 Nv1关系
	public ReverseReferenceProcessor(List<Reference> reverse) {
		this.refs=reverse;
	}

	//处理1vsN引用下的反向装填. 同时由于1vn下有可能反向的nv1是延迟加载的，此时还需要将延迟任务给取消掉。
	public void process(Object obj, List<? extends IQueryableEntity> subs) {
		for(Reference ref :refs){
			for(ISelectProvider prov:ref.getAllRefFields()){
				if(prov.isSingleColumn()){
					ReferenceField r=(ReferenceField)prov;
					Object value=r.getTargetField().getFieldAccessor().get(obj);
					for(IQueryableEntity childObj:subs){
						BeanWrapper child=BeanWrapper.wrap(childObj, BeanWrapper.FAST);
						child.setPropertyValue(r.getName(), value);
					}
				}else{
					ReferenceObject r=(ReferenceObject)prov;
					for(IQueryableEntity childObj:subs){
						BeanWrapper child=BeanWrapper.wrap(childObj, BeanWrapper.FAST);
						child.setPropertyValue(r.getName(), obj);
					}		
				}
			}	
		}
	}
}
