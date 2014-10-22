package jef.database.cache;

import jef.database.cache.WhereParser.DruidImpl;
import jef.database.cache.WhereParser.NativeImpl;
import jef.database.dialect.statement.UnionJudgement;
import jef.database.jsqlparser.visitor.Expression;

public class KeyDimension {
	private String where;
	private String order;
	
	
	public KeyDimension(String where,String order) {
		if(where==null || where.length()==0){
			this.where=where;
		}else{
			this.where=wp.process(where);
		}
		this.order=order==null?"":order;
	}

	public KeyDimension(Expression where2, Expression order2) {
		WhereParser.removeAliasAndCase(where2);
		this.where=where2.toString();
		this.order=order2==null?"":order2.toString();
	}

	@Override
	public int hashCode() {
		return where.hashCode()+order.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof KeyDimension){
			KeyDimension rhs=(KeyDimension) obj;
			return this.where.equals(rhs.where) && this.order.equals(rhs.order);
		}
		return false;
	}

	@Override
	public String toString() {
		if(order==null){
			return where;
		}else{
			return where+order;
		}
	}
	
	public static void main(String[] args) {
		long time=System.currentTimeMillis();
		for(int i=0;i<10000;i++){
			KeyDimension k=new KeyDimension("  where t.task_id=? and t.\"key\"=?",null);
		}
		System.out.println(System.currentTimeMillis()-time);
		
		KeyDimension k=new KeyDimension("  where t.task_id=? and t.\"key\"=?",null);
		System.out.println(k.where);
		System.out.println(k.order);
	}
	
	private static final WhereParser wp;
	
	static{
		if(UnionJudgement.isDruid()){
			wp=new DruidImpl();
		}else{
			wp=new NativeImpl();
		}
	}
}
