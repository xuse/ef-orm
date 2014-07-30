package jef.database.support;

public class SavepointNotSupportedException extends TransactionException{
	private static final long serialVersionUID = 1L;
	
	public SavepointNotSupportedException(String msg) {
		super(msg);		
	}
	public SavepointNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
