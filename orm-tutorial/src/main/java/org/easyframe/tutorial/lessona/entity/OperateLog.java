package org.easyframe.tutorial.lessona.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import jef.database.DataObject;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.routing.function.KeyFunction;

@Entity
@PartitionTable(  // @PartitionTable表示分表分库规则配置
	key = {
		//每个@PartitionKey表示一个分库分表的字段。
		@PartitionKey(
				field = "created",      //条件字段名
				function = KeyFunction.YEAR_MONTH_DAY //对该字段执行函数取得yyyyMMdd,并作为表名后缀
				) 
	})
public class OperateLog extends DataObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @GeneratedValue(generator = "created-sys")
    private Date created;

    @Column(name="message",length=500)
    private String message;

    public enum Field implements jef.database.Field {
        id, created, message
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
