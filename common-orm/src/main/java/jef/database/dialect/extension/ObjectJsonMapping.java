package jef.database.dialect.extension;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AColumnMapping;
import jef.database.jdbc.result.IResultSet;
import jef.tools.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * 扩展Java-DB的映射规则，将复杂对象转换为JSON与数据库中的文本进行对应
 * @author jiyi
 *
 */
public class ObjectJsonMapping extends AColumnMapping{
	private SerializerFeature feature = null;
	private SerializerFeature[] features=new SerializerFeature[0];
	
	@Override
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if (value == null) {
			st.setNull(index, Types.VARCHAR);
			return null;
		} else {
			String s=JSON.toJSONString(value,features);
			st.setString(index, s);
			return s;
		}
	}

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String o = rs.getString(n);
		if(StringUtils.isEmpty(o))return null;
		return JSON.parseObject(o, this.getFieldAccessor().getGenericType());
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Object.class;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		String s=JSON.toJSONString(value);
		return wrapSqlStr(s);
	}

	public SerializerFeature getFeature() {
		return feature;
	}

	public void setFeature(SerializerFeature feature) {
		this.feature = feature;
		this.features=new SerializerFeature[] {feature};
	}
}