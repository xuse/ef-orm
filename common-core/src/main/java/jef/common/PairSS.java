package jef.common;

import org.apache.commons.lang.ObjectUtils;

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

	@Override
	public int hashCode() {
		int hash=0;
		if(first!=null){
			hash+=first.hashCode();
		}
		if(second!=null){
			hash+=second.hashCode();
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PairSS){
			PairSS rhs=(PairSS)obj;
			if(!ObjectUtils.equals(this.first, rhs.first)){
				return false;
			}
			if(!ObjectUtils.equals(this.second, rhs.second)){
				return false;
			}
			return true;
		}
		return false;
	}
}
