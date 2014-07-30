package jef.tools;

import jef.accelerator.bean.AbstractFastProperty;
import jef.tools.reflect.Property;

import com.google.common.base.Function;

/**
 * 带数据转换功能的Property
 * 通过定制SetFunction和getFunction，在get set时转换属性的值
 * @author jiyi
 *
 */
public final class FunctionProperty extends AbstractFastProperty {
	private Property sProperty;
	private Function<Object,Object> getFunction = KEEP_RAW;
	private Function<Object,Object> setFunction = KEEP_RAW;

	public FunctionProperty(Property inner) {
		this.sProperty = inner;
	}

	public Function<Object, Object> getGetFunction() {
		return getFunction;
	}

	public FunctionProperty setGetFunction(Function<Object, Object> getFunction) {
		this.getFunction = getFunction;
		return this;
	}

	public Function<Object, Object> getSetFunction() {
		return setFunction;
	}

	public FunctionProperty setSetFunction(Function<Object, Object> setFunction) {
		this.setFunction = setFunction;
		return this;
	}

	public String getName() {
		return sProperty.getName();
	}

	public Object get(Object obj) {
		return getFunction.apply(sProperty.get(obj));
	}

	public void set(Object obj, Object value) {
		sProperty.set(obj, setFunction.apply(value));
	}
	//一个不作任何实现的Function
	public static Function<Object,Object> KEEP_RAW=new Function<Object,Object>(){
		public Object apply(Object input) {
			return input;
		}
		
	};
}
