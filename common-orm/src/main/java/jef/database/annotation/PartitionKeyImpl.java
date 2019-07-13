package jef.database.annotation;

import java.lang.annotation.Annotation;

import jef.database.routing.function.KeyFunction;
import jef.json.JSONCustom;
import jef.json.JsonTypeSerializer;

import  org.apache.commons.lang3.ArrayUtils;
import  org.apache.commons.lang3.builder.ToStringBuilder;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.ObjectSerializer;

/**
 * 默认的PartitionKey实现，用于非Annotation场合下使用。
 * 
 * @author Administrator
 * 
 */
@JSONCustom(serializer = PartitionKeyImpl.class)//, fieldAccess = true
@SuppressWarnings("all")
public class PartitionKeyImpl implements PartitionKey {
	int sourceDesc;
	/**
	 * 字段名
	 */
	public String field;
	/**
	 * 后缀生成 长度
	 */
	public int length = 0;
	/**
	 * 后缀生成 函数（预制）
	 */
	public KeyFunction function = KeyFunction.RAW;
	/**
	 * 后缀生成 函数(自定义)
	 */
	public Class<? extends PartitionFunction> funcClass = PartitionFunction.class;
	/**
	 * 后缀生成 函数（构造参数）
	 */
	public String[] funcParams = ArrayUtils.EMPTY_STRING_ARRAY;
	/**
	 * 后缀作为数据源名称?
	 */
	public boolean isDbName = false;
	/**
	 * 如果字段为空时的默认值
	 */
	public String defaultWhenFieldIsNull = "";
	/**
	 * 生成后缀如果长度不够的填充字符
	 */
	public char filler = '0';

	/**
	 * 不能删除此方法，因为Gson在反序列化此对象时需要用到（无论此构造是否为public）。
	 * 如果删除，则因为缺少空构造会使用虚拟机字节码等技术来创建对象，此时将不调用&lt;INIT&gt;方法造成无法初始化所有变量。
	 */
	private PartitionKeyImpl() {
	}

	public Class<? extends Annotation> annotationType() {
		return PartitionKey.class;
	}

	public PartitionKeyImpl(String name, int sourceDesc) {
		this.field = name;
		this.sourceDesc = sourceDesc;
	}

	public String field() {
		return field;
	}

	public int sourceDesc() {
		return sourceDesc;
	}

	public KeyFunction function() {
		return function;
	}

	public Class<? extends PartitionFunction> functionClass() {
		return funcClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jef.database.annotation.PartitionKey#functionClassConstructorParams()
	 */
	public String[] functionConstructorParams() {
		return funcParams;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.annotation.PartitionKey#isDbName()
	 */
	public boolean isDbName() {
		return isDbName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.annotation.PartitionKey#defaultWhenFieldIsNull()
	 */
	public String defaultWhenFieldIsNull() {
		return defaultWhenFieldIsNull;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.annotation.PartitionKey#length()
	 */
	public int length() {
		return length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.annotation.PartitionKey#filler()
	 */
	public char filler() {
		return filler;
	}

	public static PartitionKeyImpl create(PartitionKey k) {
		if (k instanceof PartitionKeyImpl) {
			return (PartitionKeyImpl) k;
		}
		PartitionKeyImpl key = new PartitionKeyImpl(k.field(), k.sourceDesc());
		key.defaultWhenFieldIsNull = k.defaultWhenFieldIsNull();
		key.filler = k.filler();
		key.funcClass = k.functionClass();
		key.function = k.function();
		key.funcParams = k.functionConstructorParams();
		key.isDbName = k.isDbName();
		key.length = k.length();
		return key;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public static ObjectSerializer getSerializer() {
		return new Ser();
	}

	private static class Ser extends JsonTypeSerializer<PartitionKeyImpl> implements ObjectSerializer {
		@Override
		protected Object processToJson(PartitionKeyImpl src) {
			JSONObject jo = new JSONObject();
			jo.put("field", src.field);
			jo.put("length", src.length);
			if (src.sourceDesc != 0) {
				jo.put("sourceDesc", src.sourceDesc);
			}
			if (src.funcClass != PartitionFunction.class) {
				jo.put("funcClass", src.funcClass);
			}
			if (src.function != KeyFunction.RAW) {
				jo.put("function", src.function);
			}
			if (src.defaultWhenFieldIsNull.length() > 0) {
				jo.put("defaultWhenFieldIsNull", src.defaultWhenFieldIsNull);
			}
			if (src.funcParams.length > 0) {
				jo.put("funcParams", src.funcParams);
			}
			if (src.filler != '0') {
				jo.put("filler", src.filler);
			}
			if (src.isDbName) {
				jo.put("isDbName", true);
			}
			return jo;
		}
	}
}
