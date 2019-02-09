package com.github.geequery.springdata.annotation;

import jef.database.Condition.Operator;

/**
 * 描述单个查询条件
 * @author jiyi
 *
 */
public @interface Condition {
    /**
     * 查询的字段
     * @return
     */
    String value();

    /**
     * 运算符
     * @return
     */
    Operator op() default Operator.EQUALS;
}
