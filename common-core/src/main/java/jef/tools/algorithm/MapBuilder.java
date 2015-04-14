package jef.tools.algorithm;

import java.util.Set;

public class MapBuilder {
	public Node build(Set<Node> open, Set<Node> close) {
		Node nodeA = new Node("A");
		Node nodeB = new Node("B");
		Node nodeC = new Node("C");
		Node nodeD = new Node("D");
		Node nodeE = new Node("E");
		Node nodeF = new Node("F");
		Node nodeG = new Node("G");
		Node nodeH = new Node("H");
		nodeA.getChild().put(nodeB, 1);
		nodeA.getChild().put(nodeC, 1);
		nodeA.getChild().put(nodeD, 4);
		nodeA.getChild().put(nodeG, 5);
		nodeA.getChild().put(nodeF, 2);
		nodeB.getChild().put(nodeA, 1);
		nodeB.getChild().put(nodeF, 2);
		nodeB.getChild().put(nodeH, 4);
		nodeC.getChild().put(nodeA, 1);
		nodeC.getChild().put(nodeG, 3);
		nodeD.getChild().put(nodeA, 4);
		nodeD.getChild().put(nodeE, 1);
		nodeE.getChild().put(nodeD, 1);
		nodeE.getChild().put(nodeF, 1);
		nodeF.getChild().put(nodeE, 1);
		nodeF.getChild().put(nodeB, 2);
		nodeF.getChild().put(nodeA, 2);
		nodeG.getChild().put(nodeC, 3);
		nodeG.getChild().put(nodeA, 5);
		nodeG.getChild().put(nodeH, 1);
		nodeH.getChild().put(nodeB, 4);
		nodeH.getChild().put(nodeG, 1);
		open.add(nodeB);
		open.add(nodeC);
		open.add(nodeD);
		open.add(nodeE);
		open.add(nodeF);
		open.add(nodeG);
		open.add(nodeH);
		close.add(nodeA);
		return nodeA;
	}
}