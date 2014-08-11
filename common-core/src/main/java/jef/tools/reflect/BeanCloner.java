package jef.tools.reflect;

import jef.accelerator.cglib.beans.BeanCopier;

final class BeanCloner extends Cloner{
	private BeanCopier bc;
	
	public BeanCloner(BeanCopier create) {
		this.bc=create;
	}

	@Override
	public Object clone(Object object) {
		Object result=bc.createInstance();
		bc.copy(object, result, CloneUtils.clone_cvt);
		return result;
	}
}
