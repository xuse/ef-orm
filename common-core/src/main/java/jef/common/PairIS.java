package jef.common;

public class PairIS {
	public int first;
	public String second;
	
	public PairIS(int f,String s) {
		first = f;
		second = s;
	}

	public PairIS() {
	}
	
	public int getFirst() {
		return first;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public String getSecond() {
		return second;
	}

	public void setSecond(String second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return first+":"+second;
	}
	
	
}
