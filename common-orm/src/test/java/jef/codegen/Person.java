package jef.codegen;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.database.DataObject;
import jef.database.annotation.Indexed;

/**
 * JEF-ORM 演示用例
 * 
 * 描述 人 这一实体
 * 
 * @author Administrator
 * @Date 2011-4-12
 */
@Entity
@Table(name = "person_table")
public class Person extends DataObject {

	private static final long serialVersionUID = 1L;

	@Column
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Integer id;

	@Column(name = "person_name", length = 80, nullable = false)
	@Indexed
	private String name;

	/**
	 * 元模型定义 从JEF 0.4 开始，JEF部分支持JPA的Annotaion定义，从而可以不在类中定义一个静态的TableMetadata字段.
	 * 但是JPA规范确定并支持，在entity实体的相同目录下，定义一个名称加下划线的元模型类。 为了维持Entity的POJO特性。
	 * JEF认为，大多数情况下，不需要这样累赘的，重量级的元模型解决方案，仅仅通过一个枚举的定义， 即可起到JPA元模型的默认效果。
	 * 
	 * 为此，依然保留JEF现有的元模型定义
	 */
	public enum Field implements jef.database.Field {
		id, name
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
