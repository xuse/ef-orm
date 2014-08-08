package jef.database.meta;


/**
 * 用于描述查询中一个选择/装配的定义
 * 
 * @Company: Asiainfo-Linkage Technologies(China),Inc. Hangzhou
 * @author Administrator
 * @Date 2011-6-17
 */
public interface ISelectProvider {
	public static final int PROJECTION_NORMAL_NOVIRTUAL = -2;//禁止使用rownum和rowid等虚拟列
	public static final int PROJECTION_NOT_SELECT = -1;//不选择任何数据从某张表，仅在alltableColumns中使用
	public static final int PROJECTION_NORMAL = 0;
	public static final int PROJECTION_COUNT = 1;
	public static final int PROJECTION_COUNT_DISTINCT = 2;
	public static final int PROJECTION_SUM = 3;
	public static final int PROJECTION_AVG = 4;
	public static final int PROJECTION_MAX = 10;
	public static final int PROJECTION_MIN = 11;
	public static final int PROJECTION_CUST_FUNC = 12;
	//255以内的都是排他定义，255以上的附加位定义
	public static final int PROJECTION_HAVING_NOT_SELECT = 1024;
	public static final int PROJECTION_HAVING = 2048;
	public static final int PROJECTION_GROUP = 4096;
	public static final int PROJECTION_DISTINCT = 8192;
	
	
	/**
	 * 引用字段路径，用于装配，如果返回值为null，表示直接装配到基本对象上。
	 * 
	 * @return
	 */
	String getName();

	/**
	 * 返回装配的
	 * @return
	 */
	int getProjection();
	
	boolean isSingleColumn();
}
