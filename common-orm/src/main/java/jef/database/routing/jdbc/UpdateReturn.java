package jef.database.routing.jdbc;

import java.sql.SQLException;
import java.util.List;

/**
 * @author junyu
 *
 */
public class UpdateReturn{
    private int affectedRows;
    private List<SQLException> exceptions;
	
    public int getAffectedRows() {
		return affectedRows;
	}
	public void setAffectedRows(int affectedRows) {
		this.affectedRows = affectedRows;
	}
	public List<SQLException> getExceptions() {
		return exceptions;
	}
	public void setExceptions(List<SQLException> exceptions) {
		this.exceptions = exceptions;
	}
}
