/**
 * 
 */
package jef.database.meta.object;

/**
 * 外键的匹配类型
 * @author qihongfei
 *
 */
public enum ForeignKeyMatchType {

	FULL, // 不允许多列外键约束中的任何一个为null,除非他们全部为null，这样不要求被引用表中有与其匹配的数据
	PARTIAL, // 此特性尚未实现
	SIMPLE; // 这是默认值。允许外键约束中的任何一列为null,只要外键约束中的一列为null,则不要求与被引用表相匹配
	
	public static ForeignKeyMatchType parseName(String name){
		
		for (ForeignKeyMatchType a : ForeignKeyMatchType.values()) {  
            if (a.name().equals(name)) {  
                return a;  
            }  
        }  
		return null;
	}
}
