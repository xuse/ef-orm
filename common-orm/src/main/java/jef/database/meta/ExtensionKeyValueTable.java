package jef.database.meta;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.OneToMany;

import jef.database.IQueryableEntity;
import jef.database.annotation.DynamicKeyValueExtension;
import jef.database.annotation.JoinType;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.ColumnMapping;
import jef.database.support.accessor.KvEntensionProperty;

public final class ExtensionKeyValueTable extends AbstractExtensionConfig implements ExtensionConfigFactory {
	private DynamicKeyValueExtension config;
	private TupleMetadata kvTable;
	private JoinPath joinPath;
	public ExtensionKeyValueTable(DynamicKeyValueExtension dkv, Class<?> entityClass,AbstractMetadata parent) {
		super(dkv.metadata(),parent);
		this.config=dkv;
		// 创建KV表
		TupleMetadata tuple = new TupleMetadata(config.table());
		
		List<JoinKey> joinkeys=new ArrayList<JoinKey>();
		
		for (ColumnMapping<?> m : parent.getPKFields()) {
			ColumnType ct = m.get();
			if (ct instanceof ColumnType.GUID) {
				ct = ((ColumnType.GUID) ct).toNormalType();
			} else if (ct instanceof ColumnType.AutoIncrement) {
				ct = ((ColumnType.AutoIncrement) ct).toNormalType();
			}
			tuple.addColumn(m.fieldName(),m.rawColumnName(), ct,true);
			joinkeys.add(new JoinKey(m.field(),tuple.getField(m.fieldName())));
		}
		tuple.addColumn(config.keyColumn(),config.keyColumn(), new ColumnType.Varchar(64).notNull(),true);
		tuple.addColumn(config.valueColumn(), new ColumnType.Varchar(4000));
		this.kvTable=tuple;
		this.joinPath=new JoinPath(JoinType.INNER,joinkeys.toArray(new JoinKey[joinkeys.size()]));
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
		if(parent instanceof TableMetadata){
			TableMetadata tm=(TableMetadata)parent;
			tm.extendContainer=getContainerTuple();
			tm.extendMeta=getExtensionMeta();
			return tm;
		}else{
			throw new IllegalArgumentException(parent.getClass().getName());
		}
	}

	public ITableMetadata getRawMetadata() {
		return parent;
	}


	public void initMeta() {
		TableMetadata tm=(TableMetadata)parent;
		CascadeConfig config=new CascadeConfig(null,(OneToMany)null);
		config.path=joinPath;
		KvEntensionProperty property=new KvEntensionProperty("attributes",kvTable,parent,this.getExtensionMeta(),this.config);
		ReferenceObject ref=tm.innerAdd(property,kvTable, config);
		ref.setPriority(5);
//		tm.addRefField_1vsN(Map.class, , tm.extendContainer, config);
	}
}
