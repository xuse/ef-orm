package jef.database.routing.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jef.database.annotation.PartitionFunction;
import jef.database.query.RegexpDimension;
import jef.tools.StringUtils;
import jef.tools.string.CharUtils;
import jef.tools.string.StringIterator;

import org.apache.commons.lang.ObjectUtils;

/**
 * 描述针对分表的维度，不是一个可度量的维度，而是直接将这个字符串拼到表名中
 */

public final class RawFunc implements PartitionFunction<Object>{
	private String[] nullValue;
	private int maxLen;

	public RawFunc(String defaultWhenFieldIsNull, int maxLen) {
		if (defaultWhenFieldIsNull.length() > 0) {
			this.nullValue = StringUtils.split(defaultWhenFieldIsNull, ',');
			for (int i = 0; i < nullValue.length; i++) {
				nullValue[i] = nullValue[i].trim();
			}
		}
		if (maxLen > 0)
			this.maxLen = maxLen;
	}

	public String eval(Object value) {
		return String.valueOf(value);
	}

	@SuppressWarnings("unchecked")
	public List<Object> iterator(Object min, Object max, boolean left, boolean right) {
		if (min == null && max == null) {
			if (nullValue != null) {
				return Arrays.asList((Object[]) nullValue);
			} else {
				return Collections.EMPTY_LIST;
			}
		} else if (ObjectUtils.equals(min, max)) {
			return Arrays.asList(min);
		} else {
			// 范围丢失

			if (min instanceof Integer && max instanceof Integer) {
				return iteratorInt((Integer) min, (Integer) max,left,right);
			} else if (min instanceof Long && max instanceof Long) {
				return iteratorLong((Long) min, (Long) max,left,right);
			} else if (min instanceof String) {
				return iteratorString((String) min, (String) max,left,right);
			} else {
				return Arrays.asList(min, max);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> iteratorLong(long min, long max,boolean left,boolean right) {
		List<Object> result = new ArrayList<Object>();
		if (max < min) {
			return Collections.EMPTY_LIST;
		}
		long step = 1;
		if ((max - min) > 1000) {
			step = (max - min) / 100;
		}
		long i = left?min:min+step;
		for (; i < max; i += step) {
			result.add(i);
		}
		if (right && i < max) {
			result.add(max);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<Object> iteratorInt(int min, int max,boolean left,boolean right) {
		List<Object> result = new ArrayList<Object>();
		if (max < min) {
			return Collections.EMPTY_LIST;
		}
		int step = 1;
		if ((max - min) > 1000) {
			step = (max - min) / 100;
		}
		int i = left?min:min+step;
		for (; i < max; i += step) {
			result.add(i);
		}
		if (right && i < max) {
			result.add(max);
		}
		return result;
	}

	private List<Object> iteratorString(String min, String max,boolean left, boolean right) {
		//由于字符串截断因素的影响，开始的位置都应该当做是闭区间。
		StringIterator st = new StringIterator(min, max, maxLen, "0123456789".toCharArray(),true,right);
		List<Object> result = new ArrayList<Object>();
		while (st.hasNext()) {
			result.add(st.next());
		}
		return result;
	}

	public boolean acceptRegexp() {
		return true;
	}

	public Collection<Object> iterator(RegexpDimension regexp) {
		if (maxLen > 0 && regexp.getBaseExp().length() >= maxLen) {
			return Arrays.<Object> asList(regexp.getBaseExp());
		} else {
			String baseExp = regexp.getBaseExp();
			Collection<Object> list = new ArrayList<Object>(100);
			for (char c : CharUtils.ALPHA_NUM_UNDERLINE) {
				list.add(baseExp + c);
			}
			return list;
		}

	}

}
