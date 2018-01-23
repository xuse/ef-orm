package jef.accelerator.bean;

/**
 * 用于生产bean存取器的 抽象工厂
 * @author Administrator
 *
 */
@FunctionalInterface
public interface BeanAccessorFactory {
	 BeanAccessor getBeanAccessor(Class<?> javaBean);
}
