package jef.common;

/**
 * 轻量级容器
 * @author jiyi
 *
 */
public class PairSS {
	public String first;
	public String second;
	
	public PairSS() {
	}
	
	public PairSS(String f,String s) {
		first = f;
		second = s;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getSecond() {
		return second;
	}

	public void setSecond(String second) {
		this.second = second;
	}
}
