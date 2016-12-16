package jef.database.ddl;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import jef.database.annotation.EasyEntity;
import jef.database.annotation.Indexed;

@Table(name = "TABLE_FOR_TEST",indexes={
		@Index(columnList="id,name desc",name="IDX_DEFAULT_TEST",unique=true)  //单独再定义一个复合索引		
},uniqueConstraints={@UniqueConstraint(
		columnNames={"code","name"})})
@Entity
@EasyEntity(checkEnhanced=false)
public class TableForTest extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Id
	@Column(length=20)
	private int id;
	
	@Column(length=64)
    private String name;

	@Column(columnDefinition="number(18,8)",name="FLOAT_NUMBER")
	private BigDecimal floatNumber;
	/**
	 * 指定列有索引
	 */
	@Indexed
    @Column(columnDefinition="char",length=4)
    private String code;

	/**
	 * 为一个日期列添加@GeneratedValue后，在插入时如果没有指定数据，将使用sysdate填入
	 */
    @GeneratedValue(generator="created")
    @Column(name="create_time")
    private Date created;

	/**
	 * 为一个日期列添加@GeneratedValue后，在插入或者更新时，将使用sysdate填入
	 */
    @GeneratedValue(generator="modified")
    @Column(name="last_modified")
    private Date modified;
    
    
    @GeneratedValue(generator="created-sys")
    @Column(name="create_time_sys")
    private Date createdSys;
    

    @GeneratedValue(generator="modified-sys")
    @Column(name="last_modified_sys")
    private Date modifiedSys;
    
    
    
    
    public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getCreatedSys() {
		return createdSys;
	}

	public void setCreatedSys(Date createdSys) {
		this.createdSys = createdSys;
	}

	public Date getModifiedSys() {
		return modifiedSys;
	}

	public void setModifiedSys(Date modifiedSys) {
		this.modifiedSys = modifiedSys;
	}

	@Column(name="expire_time")
    private Date expireTime;
    
    private long amount;

    @Lob
    private byte[] data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public BigDecimal getFloatNumber() {
		return floatNumber;
	}

	public void setFloatNumber(BigDecimal floatNumber) {
		this.floatNumber = floatNumber;
	}

	public enum Field implements jef.database.Field {
        id, name, code, amount, expireTime, data,created,createdSys,modified,modifiedSys,floatNumber
    }
}
