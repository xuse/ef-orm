package jef.orm.multitable2;

import java.io.Serializable;

import javax.persistence.Entity;

@Entity
public class TextValuePair implements Comparable<TextValuePair>,Serializable{
	private static final long serialVersionUID = -2213222495346597158L;

	private String text;

	private String value;

	public TextValuePair() {
	}

	public TextValuePair(String text, String value) {
		this.text = text;
		this.value = value;
	}

	public String getText() {
		return text;
	}

//	@TypeAdapters(value={
//			@TypeAdapter(converter = DateToStringConvert.class, parameters = "yyyy-MM-dd")
//	})
	public void setText(String text) {
		this.text = text;
	}

	public String getValue() {
		return value;
	}

//	@TypeAdapters(value={
//			@TypeAdapter(converter = DateToStringConvert.class, parameters = "yyyy-MM-dd")
//	})
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if(text==null)return value;
		if(value==null)return text;
		return new StringBuilder(text.length()+value.length()+2).append('[').append(value).append(']').append(text).toString();
	}

	@Override
	public int hashCode() {
		return text.hashCode()+value.hashCode();
	}

	public int compareTo(TextValuePair o) {
		if(o==null)return 1;
		if(o.value==null)return 1;
		return this.value.compareTo(o.value);
	}
	
}
