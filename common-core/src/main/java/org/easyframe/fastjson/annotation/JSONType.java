package org.easyframe.fastjson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.easyframe.fastjson.parser.Feature;
import org.easyframe.fastjson.serializer.SerializerFeature;

/**
 * @author wenshao<szujobs@hotmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface JSONType {

    boolean asm() default true;
    /**
     * 可以指定序列化时字段的顺序
     * @return
     */
    String[] orders() default {};
    /**
     * 指定序列化时要忽略的字段名称
     * @return
     */
    String[] ignores() default {};
    /**
     * 指定序列化的操作选项
     * @return
     * @see SerializerFeature
     */
    SerializerFeature[] serialzeFeatures() default {};
    
    Feature[] parseFeatures() default {};

    boolean alphabetic() default true;
    /**
     * 反序列化时，如使用全序列化，则使用另一个类型来实例化，用于注解在抽象类或接口上
     * @return
     */
    Class<?> mappingTo() default Void.class;
    /**
     * 自定义序列化器实例，该序列化器中可以调用static public getInstance对象来得到序列化器实例
     * @return
     * @since easyframe
     */
    Class<?> serializer() default Void.class;
    /**
     * 自定义反序列化器实例，该序列化器中可以调用static public getInstance对象来得到反序列化器实例
     * @return
     * @since easyframe
     */
    Class<?> deserializer() default Void.class;
    /**
     * 默认关闭，采用Property进行访问，开启后使用field进行访问
     * @return
     */
    boolean fieldAccess() default false;
}
