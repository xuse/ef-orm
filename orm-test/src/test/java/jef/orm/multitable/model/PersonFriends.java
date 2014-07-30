package jef.orm.multitable.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import jef.database.DataObject;

/**
 * JEF-ORM 演示用例
 * 
 * 描述 人 <-> 人之间的朋友关系（多对多）
 * @author Administrator
 * @Date 2011-4-12
 */
@Entity
@Table(name="PERSON_FRIENDS")
public class PersonFriends extends DataObject{
	private static final long serialVersionUID = 1L;

	@Id
	@Column
	private int pid;
	
	@Id
	@Column
	private int friendId;
	
	@Column
	private transient String comment;
	
	@OneToOne(targetEntity=Person.class)
	@JoinColumn(name="friendId",referencedColumnName="id")
	private Person friend;
	
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public enum Field implements jef.database.Field{
		pid, friendId,comment
	}
	
	public int getPid() {
		return pid;
	}
	public void setPid(int pid) {
		this.pid = pid;
	}
	public int getFriendId() {
		return friendId;
	}
	public void setFriendId(int friendId) {
		this.friendId = friendId;
	}
	public Person getFriend() {
		return friend;
	}
	public void setFriend(Person friend) {
		this.friend = friend;
	}
}
