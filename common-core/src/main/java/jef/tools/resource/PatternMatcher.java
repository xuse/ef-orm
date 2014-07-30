package jef.tools.resource;

/**
 * 判断字符串是否匹配模式
 * <p></p>
 * @author huanghuafeng 2012-11-16 下午03:36:27
 * @version V1.0
 */
public interface PatternMatcher {

    /**
     * 判断字符串是否匹配模式
     * @author huanghuafeng 2012-11-16 下午03:36:18
     * @param pattern  模式，可以是正则表达式
     * @param source   需要匹配的字符串
     * @return   如果匹配则返回true，如果不匹配则返回false
     */
    boolean match(String pattern, String source);

	boolean matchStart(String fullPattern, String string);

	boolean isPattern(String substring);
}