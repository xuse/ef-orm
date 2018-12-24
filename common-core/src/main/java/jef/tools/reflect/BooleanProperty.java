package jef.tools.reflect;

public interface BooleanProperty extends Property {
	boolean getBoolean(Object obj);

	void setBoolean(Object obj, boolean value);
}
