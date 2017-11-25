package jef.database.support;

public class EntityNotEnhancedException extends RuntimeException{
	private static final long serialVersionUID = 3491487669935469315L;

	public EntityNotEnhancedException(String message) {
		super(message+" was not enhanced. Please add '-javaagent:<path of spring-instrument.jar>' as a VM arg");
	}
}
