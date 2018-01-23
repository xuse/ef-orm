package jef.database.exception;

import java.sql.SQLException;

/**
 * Defines a contract for implementations that can extract the name of a
 * violated constraint from a SQLException that is the result of that constraint
 * violation.
 */
@FunctionalInterface
public interface ViolatedConstraintNameExtracter {
	/**
	 * Extract the name of the violated constraint from the given SQLException.
	 * 
	 * @param sqle
	 *            The exception that was the result of the constraint violation.
	 * @return The extracted constraint name.
	 */
	public String extractConstraintName(SQLException sqle);
}
