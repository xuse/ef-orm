package jef.database.meta;

import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;

public final class ReferenceObject extends AbstractRefField implements IReferenceAllTable{
	private boolean lazyLob;
	
	public ReferenceObject(Class<?> fType,String fName, Reference ref,CascadeConfig config) {
		super(fType,fName, ref,config==null?null:config.asMap);
		lazyLob=ORMConfig.getInstance().isEnableLazyLoad() && this.reference.getTargetType().getLobFieldNames()!=null;
	}

	public boolean isSingleColumn() {
		return false;
	}

	public ITableMetadata getFullModeTargetType() {
		return this.getReference().getTargetType();
	}

	public String simpleModeSql(String tableAlias) {
		return null;
	}

	public String getSelectedAliasOf(ColumnMapping<?> f, DatabaseDialect dialect, String schema) {
		return AliasProvider.DEFAULT.getSelectedAliasOf(f, dialect, schema);
	}
	@Override
	public String getResultAliasOf(ColumnMapping<?> f, String schema) {
		return AliasProvider.DEFAULT.getResultAliasOf(f, schema);
	}
	
	public boolean isLazyLob() {
		return lazyLob;
	}
	@Override
	public ISelectProvider toNestedDesc(String lastName) {
		if(lastName==null){
			return this;
		}
		return new NestedReferenceObject(lastName.concat(".").concat(getName()));
	}
	private class NestedReferenceObject implements IReferenceAllTable{
		String name;
		public NestedReferenceObject(String newName) {
			this.name=newName;
		}
		
		public String getName() {
			return name;
		}

		public int getProjection() {
			return 0;
		}

		public boolean isSingleColumn() {
			return false;
		}

		public ITableMetadata getFullModeTargetType() {
			return ReferenceObject.this.getFullModeTargetType();
		}

		public String simpleModeSql(String tableAlias) {
			return null;
		}

		public String getSelectedAliasOf(ColumnMapping<?> f, DatabaseDialect dialect, String schema) {
			return AliasProvider.DEFAULT.getSelectedAliasOf(f, dialect, schema);
		}
		@Override
		public String getResultAliasOf(ColumnMapping<?> f, String schema) {
			return AliasProvider.DEFAULT.getResultAliasOf(f, schema);
		}
		public boolean isLazyLob() {
			return lazyLob;
		}
	}
}
