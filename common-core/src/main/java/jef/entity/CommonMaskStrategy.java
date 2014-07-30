package jef.entity;

import java.util.Arrays;

import jef.tools.StringUtils;
import jef.tools.string.RegexpUtils;

/**
 * 通用模糊策略类
 * <p>
 * 实现一些较为常用的模糊策略，如替换前几位字符为*、末几位字符为*、将第几位之后、末几位之前的所有字符替换为*等。
 * </p>
 * 
 * @Company Asiainfo-Linkage Technologies (China), Inc.
 * @author majj3@asiainfo-linkage.com
 * @Date 2012-7-12
 */
public class CommonMaskStrategy implements EvalStrategy {

	private static final String REGEX_EXPRESSION = "^([\\*,#])?(\\d*)?(\\(-?\\d*)\\,(-?\\d*\\))$";

	/**
	 * 默认替换所有字符为"*"
	 */
	private static final String DEFAULT_MASK_EXPRESSION = "*(,)";

	/**
	 * 默认替换符为"*"
	 */
	private static final String DEFAULT_MASK_CHAR = "*";

	private String[] parsedExpressions;

	/**
	 * 使用默认替换表达式，即替换所有字符为"*"
	 */
	public CommonMaskStrategy() {
		parsedExpressions = parse(DEFAULT_MASK_EXPRESSION);
	}

	/**
	 * @param expression
	 *            表达式格式为：替换符fixed(leftOffset, rightOffset)<br>
	 *            替换符仅支持*和#, <br>
	 *            替换符, fixed, leftOffset, rightOffset 均可以不指定，但位置需保留；<br>
	 *            leftOffset 支持负数，负数表示距离末尾的偏移位置(即替换末几位字符)，负数时rightOffset值无效，<br>
	 *            leftOffset 不指定则默认为0; <br>
	 *            rightOffset 支持负数，负数表示距离末尾的偏移位置。<br>
	 *            举例：<br>
	 *            "(,)", 表示将所有字符替换为*<br>
	 *            "#(,)", 表示将所有字符替换为#<br>
	 *            "#3(,)", 表示将所有字符替换为3个#(即###)<br>
	 *            "#(2,8)", 表示将2-8位字符替换为#<br>
	 *            "#3(2,8)", 表示将2-8位字符替换3个#(即###)<br>
	 *            "#(2,)", 表示将第2个字符之后的所有字符替换为#<br>
	 *            "#(-2,)", 表示将末2位替换为#<br>
	 *            "#(,8)", 表示将1-8位字符替换为#(即替换第9位之前的所有字符)<br>
	 *            "#(,-2)", 表示将末2位之前的所有字符替换为#<br>
	 *            "#(3,-2)", 表示将第3位之后、末2位之前的所有字符替换为#
	 */
	public CommonMaskStrategy(String expression) {
		parsedExpressions = parse(expression);
		if (parsedExpressions == null) {
			throw new IllegalArgumentException("Illeagal mask expression:" + expression);
		}
		if (parsedExpressions[0] == null) {
			parsedExpressions[0] = DEFAULT_MASK_CHAR;
		}
	}

	private String[] parse(String expression) {
		return RegexpUtils.getMatcherResult(expression, REGEX_EXPRESSION, true);
	}

	public String eval(Object srcValue, Object thisEntityObj) {
		if (srcValue == null)
			return null;

		String value = toStringValue(srcValue);
		return mask(value, parseRange(value, parsedExpressions[2], parsedExpressions[3]));
	}

	private String toStringValue(Object srcValue) {
		return srcValue instanceof String ? (String) srcValue : srcValue.toString();
	}

	private Integer[] parseRange(String value, String leftOffset, String rightOffset) {
		// 初始化下界为0, 上界为value.length
		Integer[] result = new Integer[] { 0, value.length() };

		// 计算起始位置
		String boundaryValue = leftOffset.substring(1);
		if (StringUtils.isNotBlank(boundaryValue)) {
			result[0] = Integer.valueOf(boundaryValue);

			// 起始位置为负数时，忽略rightOffset
			if (result[0] < 0) {
				result[0] = value.length() + result[0];
				return result;
			}
		}

		// 计算替换长度
		boundaryValue = rightOffset.substring(0, rightOffset.length() - 1);
		if (StringUtils.isNotBlank(boundaryValue)) {
			int rightIndex = Integer.valueOf(boundaryValue);
			result[1] = calculateReplaceLength(value.length(), result[0], rightIndex);
		}

		return result;
	}

	private int calculateReplaceLength(int totalLength, int leftIndex, int rightIndex) {
		int len = rightIndex;
		if (rightIndex < 0) {
			len = totalLength - leftIndex + rightIndex;
		} else {
			len = rightIndex - leftIndex;
		}
		return len;
	}

	private String mask(String srcValue, Integer[] ranges) {
		if (StringUtils.isBlank(parsedExpressions[1])) {
			return StringUtils.replace(srcValue, parsedExpressions[0].charAt(0), ranges[0],
					ranges[1]);
		} else {
			return StringUtils.replace(srcValue, parsedExpressions[0].charAt(0),
					Integer.parseInt(parsedExpressions[1]), ranges[0], ranges[1]);
		}
	}

	public static void main(String[] args) {
		CommonMaskStrategy maskStategy = new CommonMaskStrategy();
		String[] ranges = maskStategy.parse("*(4,)");
		System.out.println(ranges[0] + ranges[1] + ranges[2]);
		ranges = maskStategy.parse("(4,6)");
		System.out.println(ranges[0] + ranges[1] + ranges[2]);
		System.out.println("parse(\"*(,)\"): " + Arrays.toString(maskStategy.parse("*(,)")));
		System.out.println("parse(\"*(4,6)\"): " + Arrays.toString(maskStategy.parse("*(4,6)")));
		System.out.println("parse(\"*(4,6]\"): " + Arrays.toString(maskStategy.parse("*(4,6]")));
		System.out.println("parse(\"*(4,]\"): " + Arrays.toString(maskStategy.parse("*(4,]")));
		System.out.println("parse(\"*[,6)\"): " + Arrays.toString(maskStategy.parse("*[,6)")));
		System.out.println("parse(\"&[,6)\"): " + Arrays.toString(maskStategy.parse("&[,6)")));
		System.out.println("parse(\"#3(0,6)\"): " + Arrays.toString(maskStategy.parse("#3(0,6)")));
		System.out.println("parse(\"#(0,-6)\"): " + Arrays.toString(maskStategy.parse("#(0,-6)")));
		System.out.println("parse(\"#3(0,-6)\"): " +
				Arrays.toString(maskStategy.parse("#3(0,-6)")));
		System.out.println("parse(\"#(-4,)\"): " + Arrays.toString(maskStategy.parse("#(-4,)")));
	}
}
