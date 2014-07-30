//package jef.tools;
//
//import java.lang.reflect.Type;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import jef.common.log.LogUtil;
//import jef.database.DbUtils;
//import jef.database.Field;
//import jef.database.jsqlparser.parser.ParseException;
//import jef.database.jsqlparser.statement.select.SelectExpressionItem;
//import jef.database.jsqlparser.statement.select.SelectItem;
//import jef.tools.JsonPropertyProcessor.JsonObjectWrapper;
//import jef.tools.reflect.BeanWrapperImpl;
//
//import com.google.gson.JsonDeserializationContext;
//import com.google.gson.JsonDeserializer;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParseException;
//import com.google.gson.JsonSerializationContext;
//import com.google.gson.JsonSerializer;
//
//public class Json extends JsonUtil {
//	static{
//		JsonUtil.registerTypeHierarchyAdapter(jef.database.Field.class, new FieldAdapter());
//	}
//
//	@SuppressWarnings({"unchecked","rawtypes"})
//	static class FieldAdapter implements JsonSerializer<jef.database.Field>,JsonDeserializer<jef.database.Field>{
//		public Field deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//			JsonObject obj= json.getAsJsonObject();
//			if(obj==null)throw new RuntimeException("Can not Deserialize to jef.database.field "+ json.toString());
//			try {
//				String clzName=obj.get("class").getAsString();
//				Class<? extends Enum> enumType=Class.forName(clzName).asSubclass(Enum.class);
//				Object field=Enum.valueOf(enumType, obj.get("field").getAsString());
//				return (Field) field;
//			} catch (ClassNotFoundException e) {
//				LogUtil.exception(e);
//				throw new RuntimeException(e);
//			}
//		}
//
//		public JsonElement serialize(Field src, Type typeOfSrc, JsonSerializationContext context) {
//			if(src instanceof Enum){
//				JsonObject obj=new JsonObject();
//				obj.add("class", context.serialize(((Enum) src).getDeclaringClass().getName()));
//				obj.add("field", context.serialize(src.name()));
//				return obj;
//			}else{
//				throw new RuntimeException("Can not serialize field "+ src.getClass());
//			}
//		}
//	}
//
//	/**
//	 * 描述一个Json序列化的配置
//	 * 
//	 * @author Administrator
//	 * 
//	 */
//	public static class SerializeRule<T> implements ISerializeRule<T>{
//		Class<T> type;
//		String rawContent;
//		JsonSerializer<T> serializer;
//		Map<String,Object> attributes=null;
//		Map<String,JsonPropertyProcessor<T>> processor=null;
//		
//		private SerializeRule(Class<T> clz, String content) {
//			this.type = clz;
//			this.rawContent = content;
//		}
//		
//		/**
//		 * 在Json序列化时，可以添加原来不存在的一些固定值字段
//		 * @param key
//		 * @param value
//		 */
//		public void addAttribute(String key,Object value){
//			if(attributes==null){
//				attributes=new HashMap<String,Object>();
//			}
//			attributes.put(key, value);
//		}
//		
//		/**
//		 * 在Json序列化时，可以添加原来不存在的一些固定值字段
//		 * @param key
//		 * @param value
//		 */
//		public void addPropertyProcessor(String key,JsonPropertyProcessor<T> value){
//			if(processor==null){
//				processor=new HashMap<String,JsonPropertyProcessor<T>>();
//			}
//			processor.put(key, value);
//		}
//
//		 Map<String, String> getSelectMapping() {
//			 if(StringUtils.isBlank(rawContent))return null;
//			Map<String, String> mapping = new HashMap<String, String>();
//			List<SelectItem> items;
//			try {
//				items = DbUtils.parseSelectItems(rawContent);
//			} catch (ParseException e1) {
//				throw new RuntimeException(e1.getMessage());
//			}
//			for (SelectItem item : items) {
//				if (item instanceof SelectExpressionItem) {
//					SelectExpressionItem expression = (SelectExpressionItem) item;
//					mapping.put(expression.getExpression().toString(), expression.getAlias());
//				} else if (item instanceof jef.database.jsqlparser.statement.select.AllTableColumns) {
//					jef.database.jsqlparser.statement.select.AllTableColumns e = (jef.database.jsqlparser.statement.select.AllTableColumns) item;
//					mapping.put(e.getTable().toString(), null);
//				}
//			}
//			return mapping;
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			if (obj == null)
//				return false;
//			if (obj instanceof SerializeRule) {
//				SerializeRule<?> other = (SerializeRule<?>) obj;
//				return type.equals(other.type) && rawContent.equals(other.rawContent);
//			} else {
//				return false;
//			}
//		}
//
//		@Override
//		public int hashCode() {
//			return type.hashCode() + rawContent.hashCode();
//		}
//
//		public JsonSerializer<T> getSerializer(){
//			if(serializer!=null)return serializer;
//			serializer=new JsonSerializer<T>(){
//				public JsonElement serialize(T src, Type typeOfSrc,JsonSerializationContext context) {
//					JsonObject result=new JsonObject();
//					BeanWrapperImpl bw=new BeanWrapperImpl(src);
//					Map<String,String> map=getSelectMapping();
//					if(map==null){//默认全部输出
//						for(String key: bw.getRwPropertyNames()){
//							JsonPropertyProcessor<T> p=processor==null?null:processor.get(key);
//							if(p!=null){
//								p.process(new JsonObjectWrapper(result,context), src);	
//							}else{
//								Object value=bw.getPropertyValue(key);
//								result.add(key, context.serialize(value));	
//							}
//						}
//					}else{  //定义输出
//						for(String key:map.keySet()){
//							JsonPropertyProcessor<T> p=processor==null?null:processor.get(key);
//							if(p!=null){
//								p.process(new JsonObjectWrapper(result,context), src);	
//							}else{
//								Object value=bw.getNestedProperty(key);
//								String alias=map.get(key);
//								if(alias==null)alias=key;
//								result.add(alias, context.serialize(value));	
//							}
//						}
//					}
//					
//					JsonPropertyProcessor<T> p=processor==null?null:processor.get("*");
//					if(p!=null){
//						p.process(new JsonObjectWrapper(result,context), src);	
//					}
//					
//					if(attributes!=null){
//						for(String key: attributes.keySet()){
//							result.add(key, context.serialize(attributes.get(key)));	
//						}	
//					}
//					return result;
//				}
//			};
//			return serializer;
//		}
//
//		public Type getType() {
//			return type;
//		}
//	}
//
//	/**
//	 * 创建一个Json序列化规则:
//	 * <p>SerializeRule是这样使用的</p>
//	 * <code>
//	 *  	SerializeRule rule=Json.createRule(Person.class,"opId,school.name as schoolName");<br> 
//	 *		rule.addAttribute("entityText", object1);//要额外添加的属性<br> 
//	 *		JsonElement element=Json.serializeTreeAs(result, rule);
//	 * </code>
//	 * <p>
//	 * @param clz  规则针对的类型
//	 * @param content createRule时指定的参数类似于SQL语句中的select语法，可以指定子对象中的属性，也可以指定序列化后的别名。
//	 * @return 序列化规则
//	 */
//	public static <T> SerializeRule<T> createRule(Class<T> clz, String content) {
//		SerializeRule<T> result=new SerializeRule<T>(clz,content);
//		return result;
//	}
//	
//	public static <T> SerializeRule<T> createRule(Class<T> clz) {
//		SerializeRule<T> result=new SerializeRule<T>(clz,null);
//		return result;
//	}
//}
