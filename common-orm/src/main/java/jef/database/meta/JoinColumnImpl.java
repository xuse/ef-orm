package jef.database.meta;

import java.lang.annotation.Annotation;

import javax.persistence.JoinColumn;

public class JoinColumnImpl implements javax.persistence.JoinColumn {

	private String name;
	private String referencedColumnName;
	private boolean nullable;
	private boolean unique;
	private boolean insertable;
	private boolean updateable;
	private String columnDefinition;
	private String table;

	public JoinColumnImpl() {
	}

	public JoinColumnImpl(JoinColumn source) {
		this.name = source.name();
		this.referencedColumnName = source.referencedColumnName();
		this.nullable = source.nullable();
		this.insertable = source.insertable();
		this.updateable = source.updatable();
		this.unique = source.unique();
		this.columnDefinition = source.columnDefinition();
		this.table = source.table();
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String referencedColumnName() {
		return referencedColumnName;
	}

	@Override
	public boolean unique() {
		return unique;
	}

	@Override
	public boolean nullable() {
		return nullable;
	}

	@Override
	public boolean insertable() {
		return insertable;
	}

	@Override
	public boolean updatable() {
		return updateable;
	}

	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	@Override
	public String table() {
		return table;
	}

	public void reverseColumn() {
		String col = this.referencedColumnName;
		this.referencedColumnName = this.name;
		this.name = col;
	}
}
