package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.wrapper.variable.Variable;

public class SqlBuilder {
	private ResultSetLaterProcess afterProcessor;
	protected final LinkedList<Section> section = new LinkedList<Section>();
	private Section root;

	static class Section {
		private String name;
		private final StringBuilder sb = new StringBuilder();
		private final List<Variable> bind = new ArrayList<Variable>();

		Section(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name + "[" + sb.toString() + "]";
		}
	}

	public SqlBuilder() {
		root = new Section("");
		section.add(root);
	}

	public int sectionLength() {
		Section sec = section.peek();
		return sec.sb.length();
	}

	public void addBefore(String add) {
		Section sec = section.peek();
		String old = sec.sb.toString();
		sec.sb.setLength(0);
		sec.sb.append(add).append(old);
	}

	public void append(String append) {
		Section sec = section.peek();
		sec.sb.append(append);
	}

	public void append(String... append) {
		Section sec = section.peek();
		for (String a : append) {
			sec.sb.append(a);
		}
	}

	public void startSection(String name) {
		section.push(new Section(name));
	}

	public void endSection() {
		Section sec = section.pop();
		Section current = section.peek();
		if (sec.sb.length() == 0) {
			return;
		}
		if (current.sb.length() > 0) {
			current.sb.append(sec.name);
		}
		current.sb.append(sec.sb);
		current.bind.addAll(sec.bind);
	}

	public void addBind(Variable bind) {
		Section sec = section.peek();
		sec.bind.add(bind);
	}

	public void addAllBind(List<Variable> bind) {
		Section sec = section.peek();
		sec.bind.addAll(bind);
	}
	
	public BindSql build() {
		if (this.section.size() > 1) {
			throw new IllegalStateException();
		}
		BindSql result = new BindSql(root.sb.toString(), root.bind);
		result.setReverseResult(afterProcessor);
		return result;
	}

	public boolean isNotEmpty() {
		if (this.section.size() > 1) {
			throw new IllegalStateException();
		}
		return root.sb.length()>0;
		
	}
}
