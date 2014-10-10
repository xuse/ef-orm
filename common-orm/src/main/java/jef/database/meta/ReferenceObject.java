package jef.database.meta;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;

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

	public String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema,boolean forSelect) {
		return DbUtils.getDefaultColumnAlias(f, profile, schema);
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

		public String getSelectedAliasOf(Field f, DatabaseDialect profile, String schema,boolean forSelect) {
			return DbUtils.getDefaultColumnAlias(f, profile, schema);
		}
		public boolean isLazyLob() {
			return lazyLob;
		}
	}
}
