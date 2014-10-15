package jef.database.dialect;

public interface SqlTypeSized {
	int getLength();

	int getPrecision();

	int getScale();
}
