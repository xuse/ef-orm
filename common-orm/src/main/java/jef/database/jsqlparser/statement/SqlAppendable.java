package jef.database.jsqlparser.statement;

import jef.tools.reflect.DeepCloneable;

public interface SqlAppendable extends DeepCloneable {
	   
    /**
     * 新增的方法，为了代替原来的toString()方法的低效率
     * @param sb
     */
    public void appendTo(StringBuilder sb);
}
