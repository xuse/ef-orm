package jef.database.support;

public class TransactionTimedOutException extends TransactionException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for TransactionTimedOutException.
	 * @param msg the detail message
	 */
	public TransactionTimedOutException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionTimedOutException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionTimedOutException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
