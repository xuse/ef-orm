package jef.database.routing.function;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.database.ORMConfig;
import jef.database.annotation.PartitionFunction;
import jef.database.query.RangeDimension;
import jef.database.query.RegexpDimension;
import jef.tools.StringUtils;

/**
 * 对文本进行截断，并获取hash值，然后取模，最后按Range进行分区
 * 
 * @author jiyi
 *
 */
public class HashMod1024MappingFunction implements PartitionFunction<String> {

	private int maxLength = 0;

	private int[] ranges;

	private String[] results;

	private Collection<String> allResult;

	private BigInteger mod = BigInteger.valueOf(1024);
	
	
	public static void main(String[] args) {
//		HashMod1024MappingFunction ss = new HashMod1024MappingFunction("0:DB1,256:DB2,512:DB3,768:DB4", 0);
		HashMod1024MappingFunction ss = new HashMod1024MappingFunction("0-255:DB1,256-511:DB2,512-767:DB3,768-1023:DB4", 0);
		
		
		System.out.println(ss.allModulus());
		System.out.println("-------------------------");
		System.out.println(ss.getResult(1));
		System.out.println(ss.getResult(2));
		System.out.println("-------------------------");
		System.out.println(ss.getResult(999));
		System.out.println(ss.getResult(1023));
		System.out.println(ss.getResult(1024));
		System.out.println("-------------------------");
		System.out.println(ss.eval("A"));
		System.out.println(ss.eval("ABC123"));
		System.out.println(ss.eval("毛泽东"));
		System.out.println(ss.eval("周恩来"));
		System.out.println(ss.eval("蒋介石"));
		System.out.println(ss.eval("马克思"));
		System.out.println(ss.eval("孙悟空"));
		System.out.println(ss.eval("猪八戒"));
	}

	/**
	 * 空构造
	 */
	public HashMod1024MappingFunction() {
		this(ORMConfig.getInstance().getPartitionBucketRange(), 0);
	}

	/**
	 * 构造
	 * 
	 * @param expression
	 * @param digit
	 */
	public HashMod1024MappingFunction(String expression, int digit) {
		if(StringUtils.isEmpty(expression)) {
			expression=ORMConfig.getInstance().getPartitionBucketRange();
		}
		this.maxLength = digit;
		
		List<PairSO<RangeDimension<Integer>>> ranges = new ArrayList<PairSO<RangeDimension<Integer>>>();
		for (String s : StringUtils.split(expression, ",")) {
			int index = s.lastIndexOf(':');
			if (index < 1) {
				throw new IllegalArgumentException("Invalid config of map:" + s);
			}
			String value = s.substring(index + 1);
			s = s.substring(0, index);
			index = s.lastIndexOf('-');
			if (index > 0) { // 如果第一位就是-，认为是负号，不算分隔符
				int start = StringUtils.toInt(s.substring(0, index), null);
				int end = StringUtils.toInt(s.substring(index + 1), null);
				addRange(start, end+1, value, ranges);
			} else {
				int start = StringUtils.toInt(s, null);
				addRange(start, null, value, ranges);
			}
		}
		checkLast(ranges);
		for(PairSO<RangeDimension<Integer>> entry:ranges) {
			LogUtil.info(entry.first+":"+entry.second);
		}
		this.ranges = new int[ranges.size()];
		this.results = new String[ranges.size()];
		for (int i = 0; i < ranges.size(); i++) {
			PairSO<RangeDimension<Integer>> p = ranges.get(i);
			this.results[i] = p.first;
			this.ranges[i] = p.second.getMin();
		}
		this.allResult = Arrays.asList(results);
	}

	private void checkLast(List<PairSO<RangeDimension<Integer>>> ranges) {
		RangeDimension<Integer> last=ranges.isEmpty()?null:ranges.get(ranges.size()-1).second;
		if(last.getMax()==null) {
			last.setMax(1024);
		}
		if(last.getMax()!=1024) {
			throw new IllegalArgumentException();
		}
	}

	@SuppressWarnings("unchecked")
	private void addRange(int start, Integer end, String value, List<PairSO<RangeDimension<Integer>>> ranges) {
		if (end!=null && end < start) {
			throw new IllegalArgumentException();
		}
		RangeDimension<Integer> last=ranges.isEmpty()?null:ranges.get(ranges.size()-1).second;
		if (last==null) {
			ranges.add(new PairSO<RangeDimension<Integer>>(value,RangeDimension.createLC(start, end)));
		} else {
			if(last.getMax()==null) {
				last.setMax(start);
			}
			if(last.getMax()!=start) {
				throw new IllegalArgumentException("The range ["+last.getMax()+"-"+start+"] was skipped.");
			}
			ranges.add(new PairSO<RangeDimension<Integer>>(value,RangeDimension.createLC(start, end)));
		}
	}

	@Override
	public String eval(String value) {
		int buck = value == null ? 0 : (BigInteger.valueOf(trim(value).hashCode()).mod(mod).intValue());
		return getResult(buck);
	}

	private String trim(String value) {
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}

	private Collection<String> allModulus() {
		return allResult;
	}

	private String getResult(int buck) {
		for (int id = 0; id < ranges.length - 1; id++) {
			if (buck >= ranges[id] && buck < ranges[id + 1]) {
				return results[id];
			}
		}
		return results[ranges.length - 1];
	}

	@Override
	public Collection<String> iterator(String min, String max, boolean left, boolean right) {
		// 由于
		if (min == null || max == null) {
			return allModulus();
		}
		if (StringUtils.equals(min, max)) {
			if (!left && left == right) {
				return Collections.emptyList();
			} else {
				return Collections.singletonList(eval(min));
			}
		}
		return allModulus();
	}

	@Override
	public boolean acceptRegexp() {
		return false;
	}

	@Override
	public Collection<String> iterator(RegexpDimension regexp) {
		throw new UnsupportedOperationException();
	}

}
