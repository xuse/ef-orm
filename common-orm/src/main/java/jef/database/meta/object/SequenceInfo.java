package jef.database.meta.object;

import java.math.BigDecimal;

/**
 * 数据库中的序列信息
 * @author jiyi
 *
 */
public class SequenceInfo {
	private String catalog;
	private String schema;
	private String name;
	private long startValue;
	private int step;
	private int cacheSize;
//	private BigDecimal maxValue;
	private long minValue;
	/**
	 * 下一次调用nextval将要得到的值。注意，不是上一次调用得到的值
	 */
	private long currentValue;

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getStartValue() {
		return startValue;
	}

	public void setStartValue(long startValue) {
		this.startValue = startValue;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

//	public BigDecimal getMaxValue() {
//		return maxValue;
//	}

//	public void setMaxValue(BigDecimal maxValue) {
//		this.maxValue = maxValue;
//	}

	public long getMinValue() {
		return minValue;
	}

	public void setMinValue(long minValue) {
		this.minValue = minValue;
	}

	public long getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(long currentValue) {
		this.currentValue = currentValue;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}
	
	
}
