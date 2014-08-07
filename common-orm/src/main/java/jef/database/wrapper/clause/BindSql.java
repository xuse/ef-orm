package jef.database.wrapper.clause;

import java.util.List;

import jef.database.BindVariableDescription;

/**
 * 描述一个绑定变量的SQL语句
 * @author jiyi
 *
 */
public final class BindSql {
	private String sql;
	private List<BindVariableDescription> bind;
	
	public BindSql(String sql){
		this.sql=sql;
	}
	
	public BindSql(String sql,List<BindVariableDescription> bind){
		this.sql=sql;
		this.bind=bind;
	}
	/**
	 * 得到SQL语句
	 * @return SQL语句
	 */
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	/**
	 * 得到绑定变量描述
	 * @return 绑定变量描述
	 */
	public List<BindVariableDescription> getBind() {
		return bind;
	}
	public void setBind(List<BindVariableDescription> bind) {
		this.bind = bind;
	}
	public boolean isBind(){
		return bind!=null && bind.size()>0;
	}
	@Override
	public String toString() {
		return sql;
	}
	
}
