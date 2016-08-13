package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.wrapper.processor.BindVariableDescription;

public class SqlBuilder {
	private ResultSetLaterProcess afterProcessor;
	protected final Deque<Section> section = new LinkedList<Section>();
	private Section root;

	static class Section {
		private String name;
		private final StringBuilder sb = new StringBuilder();
		private final List<BindVariableDescription> bind = new ArrayList<BindVariableDescription>();

		Section(String name) {
			this.name = name;
		}
	}

	public SqlBuilder() {
		root = new Section("");
		section.add(root);
	}

	public BindSql build() {
		BindSql result = new BindSql(root.sb.toString(), root.bind);
		result.setReverseResult(afterProcessor);
		return result;
	}

	public int sqlLength() {
		return root.sb.length();
	}

	public int sectionLength() {
		Section sec = section.getLast();
		return sec.sb.length();
	}

	public void addBefore(String add) {
		Section sec = section.getLast();
		String old = sec.sb.toString();
		sec.sb.setLength(0);
		sec.sb.append(add).append(old);
	}

	public void append(String append) {
		Section sec = section.getLast();
		sec.sb.append(append);
	}

	public void append(String... append) {
		Section sec = section.getLast();
		for (String a : append) {
			sec.sb.append(a);
		}
	}

	public void startSection(String name) {
		section.add(new Section(name));
	}

	public void endSection() {
		Section sec = section.peekLast();
		Section current = section.getLast();
		if (sec.sb.length() > 0) {
			current.sb.append(sec.name);
			current.sb.append(sec.sb);
			current.bind.addAll(sec.bind);
		}
	}

	public void addBind(BindVariableDescription bind) {
		Section sec = section.getLast();
		sec.bind.add(bind);
	}

	public void addAllBind(List<BindVariableDescription> bind) {
		Section sec = section.getLast();
		sec.bind.addAll(bind);
	}
}
