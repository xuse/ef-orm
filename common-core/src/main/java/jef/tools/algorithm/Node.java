package jef.tools.algorithm;

import java.util.HashMap;
import java.util.Map;

public class Node {
	private String name;
	private Map<Node, Integer> child = new HashMap<Node, Integer>();

	public Node(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<Node, Integer> getChild() {
		return child;
	}

	public void setChild(Map<Node, Integer> child) {
		this.child = child;
	}
}