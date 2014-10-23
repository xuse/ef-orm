package jef.database.query;

import java.util.List;

import jef.database.Condition;
import jef.database.IConditionField;
import jef.database.IConditionField.Not;

public abstract class ConditionAccessor {
	private List<Condition> removeHandler;

	ConditionAccessor(List<Condition> list) {
		this.removeHandler = list;
	}

	abstract Condition add(Condition cond);

	public boolean remove(Condition cond) {
		return removeHandler.remove(cond);
	}

	final static class Q extends ConditionAccessor {
		private Query<?> query;

		Q(Query<?> q) {
			super(q.getConditions());
			this.query = q;
		}

		@Override
		Condition add(Condition cond) {
			query.addCondition(cond);
			return cond;
		}
	}

	final static class I extends ConditionAccessor {
		private IConditionField ic;
		public Condition wrapped;
		
		ConditionAccessor parent;
		
		@Override
		public boolean remove(Condition cond) {
			if(ic instanceof Not){
				Not not=(Not)ic;
				return not.get()==cond;
			}else{
				return super.remove(cond);
			}
		}

		I(IConditionField cond,Condition wrapped) {
			super(cond.getConditions());
			this.wrapped=wrapped;
			this.ic = cond;
		}

		@Override
		Condition add(Condition cond) {
			if(ic instanceof Not){
				((Not) ic).set(cond);
				if(parent!=null){
					parent.add(wrapped);
					parent=null;	
				}
			}else{
				ic.getConditions().add(cond);
			}
			return cond;
		}
	}
}
