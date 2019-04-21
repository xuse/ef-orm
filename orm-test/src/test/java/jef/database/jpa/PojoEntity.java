package jef.database.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Table(name = "Asdsad")
@Data
public class PojoEntity implements Serializable{
	private static final long serialVersionUID = 2981087206818983628L;

	@Column(name = "name")
	private String name;
	
	@Column(name = "id")
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "comments")
	private String comments;
}
