package jef.database.query;


/**
 * 专门用于描述存储过程输出参数的类
 * @author Administrator
 *
 */
@SuppressWarnings("rawtypes")
public class OutParam implements java.lang.reflect.Type{
	private Class type;
	private boolean isList=false;
	private OutParam(){
	}
	
	/**
	 * 指定出参数一个clz类型的返回参数
	 * @param clz
	 *      要求返回的数据类型
	 * @return
	 */
	public static OutParam typeOf(Class clz){
		OutParam result=new OutParam();
		result.type=clz;
		return result;
	}

	/**
	 * 指定出参为一个clz类型的结果集(ResultSet)，对应的存储过程返回游标
	 * @param clz 
	 *    要拼装成的实体类型，可以是绑定数据库的实体，也可以是普通JavaBean，也可以说Map.class
	 * @return
	 */
	public static OutParam listOf(Class clz){
		OutParam result=new OutParam();
		result.type=clz;
		result.isList=true;
		return result;
	}

	/**
	 * 当前出参是否为List
	 * @return
	 */
	public boolean isList() {
		return isList;
	}
	/**
	 * 出参的具体数据类型
	 * @return
	 */
	public Class getType() {
		return type;
	}
}
