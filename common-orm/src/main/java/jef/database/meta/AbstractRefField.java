package jef.database.meta;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import jef.database.annotation.Cascade;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.reflect.Property;

/**
 * 描述一个级联引用关系
 * 
 * @author jiyi
 * 
 */
public abstract class AbstractRefField implements ISelectProvider {
	/**
	 * 引用外部表的Field名称
	 */
	protected Reference reference;

	/**
	 * 是否延迟加载
	 */
	protected FetchType fetch; // 延迟加载特性
	/**
	 * 数据类型
	 */
	private Class<?> sourceFieldType;
	/**
	 * 访问属性
	 */
	private Property field;

	// TODO 对Map类映射的支持。需要重新测试一下，或许可以让现有的属性扩展变得更简单
	private Cascade asMap;

	/**
	 * 优先级。默认大家都是0
	 */
	private int priority;

	public AbstractRefField(Property fieldAccessor, Reference ref, CascadeConfig config) {
		Assert.notNull(fieldAccessor);
		this.field = fieldAccessor;
		this.reference = ref;
		this.sourceFieldType = fieldAccessor.getType();
		this.asMap = config.getAsMap();
		setCascade(config.getCascade());
		this.fetch = config.getFetch();
	}

	private boolean canInsert;
	private boolean canDelete;
	private boolean canUpdate;
	private boolean canSelect;

	// 设置时就将用途计算好，避免使用时去循环判断
	private void setCascade(CascadeType[] cascades) {
		// 形成默认值
		if (cascades.length == 0 || ArrayUtils.fastContains(cascades, CascadeType.ALL)) {
			canInsert = true;
			canDelete = true;
			canUpdate = true;
			canSelect = true;
		} else {
			canInsert = false;
			canDelete = false;
			canUpdate = false;
			canSelect = false;
		}
		//分别计算
		for (CascadeType cascade : cascades) {
			switch (cascade) {
			case DETACH:
				canSelect = true;
				canUpdate = true;
				break;
			case PERSIST:
				canInsert = true;
				break;
			case MERGE:
				canInsert = true;
				canUpdate = true;
				break;
			case REFRESH:
				canSelect = true;
			case REMOVE:
				canDelete = true;
			case ALL:
			default:
			}
		}
		// 如果是单列引用，那么仅用于查询
		if (this.isSingleColumn()) {
			canInsert = false;
			canDelete = false;
			canUpdate = false;
		}
	}

	public String getName() {
		return field.getName();
	}

	public Reference getReference() {
		return reference;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}

	public String toString() {
		return field.getName();
	}

	public FetchType getFetch() {
		return fetch;
	}

	public int getProjection() {
		return 0;
	}

	public abstract ISelectProvider toNestedDesc(String lastName);

	public abstract boolean isSingleColumn();

	public boolean isToOne() {
		return reference.getType().isToOne();
	}

	public boolean canSelect() {
		return canSelect;
	}

	public boolean canDelete() {
		return canDelete;
	}
	
	public boolean canUpdate() {
		return canUpdate;
	}
	
	public boolean canInsert() {
		return canInsert;
	}
	public Cascade getAsMap() {
		return asMap;
	}

	public void setAsMap(Cascade asMap) {
		this.asMap = asMap;
	}

	public Property getField() {
		return field;
	}

	public Class<?> getSourceFieldType() {
		return sourceFieldType;
	}

	public void setSourceFieldType(Class<?> sourceFieldType) {
		this.sourceFieldType = sourceFieldType;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
