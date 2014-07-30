package jef.database.query;

import jef.database.DbFunction;

import org.apache.commons.lang.ArrayUtils;

public class DbFunctionCall {
	private DbFunction func;
	private String[] args;
	private String name;
	private int varIndex = -1;
	
	DbFunctionCall(String func,String[] args){
		this.name=func;
		initArgs(args);
	}
	
	private void initArgs(String[] args) {
		this.args=args;
		if(args.length>0){
			varIndex=ArrayUtils.indexOf(args,"?");
		}
	}
	
	

	public int getVarIndex() {
		return varIndex;
	}

	DbFunctionCall(DbFunction func,String[] args){
		this.func=func;
		initArgs(args);
	}
	
	public DbFunction getFunc() {
		return func;
	}
	public void setFunc(DbFunction func) {
		this.func = func;
	}
	public String[] getArgs() {
		return args;
	}
	public void setArgs(String[] args) {
		this.args = args;
	}

	public String getName() {
		return name;
	}
}
