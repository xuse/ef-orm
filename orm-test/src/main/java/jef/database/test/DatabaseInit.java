package jef.database.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * GeeQuery测试内部使用，使用该注解在单元测试开始的时候执行一些数据库初始化动作。
 * @author Joey
 */
@Target({ElementType.METHOD})
@Retention(RUNTIME)
public @interface DatabaseInit {
}
