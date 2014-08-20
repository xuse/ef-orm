package jef.json;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jef.tools.DateFormats;
import jef.tools.Foo;
import jef.tools.BeanForTest;
import jef.tools.ResourceUtils;
import jef.tools.XMLUtils;
import jef.tools.reflect.GenericUtils;
import jef.tools.string.RandomData;

import org.easyframe.fastjson.JSON;
import org.easyframe.fastjson.JSONObject;
import org.easyframe.fastjson.serializer.JSONSerializer;
import org.easyframe.fastjson.serializer.ObjectSerializer;
import org.easyframe.fastjson.serializer.SerializeConfig;
import org.easyframe.fastjson.serializer.SerializerFeature;
import org.easyframe.json.ConfigManager;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;

public class JsonTest extends org.junit.Assert{
	@Test
	public void test1(){
		String s=ResourceUtils.asString("abc.json");
		Object obj=JsonUtil.toMap(s);
		System.out.println(obj);
	}

	@Test
	public void testFoo(){
		Foo foo=RandomData.newInstance(Foo.class);
		
		String s1=JSON.toJSONString(foo);
		System.out.println(s1);;
		
		Foo foo1=JSON.parseObject(s1,Foo.class);
		System.out.println(foo1);
		
		
		String s2=JSON.toJSONString(foo1);
		System.out.println(s2);
		
		assertEquals(s1,s2);
	}
	
	@Test
	public void testFooAlias(){
		Foo foo=RandomData.newInstance(Foo.class);
		
		String s1=JSON.toJSONString(foo);
		System.out.println(s1);;
		
		Foo foo1=JSON.parseObject(s1,Foo.class);
		System.out.println(foo1);
		
		String s2=JSON.toJSONString(foo1);
		System.out.println(s2);
		
		assertEquals(s1,s2);
	}
	
	
	@Test
	public void testLevel(){
		Map<String,Object> map=new HashMap<String,Object>();
		map.put("aaa", "aa");
		map.put("bbb", 123);
		BeanForTest foo=new BeanForTest();
		foo.setAge(12);
		foo.setName("彰善瘅恶");
		map.put("ccc", foo);
		map.put("onError", new JSFunction());
		String s=JsonUtil.toJsonScriptCode(map);
		System.out.println(s);
	}
	
	static class Bean{
		private int id;
		private String name;
		private JSFunction callback;
		private JSFunction func;
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public JSFunction getCallback() {
			return callback;
		}
		public void setCallback(JSFunction callback) {
			this.callback = callback;
		}
		public JSFunction getFunc() {
			return func;
		}
		public void setFunc(JSFunction func) {
			this.func = func;
		}
	}
	
	@Test
	public void test(){
		String json = "[{id:1,name:'1'},{id:2,name:'2'}]";
		Bean[] list=JsonUtil.toArray(json,Bean.class);
		Assert.assertEquals(list.length, 2);
		Bean map1=list[0];
		Assert.assertEquals(map1.id, 1);
	}
	
	
	@Test
	public void testNull(){
		String s=JsonUtil.toJson(null);
		assertEquals("{}", s);

		
		s=JSON.toJSONString(null);
		assertEquals("null", s);
	}
	
	@Test
	public void testBean(){
		Bean bean = new Bean();
		bean.func = new JSFunction();
		bean.func.setArgs("a","e","c","f");
		System.out.println(JsonUtil.toJsonScriptCode(bean));
	}
	
//
	//测试案例1： 日期格式默认按照毫秒数序列化java.util.date java.sql.date, timestamp,  caealdar等多种类型
	//测试案例3：支持Annotaion禁用字段输出:Dataobject输出
	@Test
	public void testDate(){
		BeanForTest person=new BeanForTest();
		person.setBirthDay(new Date(1394776313506L));
		
		
		String s=JsonUtil.toJson(person,null,SerializerFeature.UseSingleQuotes);
		assertEquals("{'age':0,'birthDay':1394776313506,'flag':false,'height':0,'id':0,'weight':0}",s);
		
		
		s=JsonUtil.toJson(person,DateFormats.DATE_CS.get(),SerializerFeature.UseSingleQuotes);
		assertEquals("{'age':0,'birthDay':'2014-03-14','flag':false,'height':0,'id':0,'weight':0}",s);
		
		
		{
			s=JsonUtil.toJson(person.getBirthDay(),DateFormats.DATE_CS.get(),SerializerFeature.UseSingleQuotes);
			String s1=JsonUtil.toJson(new Timestamp(person.getBirthDay().getTime()),DateFormats.DATE_CS.get(),SerializerFeature.UseSingleQuotes);
			String s2=JsonUtil.toJson(new java.sql.Date(person.getBirthDay().getTime()),DateFormats.DATE_CS.get(),SerializerFeature.UseSingleQuotes);
			String s3=JsonUtil.toJson(Calendar.getInstance(),new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),SerializerFeature.UseSingleQuotes);
			System.out.println(s);
			System.out.println(s1);
			System.out.println(s2);
			System.out.println(s3);			
		}
	}
	
	
	
	//测试案例2：Map高级格式化
	@Test
	public void testMap2(){
		String json="{a1:'v1',a2:{map1:'v2',map2:[1,2,3]},a3:333}";
		Map<String,Object> map=JsonUtil.toMap(json);
		assertTrue(map.get("a2") instanceof Map);
	}

	//测试案例4：支持TypeAdapter
	//测试案例5：
	
	public static <T> T[] fromJsonArray(String json, Class<T> rawType) {
		return JsonUtil.toObject(json,GenericUtils.newArrayType(rawType));
	}
	
	@Test
	public void xmlJson() throws SAXException, IOException{
//		Person p=RandomData.newInstance(Person.class);
		Foo p=RandomData.newInstance(Foo.class);
		String s1=JsonUtil.toJson(p);
		System.out.println(s1);
		JSONObject o=(JSONObject) JSON.toJSON(p);
		Document doc=JsonUtil.jsonToXML(o);
		String s2=XMLUtils.toString(doc);
		System.out.println(s2);
		
		JSONObject s3=JsonUtil.xmlToJson(doc);
		System.out.println(s3);
	}
	
	@Test
	public void xmlJson2() throws SAXException, IOException{
//		Person p=RandomData.newInstance(Person.class);
		Foo p=RandomData.newInstance(Foo.class);
		String s3=JsonUtil.toJsonWithoutQuot(p);
		System.out.println(s3);
		
		Foo f=JsonUtil.toObject(s3,Foo.class);
		
		System.out.println(f.getName());
		System.out.println(f.getDesc());
	}
	
	@Test
	public void testJ1() throws Exception {
		BeanForTest p = RandomData.newInstance(BeanForTest.class);
		String z = toJson(p);
		System.out.println(z);
		System.out.println(JSON.toJSONString(p));
	}

	private String toJson(Object obj) throws Exception {
		System.out.println(ConfigManager.get("custom1").hasType(BeanForTest.class));
		JSONSerializer serializer = new JSONSerializer(ConfigManager.get("custom1"));
		try {
//			serializer.config(SerializerFeature.PrettyFormat, true);
			SerializeConfig config=serializer.getMapping();
			ObjectSerializer se=config.createASMSerializer(BeanForTest.class,ImmutableMap.of("id","the_id","name","name_person"));
			config.put(BeanForTest.class,se);
			serializer.write(obj);
			System.out.println(ConfigManager.get("custom1").hasType(BeanForTest.class));
			return serializer.toString();
		} finally {
			serializer.close();
		}
	}
	@Test
	public void test222() {
		BeanForTest p = RandomData.newInstance(BeanForTest.class);
		JSON.toJSONString(p);
		JSON.toJSONString(p);
		JSON.toJSONString(p);
		JSON.toJSONString(p);
	}
	
	
	@Test
	public void testJsonField() {
		Map<String,String> map=new HashMap<String,String>();
		
		map.put("aa.bb", "cccc");
		map.put("aa.bb1", "cccfs");
		map.put("aa.bb2", "fdsfsdfdsc");
		
		String s=JSON.toJSONString(map);
		System.out.println(s);
	}
	
	
	private Map alias;

	public Map getAlias() {
		return alias;
	}

	public void setAlias(Map alias) {
		this.alias = alias;
	}}
