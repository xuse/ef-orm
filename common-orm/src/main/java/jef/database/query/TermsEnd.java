package jef.database.query;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Or;

public class TermsEnd {
	private ConditionAccessor context;
	private Condition lastCond;
	private Query<?> query;
	
	private Terms parent;

	public TermsEnd(ConditionAccessor lastContainer,Query<?> q,Terms parent) {
		this.context=lastContainer;
		this.query=q;
		this.parent=parent;
	}

	public TermsConnector or() {
		if(parent instanceof TermsConnector){
			TermsConnector connector=(TermsConnector)parent;
			if(!connector.bracket){
				if(connector.level()==1){
					return connector;
				}else{//OR的优先级小于END (>1)
					Terms terms=connector.getParent();
					return terms.end.set(connector.getWrappedCondition()).or();
				}	
			}
		}
		if(context.remove(lastCond)){
			Or or=new Or();
			or.addCondition(lastCond);
			Condition cond=Condition.get(or, Operator.EQUALS, null);
			context.add(cond);
			return new TermsConnector(new ConditionAccessor.I(or,cond),query,1,parent);
		}else{
			throw new IllegalStateException();
		}
	}
	
	public TermsConnector and() {
		if(parent instanceof TermsConnector){
			TermsConnector connector=(TermsConnector)parent;
			if(!connector.bracket){
				if(connector.level()==2){
					return (TermsConnector)parent;
				}else if(connector.level()>2){//not
					Terms terms=connector.getParent();
					return terms.end.set(connector.getWrappedCondition()).and();
				}
			}
		}
		if(context.remove(lastCond)){
			And and=new And();
			and.addCondition(lastCond);
			Condition cond=Condition.get(and, Operator.EQUALS, null);
			context.add(cond);
			return new TermsConnector(new ConditionAccessor.I(and,cond),query,2,parent);
		}else{
			throw new IllegalStateException();
		}
	}

	public TermsEnd set(Condition cond) {
		lastCond=cond;
		return this;
	}

	public TermsEnd R$() {
		if(parent instanceof TermsConnector){
			TermsConnector connector=(TermsConnector)parent;
			if(connector.getParent().isBracket()){
				((TermsConnector)connector.getParent()).bracket=false;
			}
			return connector.getParent().end.set(connector.getWrappedCondition());
			
		}
		return this;
	}
}
