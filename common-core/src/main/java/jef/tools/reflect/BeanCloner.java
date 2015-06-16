package jef.tools.reflect;

import jef.accelerator.cglib.beans.BeanCopier;

final class BeanCloner extends Cloner{
	private BeanCopier bc;
	
	public BeanCloner(BeanCopier create) {
		this.bc=create;
	}

	@Override
	public Object clone(Object object,int restLevel) {
		Object result=bc.createInstance();
		if(restLevel>0){
			bc.copy(object, result, new CloneUtils.CloneConvert(restLevel-1));
		}else{
			bc.copy(object, result, CloneUtils.clone_cvt_dummy);
		}
		return result;
	}
}
