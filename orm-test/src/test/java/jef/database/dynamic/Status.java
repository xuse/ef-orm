package jef.database.dynamic;

import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.annotation.UnsavedValue;

@Entity
public class Status extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
	@UnsavedValue("-1")
	private int code;

	private String data;

	public Status() {
	}

	public Status(int i, String string) {
		this.code = i;
		this.data = string;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public enum Field implements jef.database.Field {
		code, data
	}
}
