package com.github.geequery.entity;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.meta.AnnotationProvider;
import jef.database.meta.EntityType;
import jef.tools.Exceptions;
import jef.tools.reflect.BooleanProperty;
import jef.tools.reflect.FieldAccessor;
import jef.tools.reflect.Property;

/**
 * 代替原来的POJO处理机制
 * 
 * @author jiyi
 *
 */
public class DefaultEntityMetadata extends AbstractEntityMetadata {

	/**
	 * 记录当前Schema所对应的实体类。
	 */
	private Class<?> thisType;

	/**
	 * Bean操作类
	 */
	private final BeanAccessor containerAccessor;

	private FieldAccessor touchRecordAccessor;

	private BooleanProperty touchNotFlagAccessor;

	private FieldAccessor LazyAccessor;

	/**
	 * 标准实体的构造
	 * 
	 * @param clz
	 * @param annos
	 */
	public DefaultEntityMetadata(Class<?> clz, AnnotationProvider annos) {
		super.checkEnhanced(clz,annos);
		initAccessor(clz);
		this.thisType = clz;
		this.containerAccessor = FastBeanWrapperImpl.getAccessorFor(clz);
		initByAnno(clz, annos);
	}

	private void initAccessor(Class<?> clz) {
		String message = "{} has not enhanced or is not a valid entity.";
		try {
			java.lang.reflect.Field field = clz.getDeclaredField("___touchRecord");
			this.touchRecordAccessor = FieldAccessor.generateAccessor(field);
		} catch (NoSuchFieldException e) {
			throw Exceptions.unsupportedOperation(message, clz.getName());
		}
		try {
			java.lang.reflect.Field field = clz.getDeclaredField("___notTouch");
			this.touchNotFlagAccessor = (BooleanProperty) FieldAccessor.generateAccessor(field);
		} catch (NoSuchFieldException e) {
			throw Exceptions.unsupportedOperation(message, clz.getName());
		}
		try {
			java.lang.reflect.Field field = clz.getDeclaredField("___lazy");
			this.LazyAccessor = FieldAccessor.generateAccessor(field);
		} catch (NoSuchFieldException e) {
			throw Exceptions.unsupportedOperation(message, clz.getName());
		}
	}

	@Override
	public Property getTouchRecord() {
		return touchRecordAccessor;
	}

	@Override
	public BooleanProperty getTouchIgnoreFlag() {
		return touchNotFlagAccessor;
	}

	public EntityType getType() {
		return EntityType.NATIVE;
	}

	public Object newInstance() {
		return containerAccessor.newInstance();
	}

	public Class<?> getThisType() {
		return thisType;
	}

	@Override
	public BeanAccessor getContainerAccessor() {
		return containerAccessor;
	}

	public boolean enhanced() {
		return touchRecordAccessor != null && LazyAccessor != null && touchNotFlagAccessor != null;
	}

	@Override
	public FieldAccessor getLazyAccessor() {
		return LazyAccessor;
	}
}
