package jef.database.meta;

import jef.database.IQueryableEntity;
import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.ColumnMapping;

public final class ExtensionKeyValueTable extends AbstractExtensionConfig implements ExtensionConfigFactory {
	private DynamicKeyValueExtension config;
	private TupleMetadata kvTable;
	
	public ExtensionKeyValueTable(DynamicKeyValueExtension dkv, Class<?> entityClass,AbstractMetadata parent) {
		super(dkv.metadata(),parent);
		this.config=dkv;
		// 创建KV表
		TupleMetadata tuple = new TupleMetadata(config.table());
		for (ColumnMapping<?> m : parent.getPKFields()) {
			ColumnType ct = m.get();
			if (ct instanceof ColumnType.GUID) {
				ct = ((ColumnType.GUID) ct).toNormalType();
			} else if (ct instanceof ColumnType.AutoIncrement) {
				ct = ((ColumnType.AutoIncrement) ct).toNormalType();
			}
			tuple.addColumn(m.fieldName(),m.rawColumnName(), ct,true);
		}
		tuple.addColumn(config.keyColumn(),config.keyColumn(), new ColumnType.Varchar(64),true);
		tuple.addColumn(config.valueColumn(), new ColumnType.Varchar(4000));
		this.kvTable=tuple;
	}


	@Override
	public boolean isDynamicTable() {
		return false;
	}

	public DynamicKeyValueExtension getConfig(){
		return config;
	}

	public TupleMetadata getContainerTuple() {
		return kvTable;
	}

	@Override
	public ExtensionConfig getDefault() {
		return this;
	}

	@Override
	public ExtensionConfig getExtension(String extensionName) {
		return this;
	}

	@Override
	public ExtensionConfig getExtension(IQueryableEntity q) {
		return this;
	}


	@Override
	protected AbstractMetadata merge() {
//		DynamicMetadata tuple = new DynamicMetadata(parent,this);
//		for (ColumnMapping<?> f : getExtensionMeta().getColumns()) {
//			tuple.updateColumn(f.fieldName(), f.rawColumnName(), f.get(), f.isPk());
//		}
		return parent;
	}

	public ITableMetadata getRawMetadata() {
		return parent;
	}
}
