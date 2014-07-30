package jef.tools;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import org.easyframe.fastjson.JSONObject;
import org.easyframe.fastjson.parser.ParserConfig;
import org.easyframe.json.JsonTypeDeserializer;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new IllegalArgumentException();
	}

	public Set<Type> getAutowiredFor() {
		return Collections.<Type>singleton(Foo.class);
	}
}
