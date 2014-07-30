package jef.database.query;

import jef.database.annotation.JoinType;
import jef.database.meta.JoinPath;
import jef.database.meta.Reference;

public class JoinUtil {
	
	/**
	 * 创建一个连接，如果不能创建返回null
	 * 
	 * @param parent
	 * @param e
	 * @param pathHint
	 *            提示的连接路径。用于当公共路劲不足以建立关系的场合
	 * @return null不能建立练级
	 */
	public static AbstractJoinImpl create(JoinElement parent, Query<?> e, JoinPath pathHint,JoinType forceType,boolean reverse) {
		return JoinImpl2.create(parent, e, pathHint, forceType,reverse);
	}
	
	public static AbstractJoinImpl create(JoinElement parent, Reference e,Query<?> tQuery){
		return JoinImpl2.create(parent, e, tQuery);
	}
}
