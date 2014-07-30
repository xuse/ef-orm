package jef.database.cache;

import java.io.StringReader;

import javax.persistence.PersistenceException;

import jef.database.jsqlparser.VisitorAdapter;
import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.parser.TokenMgrError;
import jef.database.jsqlparser.schema.Column;
import jef.tools.StringUtils;

public class KeyDimension {
	private String where;
	private String order;
	
	static{
		String a="A123B";
		if(a.toUpperCase()!=a){//String的实现必须满足大写字符串取大写还是本身的要求
			throw new UnsupportedClassVersionError("The JDK Implementation is too old!");
		}
	}
	
	public KeyDimension(String where,String order) {
		if(StringUtils.isEmpty(where)){
			this.where=where;
		}else{
			StSqlParser parser= new StSqlParser(new StringReader(where));
			try {
				Expression exp=parser.WhereClause();
				removeAliasAndCase(exp);
				this.where=exp.toString();	
			} catch (ParseException e) {
				throw new PersistenceException("["+where+"]",e);
			}catch(TokenMgrError e){
				throw new PersistenceException("["+where+"]",e);
			}	
		}
		this.order=order==null?"":order;;
	
	}

	public KeyDimension(Expression where2, Expression order2) {
		removeAliasAndCase(where2);
		this.where=where2.toString();
		this.order=order2==null?"":order2.toString();
	}

	private void removeAliasAndCase(Expression exp) {
		exp.accept(new VisitorAdapter(){
			public void visit(Column tableColumn) {
				super.visit(tableColumn);
				tableColumn.setTableAlias(null);
				String s=tableColumn.getColumnName();
				String s2=s.toUpperCase();
				if(s2!=s){
					tableColumn.setColumnName(s2);
				}
			}
		});
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
		new KeyDimension("  where t.task_id=? and t.\"key\"=?",null);
	}
}
