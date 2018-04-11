package jef.database.test.generator;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import jef.database.DataObject;
import jef.database.annotation.HiloGeneration;

/**
 * 关于自增主键的配置和设计
 * 
 * 
 * 一、JPA实现分为 Auto,Sequence, Identity, Table四种， AUTO的实现即为根据数据库 Identity >
 * Sequence > Table (由于数据库多少都支持前两个特性，所以实际上Table特性无效) 
 * 
 * 二、JEF设计中，标记为
 * IDentity和Sequence的默认都被处理为 AUTO. （设置为开关，默认开启） 
 * 
 * 三、如果关闭自动映射为Auto的功能，那么配成是什么就是什么。
 * 四、修饰： Step跳跃模式， Hilo模式。
 * 
 * @author Administrator
 * 
 */
@Entity
public class IdTestTable extends DataObject {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@GeneratedValue(strategy = GenerationType.TABLE)
	@TableGenerator(name = "table-seq1", table = "AAA1" , allocationSize = 10, valueColumnName = "seq_value", pkColumnName="table_name",pkColumnValue = "key1")
	private int tableSeq;

	@GeneratedValue(strategy = GenerationType.TABLE)
	@HiloGeneration
	private int tableHilo;

	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@SequenceGenerator(name = "VVVV", sequenceName = "AAA", initialValue = 1000)
	private int seq;

	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@HiloGeneration
	private int seqHilo;

	//@GeneratedValue(generator = "uuid")
	private String uuid;

	//@GeneratedValue(generator = "guid")
	private String guid;

	@GeneratedValue
	private Date created;

	@GeneratedValue
	private Date updated;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getTableSeq() {
		return tableSeq;
	}

	public void setTableSeq(int tableSeq) {
		this.tableSeq = tableSeq;
	}

	public int getTableHilo() {
		return tableHilo;
	}

	public void setTableHilo(int tableHilo) {
		this.tableHilo = tableHilo;
	}

	public int getSeqHilo() {
		return seqHilo;
	}

	public void setSeqHilo(int seqHilo) {
		this.seqHilo = seqHilo;
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public enum Field implements jef.database.Field {
		id, tableSeq, tableHilo, seq, seqHilo, uuid, guid, created, updated
	}
}
