package jef.database.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import jef.database.annotation.PartitionFunction;
import jef.tools.DateUtils;
import jef.tools.support.LangUtils;

import org.apache.commons.lang.ObjectUtils;

/*
 * 对应SQL条件
 * 1. BETWEEN
 * 2. x > a and x < b
 * 3. x >=a and x<=b
 * ...
 * 注意，由于大于和小于也可能出现无限条件，比如 a > 1
 * 这种情况，要特殊考虑
 * 描述的区间始终一个 > min and <max的区间， 但Dimension本身不保证max一定>min
 * 
 */
@SuppressWarnings("rawtypes")
public class RangeDimension<T extends Comparable<T>> implements Dimension {
	/**
	 * 区间的左边界
	 */
	private T min;
	/**
	 * 区间的右边界
	 */
	private T max;

	// 是否左闭区间
	private boolean isLeftCloseSpan = true;
	// 是否左开区间
	private boolean isRightCloseSpan = true;

	
	/**
	 * 默认构造，左右闭区间
	 * 
	 * @param min2
	 * @param max2
	 */
	public RangeDimension(T min2, T max2) {
		this.min = min2;
		this.max = max2;
	}

	/**
	 * 创建一个左闭右开区间
	 * 
	 * @param min
	 * @param max
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> RangeDimension<T> createLC(Object min, Object max) {
		return new RangeDimension<T>((T)min, (T)max, true, false);
	}

	/**
	 * 创建一个左开右闭区间
	 * @param min
	 * @param max
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> RangeDimension<T> createCL(Object min, Object max) {
		return new RangeDimension<T>((T)min, (T)max, false, true);
	}

	/**
	 * 创建一个开区间
	 * @param min
	 * @param max
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> RangeDimension<T> createCC(Object min, Object max) {
		return new RangeDimension<T>((T)min, (T)max, false, false);
	}
	
	/**
	 * 创建一个闭区间
	 * @param min
	 * @param max
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> RangeDimension<T> create(Object min, Object max) {
		return new RangeDimension<T>((T)min, (T)max);
	}
	
	/**
	 * 构造，可以指定区间开启
	 * 
	 * @param min
	 * @param max
	 * @param leftClose
	 * @param rightClose
	 */
	public RangeDimension(T min, T max, boolean leftClose, boolean rightClose) {
		this.min = min;
		this.max = max;
		this.isLeftCloseSpan = leftClose;
		this.isRightCloseSpan = rightClose;
	}

	public RangeDimension(T value) {
		this(value, value);
	}

	@SuppressWarnings("unchecked")
	public Dimension mergeAnd(Dimension d) {
		if (d instanceof ComplexDimension) {
			return d.mergeAnd(this);
		}
		// if (d instanceof Points) {
		// List<Object> list = ArrayUtils.asList(((Points) d).points);
		// for (Iterator<Object> iter = list.iterator(); iter.hasNext();) {
		// T obj = (T) iter.next();
		// // 同时满足左右边界条件？
		// if (isInsideLeftBorder(obj) && isInsideRightBorder(obj)) {
		// } else {
		// iter.remove();
		// }
		// }
		// return new Points(list.toArray());
		// } else
		if (d instanceof RangeDimension) {
			// 算法,两个范围的合并
			RangeDimension<T> other = (RangeDimension<T>) d;
			Entry<T, Boolean> eLeft = max(this.min, this.isLeftCloseSpan, other.min, other.isLeftCloseSpan, LangUtils.NULL_IS_MINIMUM, false);// 左取大;
			Entry<T, Boolean> eRight = min(this.max, this.isRightCloseSpan, other.max, other.isRightCloseSpan, LangUtils.NULL_IS_MAXIMUM, false);// 右取小;
			return new RangeDimension<T>(eLeft.getKey(), eRight.getKey(), eLeft.getValue(), eRight.getValue());
		} else {
			throw new UnsupportedOperationException("Unknown dimenssion type:" + d.getClass().getName());
		}
	}

	// 取两个点中较大的那个
	private Entry<T, Boolean> max(T v1, Boolean c1, T v2, Boolean c2, int nullSupport, boolean closeFirst) {
		if (ObjectUtils.equals(v1, v2)) {
			if (closeFirst) {// 当值相等时，取闭区间
				return new jef.common.Entry<T, Boolean>(v1, c1 || c2);
			} else {
				return new jef.common.Entry<T, Boolean>(v1, c1 && c2);
			}
		} else {
			Comparable max = LangUtils.max(v1, v2, nullSupport);
			if (max == v1) {
				return new jef.common.Entry<T, Boolean>(v1, c1);
			} else {
				return new jef.common.Entry<T, Boolean>(v2, c2);
			}
		}
	}

	// 取两个点中较小的那个
	private Entry<T, Boolean> min(T v1, boolean c1, T v2, boolean c2, int nullSupport, boolean closeFirst) {
		if (ObjectUtils.equals(v1, v2)) {
			if (closeFirst) {// 当值相等时，取闭区间
				return new jef.common.Entry<T, Boolean>(v1, c1 || c2);
			} else { // 当值相等时，取开区间
				return new jef.common.Entry<T, Boolean>(v1, c1 && c2);
			}

		} else {
			Comparable max = LangUtils.min(v1, v2, nullSupport);
			if (max == v1) {
				return new jef.common.Entry<T, Boolean>(v1, c1);
			} else {
				return new jef.common.Entry<T, Boolean>(v2, c2);
			}
		}
	}

	/**
	 * 判断给定的值是否 大于或大于等于(取决于左侧是否开区间) 左边界值。
	 * 
	 * @param obj
	 * @return
	 */
	protected boolean isInsideLeftBorder(T obj, boolean include) {
		if (obj == null)
			return false;
		if (min == null)
			return true;
		int i = obj.compareTo(min);
		if (i == 0)// 相等的情况
			return isLeftCloseSpan && include;
		if (i > 0) // 对方大于次方的最小值
			return true;
		return false;
	}

	/**
	 * 判断给定的值是否 小于或小于等于(取决于右侧是否开区间) 右边界值。
	 * 
	 * @param obj
	 * @return
	 */
	protected boolean isInsideRightBorder(T obj, boolean include) {
		if (obj == null)
			return false;
		if (max == null)
			return true;
		int i = obj.compareTo(max);
		if (i == 0)
			return isRightCloseSpan && include;
		if (i < 0)
			return true;
		return false;
	}
	
	/**
	 * 是否为全区间。
	 * 全区间就是从负无穷到正无穷的区间，是作为无效区间处理的。但是无效区间有两种，一种是空区间，一种是全区间
	 * @return
	 */
	public boolean isAll() {
		return min == null && max == null; 
	}
	
	
	public boolean isValid() {
		if (min == null && max == null)
			return true;
		if (min == null || max == null)
			return true;
		int n = min.compareTo(max);
		if (n > 0)
			return false;
		if (n == 0) {
			return isLeftCloseSpan && isRightCloseSpan;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public Dimension mergeOr(Dimension d) {
		if (d instanceof ComplexDimension) {
			return d.mergeOr(this);
		}
		if (d instanceof RangeDimension) {
			// 算法,两个范围的合并
			// 首先检查两个区间必须有重叠
			RangeDimension<T> other = (RangeDimension<T>) d;
			if (isInsideLeftBorder(other.max, other.isRightCloseSpan) && isInsideRightBorder(other.max, other.isRightCloseSpan)
					||
				isInsideLeftBorder(other.min, other.isLeftCloseSpan) && isInsideRightBorder(other.min, other.isLeftCloseSpan)) {//最大的点落在此处
				// 在重叠的基础上，取较小的左边界和较大的右边界
				Entry<T, Boolean> eLeft = min(this.min, this.isLeftCloseSpan, other.min, other.isLeftCloseSpan, LangUtils.NULL_IS_MINIMUM, true);// 左取小;
				Entry<T, Boolean> eRight = max(this.max, this.isRightCloseSpan, other.max, other.isRightCloseSpan, LangUtils.NULL_IS_MAXIMUM, true);// 右取大;
				return new RangeDimension<T>(eLeft.getKey(), eRight.getKey(), eLeft.getValue(), eRight.getValue());
			} else {
				ComplexDimension result=new ComplexDimension(this);
				return result.mergeOr(d);
			}
		} else {
			throw new UnsupportedOperationException("Unknown dimenssion type:" + d.getClass().getName());
		}
	}

	@Override
	public String toString() {
		T o = isPoint();
		if (o != null)
			return format(o);
		if (min == null && max == null){
			return "All!";
		}
		if (isValid()) {
			StringBuilder sb = new StringBuilder();
			sb.append(isLeftCloseSpan ? "[" : "(");
			sb.append(min == null ? "-∞" : format(min)).append(',').append(max == null ? "+∞" : format(max));
			sb.append(isRightCloseSpan ? "]" : ")");
			return sb.toString();
		} else {
			return "Invalid!";
		}
	}

	private String format(Object v) {
		if (v instanceof Date) {
			return DateUtils.formatDate((Date) v);
		} else {
			return String.valueOf(v);
		}
	}

	/**
	 * 如果当前区间是收敛了一个点，那么返回这个点
	 * 
	 * @return
	 */
	public T isPoint() {
		if (max == null || min == null)
			return null;
		if (ObjectUtils.equals(max, min)) {
			if (this.isLeftCloseSpan && this.isRightCloseSpan) {
				return min;
			}
		}
		return null;
	}

	public Object getMin() {
		return min;
	}

	public Object getMax() {
		return max;
	}

	// 逻辑非运算
	@SuppressWarnings("unchecked")
	public Dimension mergeNot() {
		ComplexDimension result = new ComplexDimension(new RangeDimension(null, min, true, !isLeftCloseSpan));
		result.mergeOr(new RangeDimension(max, null, !isRightCloseSpan, true));
		return result;
	}

	public static final List<Object> EMPTY_REGEXP=Arrays.<Object>asList(new RegexpDimension(""));
	
	@SuppressWarnings("unchecked")
	public static final RangeDimension<?> EMPTY_RANGE=new RangeDimension(null);
	
	/**
	 * 将范围值转化为枚举值
	 */
	@SuppressWarnings("unchecked")
	public Collection<?> toEnumationValue(Collection<PartitionFunction> funcs) {
		Object sObj = min;
		Object eObj = max;
		if(funcs.size()==1){
			Collection<?> result=funcs.iterator().next().iterator(sObj, eObj, isLeftCloseSpan, isRightCloseSpan);
			if(result.isEmpty())return EMPTY_REGEXP;
			return result;
		}
		Set<?> set=new TreeSet();
		for(PartitionFunction func:funcs){
			Collection add=func.iterator(sObj,eObj,isLeftCloseSpan,isRightCloseSpan);
			//当有多个维度组合时且任何一个维护无法得出结论时，实际上实际上是无法判断哪个维护是最密枚举，而将稀疏枚举向上传递可能造成误报和漏报.此时当做无法枚举处理。
			if(add.isEmpty())return add;
			set.addAll(add);
			
		}
		return set;
	}
}
