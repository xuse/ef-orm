package jef.database.routing.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * @author junyu
 * 
 */
public class QueryReturn{
	private Statement statement;
	private ResultSet resultset;
	private List<SQLException> exceptions;
	private String currentDBIndex;

	public Statement getStatement() {
		return statement;
	}

	public void add2ExceptionList(SQLException e){
	   if(null==this.exceptions){
		   this.exceptions=new LinkedList<SQLException>();
	   }
	   
	   this.exceptions.add(e);
	}
	
	public void setStatement(Statement statement) {
		this.statement = statement;
	}

	public ResultSet getResultset() {
		return resultset;
	}

	public void setResultset(ResultSet resultset) {
		this.resultset = resultset;
	}

	public List<SQLException> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<SQLException> exceptions) {
		this.exceptions = exceptions;
	}

	public String getCurrentDBIndex() {
		return currentDBIndex;
	}

	public void setCurrentDBIndex(String currentDBIndex) {
		this.currentDBIndex = currentDBIndex;
	}
}
