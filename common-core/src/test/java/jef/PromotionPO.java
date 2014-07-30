package jef;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class PromotionPO {
	private String code;
	private String description;
	private Double discount;
	private Date endTime;
	private long ID;
	private int priority;
	
	private int promotionType;
	private Timestamp startTime;
	private String supplierID;
	
	private String field1;
	private String field2;
	private String field3;
	private String field4;
	private String field5;
	private String field6;
	private String field7;
	
	private Set<List<? extends CharSequence>[]> type;
	
	private Boolean a1B;
	private Object a0a;
	
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Double getDiscount() {
		return discount;
	}
	public void setDiscount(Double discount) {
		this.discount = discount;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public long getID() {
		return ID;
	}
	public void setID(long ID) {
		this.ID = ID;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public int getPromotionType() {
		return promotionType;
	}
	public void setPromotionType(int promotionType) {
		this.promotionType = promotionType;
	}
	public Timestamp getStartTime() {
		return startTime;
	}
	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}
	public String getSupplierID() {
		return supplierID;
	}
	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}
	public String getField1() {
		return field1;
	}
	public void setField1(String field1) {
		this.field1 = field1;
	}
	public String getField2() {
		return field2;
	}
	public void setField2(String field2) {
		this.field2 = field2;
	}
	public String getField3() {
		return field3;
	}
	public void setField3(String field3) {
		this.field3 = field3;
	}
	public String getField4() {
		return field4;
	}
	public void setField4(String field4) {
		this.field4 = field4;
	}
	public String getField5() {
		return field5;
	}
	public void setField5(String field5) {
		this.field5 = field5;
	}
	public String getField6() {
		return field6;
	}
	public void setField6(String field6) {
		this.field6 = field6;
	}
	public String getField7() {
		return field7;
	}
	public void setField7(String field7) {
		this.field7 = field7;
	}
	public Set<List<? extends CharSequence>[]> getType() {
		return type;
	}
	public void setType(Set<List<? extends CharSequence>[]> type) {
		this.type = type;
	}
	public Boolean getA1B() {
		return a1B;
	}
	public void setA1B(Boolean a1b) {
		a1B = a1b;
	}
	public Object getA0a() {
		return a0a;
	}
	public void setA0a(Object a0a) {
		this.a0a = a0a;
	}
}
