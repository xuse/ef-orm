package jef.database.meta;

import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.tools.reflect.Property;

public final class ReferenceObject extends AbstractRefField implements IReferenceAllTable{
	/**
	 * 是否延迟加载Lob字段
	 */
	private boolean lazyLob;
	
	public ReferenceObject(Property fName, Reference ref,CascadeConfig config) {
		super(fName, ref,config);
		lazyLob=ORMConfig.getInstance().isEnableLazyLob() && this.reference.getTargetType().getLobFieldNames()!=null;
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

	public String getSelectedAliasOf(ColumnMapping f, DatabaseDialect dialect, String schema) {
		return AliasProvider.DEFAULT.getSelectedAliasOf(f, dialect, schema);
	}
	@Override
	public String getResultAliasOf(ColumnMapping f, String schema) {
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

		public String getSelectedAliasOf(ColumnMapping f, DatabaseDialect dialect, String schema) {
			return AliasProvider.DEFAULT.getSelectedAliasOf(f, dialect, schema);
		}
		@Override
		public String getResultAliasOf(ColumnMapping f, String schema) {
			return AliasProvider.DEFAULT.getResultAliasOf(f, schema);
		}
		public boolean isLazyLob() {
			return lazyLob;
		}
	}
}
