package jef.tools.reflect;

import jef.accelerator.cglib.beans.BeanCopier;

final class BeanCloner extends Cloner{
	private BeanCopier bc;
	
	public BeanCloner(BeanCopier create) {
		this.bc=create;
	}

	@Override
	public Object clone(Object object,boolean deep) {
		Object result=bc.createInstance();
		if(deep){
			bc.copy(object, result, CloneUtils.clone_cvt_deep);
		}else{
			bc.copy(object, result, CloneUtils.clone_cvt_safe);
		}
		return result;
	}
}
