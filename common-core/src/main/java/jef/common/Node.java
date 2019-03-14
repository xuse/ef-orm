package jef.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 链表数据结构
 * 
 * 一个最简单的单链表
 * 
 * @author jiyi
 *
 * @param <E>
 */
public final class Node<E> implements Iterable<E>{
	private Node<E> next;
	private E value;

	public Node(E e) {
		this.value = e;
	}

	public Node<E> getNext() {
		return next;
	}

	public void setNext(Node<E> next) {
		this.next = next;
	}

	public E getValue() {
		return value;
	}

	/**
	 * 在整个链表的最后加入指定值的节点
	 * 
	 * @param value
	 * @return 当前节点
	 */
	public Node<E> append(E value) {
		Node<E> tail = this;
		while (tail.next != null) {
			tail = tail.next;
		}
		Node<E> last=new Node<E>(value);
		tail.setNext(last);
		return this;
	}
	
	/**
	 * 将当前节点加在新节点之后，然后以新节点作为链表的开始 
	 * @param value
	 * @return
	 */
	public Node<E> insert(E value){
		Node<E> first=new Node<E>(value);
		first.setNext(this);
		return first;
	}
	
	/**
	 * 计算从当前节点到链表尾部还有几个节点
	 * @return 如果当前节点是链表的结尾返回1
	 */
	public int size() {
		Node<E> tail = this;
		int count=1;
		while (tail.next != null) {
			count++;
			tail = tail.next;
		}
		return count;
	}
	

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private Node<E> n=Node.this;
			@Override
			public boolean hasNext() {
				return n!=null;
			}

			@Override
			public E next() {
				Node<E> e=this.n;
				if(e==null) {
					throw new NoSuchElementException();
				}
				this.n=n.next;
				return e.value;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
