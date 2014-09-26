package jef.database.test.jdbc;

public enum OperType {
	create_connection,
	close_connection,
	
	set_connection_attr,
	set_statement_attr,
	set_resultset_attr,
	
	set_autocommit_false,
	set_autocommit_true,
	commit,
	rollback,
	
	preapre_statement,
	close_prepared_statement,
	
	create_statement,
	close_statement,
	
	create_resultSet,
	close_resultSet,
	
	execute_update,
	execute_batch,
	
	create_savepoint,
	release_savepoint,
	rollback_savepoint
}
