package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.query.BindVariableField;
import jef.database.wrapper.InsertSqlResult;
import jef.tools.Assert;
import jef.tools.reflect.Property;

public abstract class AbstractTimeMapping<T> extends ATypeMapping<T> {
	private int generated;
	private Property accessor;

	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		if(type instanceof ColumnType.TimeStamp){
			this.generated = ((ColumnType.TimeStamp) type).getGenerateType();
		}else if(type instanceof ColumnType.Date){
			this.generated = ((ColumnType.Date) type).getGenerateType();
		}
		BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(meta.getContainerType());
		if (meta.getType() != EntityType.TUPLE) {
			Assert.isTrue(meta.getAllFieldNames().contains(field.name()));
		}
		accessor = ba.getProperty(field.name());
	}

	@Override
	public void processInsert(Object value, InsertSqlResult result, List<String> cStr, List<String> vStr, boolean smart, IQueryableEntity obj) throws SQLException {
		if (!obj.isUsed(field) && generated>0) {
			if (isJavaSysdate()) {
				value=getCurrentValue();
				accessor.set(obj, value);
			} else{
				cStr.add(getColumnName(result.profile, true));
				vStr.add(getFunctionString(result.profile));
				return;
			}
		}
		super.processInsert(value, result, cStr, vStr, smart, obj);
	}

	@Override
	public void processPreparedInsert(IQueryableEntity obj, List<String> cStr, List<String> vStr, InsertSqlResult result, boolean smart) throws SQLException {
		if (!obj.isUsed(field)  && generated>0) {
			if (isJavaSysdate()) {
				accessor.set(obj, getCurrentValue());
			} else{
				cStr.add(getColumnName(result.profile, true));
				vStr.add(getFunctionString(result.profile));
				return;
			}
		}
		super.processPreparedInsert(obj, cStr, vStr, result, smart);
	}

	public abstract String getFunctionString(DatabaseDialect profile);

	public abstract Object getCurrentValue();
	
	public void processAutoUpdate(DatabaseDialect profile,List<String> sqls, List<jef.database.Field> params){
		String columnName = getColumnName(profile, true);
		if(isJavaSysdate()){
			sqls.add(columnName+" = ?");
			params.add(new BindVariableField(getCurrentSqlValue()));
		}else{
			sqls.add(columnName+" = "+getFunctionString(profile));
		};
	}

	private Object getCurrentSqlValue(){
		if(this.getSqlType()==Types.TIMESTAMP){
			return new java.sql.Timestamp(System.currentTimeMillis());
		}else{
			return new java.sql.Date(System.currentTimeMillis());
		}
	}

	public final boolean isForUpdate() {
		return generated==2 || generated==4;
	}

	public final boolean isJavaSysdate() {
		return generated>=3;
	}
}
