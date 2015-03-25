package jef.tools;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jef.json.JsonTypeDeserializer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;

public class FooDeserializer extends JsonTypeDeserializer<Foo>{
	@Override
	public Foo processObject(ParserConfig config,JSONObject obj) {
		String className=obj.getString("Date");
		try {
			Foo foo=(Foo)Class.forName(className).newInstance();
			foo.setName(obj.getString("NAME_1"));
			foo.setDesc("AAA");
			return foo;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		throw new IllegalArgumentException();
	}

	public Set<Type> getAutowiredFor() {
		return Collections.<Type>singleton(Foo.class);
	}
}
