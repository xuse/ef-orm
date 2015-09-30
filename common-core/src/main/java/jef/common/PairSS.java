package jef.common;

import com.google.common.base.Objects;

/**
 * 轻量级容器
 * 
 * @author jiyi
 *
 */
public class PairSS {
	public String first;
	public String second;

	public PairSS() {
	}

	public PairSS(String f, String s) {
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
		int hash = 0;
		if (first != null) {
			hash += first.hashCode();
		}
		if (second != null) {
			hash += second.hashCode();
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PairSS) {
			PairSS rhs = (PairSS) obj;
			if (!Objects.equal(this.first, rhs.first)) {
				return false;
			}
			if (!Objects.equal(this.second, rhs.second)) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return first + ":" + second;
	}
}
