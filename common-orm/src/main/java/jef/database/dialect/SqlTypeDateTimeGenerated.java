package jef.database.dialect;

public interface SqlTypeDateTimeGenerated {
	int getGenerateType();

	ColumnType setGenerateType(int dateGenerateType);
}
