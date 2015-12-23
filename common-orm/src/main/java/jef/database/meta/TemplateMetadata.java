package jef.database.meta;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.accelerator.bean.BeanAccessor;
import jef.common.Entry;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.PojoWrapper;
import jef.database.annotation.Index;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AnnotationProvider.ClassAnnotationProvider;

import com.google.common.collect.Multimap;

@SuppressWarnings("rawtypes")
public class TemplateMetadata extends AbstractMetadata {
	private AbstractMetadata wrapped;
	private ExtensionTemplate template;

	public ExtensionTemplate getExtension() {
		return template;
	}

	public TemplateMetadata(ExtensionTemplate ef) {
		this.template = ef;
		this.wrapped = ef.getTemplate();
	}

	@Override
	public Class<? extends IQueryableEntity> getContainerType() {
		return wrapped.getContainerType();
	}

	@Override
	public ColumnMapping getColumnDef(Field field) {
		ColumnMapping result=schemaMap.get(field);
		if (result != null) {
			return result;
		}
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public Field getField(String name) {
		Field field = super.getField(name);
		if (field != null) {
			return field;
		}
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public Class<?> getThisType() {
		return wrapped.getThisType();
	}

	@Override
	public Field getFieldByLowerColumn(String columnInLowerCase) {
		Field field = wrapped.getFieldByLowerColumn(columnInLowerCase);
		if (field != null) {
			return field;
		}
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public List<ColumnMapping> getPKFields() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public List<Index> getIndexDefinition() {
		return wrapped.getIndexDefinition();
	}

	@Override
	public PartitionTable getPartition() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public Entry<PartitionKey, PartitionFunction>[] getEffectPartitionKeys() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public Multimap<String, PartitionFunction> getMinUnitFuncForEachPartitionKey() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public IQueryableEntity newInstance() {
//		throw new UnsupportedOperationException("this is a abstract metadata template.");
		return wrapped.newInstance();
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public String getSimpleName() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public PojoWrapper transfer(Object p, boolean isQuery) {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public EntityType getType() {
		return EntityType.TEMPLATE;
	}

	@Override
	public boolean containsMeta(ITableMetadata meta) {
		return false;
	}

	@Override
	public ExtensionConfig getExtension(IQueryableEntity d) {
		return template.getExtension(d);
	}

	@Override
	public ExtensionConfig getExtension(String d) {
		return template.getExtension(d);
	}

	@Override
	protected Collection<ColumnMapping> getColumnSchema() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public BeanAccessor getContainerAccessor() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	public void setUnprocessedFields(List<java.lang.reflect.Field> unprocessedField, ClassAnnotationProvider annos) {
		this.template.unprocessedField = unprocessedField;
		this.template.annos = annos;
	}

	@Override
	public TupleMetadata getExtendsTable() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public Collection<ColumnMapping> getExtendedColumns() {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}

	@Override
	public ColumnMapping getExtendedColumnDef(String field) {
		throw new UnsupportedOperationException("this is a abstract metadata template.");
	}
	
	@Override
	public Map<String,String> getColumnComments() {
		return wrapped.getColumnComments();
	}
}
