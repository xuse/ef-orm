package jef.database;

public interface ConnectionFactory {
	public abstract WrappedConnection startTransaction();

	public abstract void shutdown();
}
