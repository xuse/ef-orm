package jef.database.annotation;

import java.lang.annotation.Annotation;

import jef.tools.StringUtils;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.easyframe.fastjson.JSONObject;
import org.easyframe.fastjson.annotation.JSONType;
import org.easyframe.fastjson.serializer.ObjectSerializer;
import org.easyframe.json.JsonTypeSerializer;

@JSONType(serializer=PartitionTableImpl.class,fieldAccess=true)
@SuppressWarnings("all")
public class PartitionTableImpl implements PartitionTable {
	private String appender="_";
	private String keySeparator="";
	private PartitionKeyImpl[] key;
	private String dbPrefix="";
	
	public Class<? extends Annotation> annotationType() {
		return PartitionTable.class;
	}

	public String appender() {
		return appender;
	}

	public String keySeparator() {
		return keySeparator;
	}

	public PartitionKey[] key() {
		return key;
	}
	
	public static PartitionTableImpl create(PartitionTable t){
		if(t instanceof PartitionTableImpl){
			return (PartitionTableImpl)t;
		}
		PartitionTableImpl impl=new PartitionTableImpl();
		impl.appender=t.appender();
		impl.keySeparator=t.keySeparator();
		PartitionKey[] from=t.key();
		PartitionKeyImpl[] keys=new PartitionKeyImpl[from.length];
		for(int i=0;i<from.length;i++){
			keys[i]=PartitionKeyImpl.create(from[i]);
		}
		impl.key=keys;
		return impl;
	}
	
	public void setAppender(String appender) {
		this.appender = appender;
	}

	public void setKeySeparator(String keySeparator) {
		this.keySeparator = keySeparator;
	}

	public void setKey(PartitionKeyImpl[] key) {
		this.key = key;
	}

	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public static ObjectSerializer getSerializer(){
		return new Ser();
	}
	
	private static class Ser extends JsonTypeSerializer<PartitionTableImpl> {
		@Override
		protected Object processToJson(PartitionTableImpl src) {
			JSONObject jo=new JSONObject();
			jo.put("key", src.key);
			if(!"_".equals(src.appender)){
				jo.put("appender", src.appender);
			}
			if(StringUtils.isNotEmpty(src.keySeparator)){
				jo.put("keySeparator", src.keySeparator);
			}
			return jo;
		}
	}

	public String dbPrefix() {
		return this.dbPrefix;
	}
}
