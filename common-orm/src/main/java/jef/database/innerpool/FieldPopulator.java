package jef.database.innerpool;

import java.sql.SQLException;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMappings;
import jef.database.dialect.type.MappingType;
import jef.database.meta.IReferenceColumn;
import jef.database.wrapper.ColumnDescription;
import jef.database.wrapper.ColumnMeta;
import jef.database.wrapper.IPopulator;
import jef.database.wrapper.result.IResultSet;
import jef.tools.Assert;
import jef.tools.reflect.BeanWrapper;

public class FieldPopulator implements IPopulator{
	
	private String name;
	private String columnName;
	private ColumnDescription desc;
	
	public FieldPopulator(IReferenceColumn field,DatabaseDialect profile,String schema,ColumnMeta columns,BeanAccessor ba){
		this.columnName=field.getSelectedAlias(schema, profile, false);
		Assert.notNull(columnName);
		this.name = field.getName();
		ColumnDescription desc = columns.getByFullName(columnName);
		if (desc == null) {
			throw new IllegalArgumentException("Column not in ResultSet:" + columnName + " all:" + columns);
		}
		
		//计算元模型类型
		MappingType<?> t = field.getTargetColumnType();
		
		//判断计算容器类型
		String name=this.name;
		int n = name.indexOf('.');
		BeanAccessor bw = ba;
		while (n > -1) {
			String thisName = name.substring(0, n);
			name = name.substring(n + 1);
			n = name.indexOf('.');
			Class<?> type = bw.getPropertyType(thisName); //FIXME这里没有NestedObjectPopulator健壮
			bw = FastBeanWrapperImpl.getAccessorFor(type);
		}
		Class<?> javaContainer=bw.getPropertyType(name);
		desc.setAccessor(ColumnMappings.getAccessor(javaContainer, t, desc, true));
		this.desc=desc;
	}
	
	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
		String name=this.name;
		int n = name.indexOf('.');
		BeanWrapper bw = wrapper;
		while (n > -1) {
			String thisName = name.substring(0, n);
			name = name.substring(n + 1);
			n = name.indexOf('.');
			Object bean = bw.getPropertyValue(thisName); //FIXME这里没有NestedObjectPopulator健壮
			bw = BeanWrapper.wrap(bean);
		}
		try {
			bw.setPropertyValue(name, desc.getAccessor().getProperObject(rs, desc.getN()));
		} catch (SQLException s) {
			LogUtil.exception("[" + columnName + "] is not found from the resultset.", s);
			throw s;
		}
		
	}

}
