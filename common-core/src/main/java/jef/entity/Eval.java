package jef.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对实体字段的值需要重新计算时可使用该标注
 * @author MJJ
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Eval {
	/**
	 * 计算类型，缺省为MASK表示对值进行模糊化
	 * @return
	 */
	EvalType type() default EvalType.MASK;
	
	/**
	 * 值计算策略类，缺省使用通用的信息模糊化的策略类
	 * 
	 */
	Class<? extends EvalStrategy> strategyClass() default CommonMaskStrategy.class;
	
	/**
	 * 值计算策略类完整类名
	 */
	String strategyClassName() default "";
	
	/**
	 * 策略类的构造参数
	 * 
	 */
	String[] constructParams() default {};
	/**
	 * 前置条件，当该条件满足时才执行计算,未设置条件或者条件脚本不能被正确评估则都表示条件为真
	 * 计算条件使用lua脚本描述，脚本里可以使用形如context["key"]来获取上下文中的值
	 */
	String precondition() default "";
}

