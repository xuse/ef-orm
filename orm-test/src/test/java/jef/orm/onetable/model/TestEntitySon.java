package jef.orm.onetable.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity()
@Table(name = "test_entity_son")
public class TestEntitySon extends TestEntity {

	@Column(name = "field_9")
	private String field;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
	
	public enum Field implements jef.database.Field{
		field
	}
}
