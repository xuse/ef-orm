package jef.database.query;

import jef.database.Condition;



public class TermsConnector extends Terms {
	private int level;
	private Condition ic;
	private Terms parent;

	/**
	 * 
	 * @param accessor
	 * @param query
	 * @param level OR 1 AND 2 NOT 3
	 * @param parent
	 */
	public TermsConnector(ConditionAccessor.I accessor,Query<?> query,int level,Terms parent) {
		super(accessor,query);
		this.level=level;
		this.ic=accessor.wrapped;
		this.parent=parent;
	}
	
	public int level(){
		return level;
	}
	
	Condition getWrappedCondition(){
		return ic;
	}
	
	public Terms getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return ic.toString();
	}
}
