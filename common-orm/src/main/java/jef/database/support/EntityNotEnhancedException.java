package jef.database.support;

public class EntityNotEnhancedException extends RuntimeException{

	public EntityNotEnhancedException(String message) {
		super(message+" was not enhanced.");
	}
}
