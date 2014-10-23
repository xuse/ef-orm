package jef.orm.onetable.model;

import javax.persistence.Entity;

import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
public class Keyword extends jef.database.DataObject {

	private String desc;

	private String comment;

	private int percent;

	private String top;

	private String order;

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}

	public String getTop() {
		return top;
	}

	public void setTop(String top) {
		this.top = top;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public enum Field implements jef.database.Field {
		desc, comment, percent, top, order
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	
}
