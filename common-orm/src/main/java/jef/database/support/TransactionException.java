package jef.database.support;

/**
 * Superclass for all transaction exceptions.
 *
 */
public abstract class TransactionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7603126306379761942L;

	/**
	 * Constructor for TransactionException.
	 * @param msg the detail message
	 */
	public TransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
