package jef.accelerator.asm.commons;

import com.github.geequery.asm.FieldVisitor;

import jef.tools.Assert;

public abstract class FieldExtCallback {
	
	public FieldExtCallback(FieldVisitor v){
		this.visitor=v;
		Assert.notNull(v);
	}
	
	FieldVisitor visitor;
	
	
	public abstract void onFieldRead(FieldExtDef info);
}
