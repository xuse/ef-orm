package jef.database.wrapper.variable;

import java.sql.SQLException;

import jef.database.query.Query;
import jef.tools.reflect.BeanWrapper;


public abstract class Variable {
	abstract String name();
	
	abstract Object jdbcSet(BindVariableContext context,int index,BeanWrapper bean,Query<?> query)throws SQLException ;
}
