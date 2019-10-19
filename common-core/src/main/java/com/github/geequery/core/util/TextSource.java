package com.github.geequery.core.util;

import java.io.File;
import java.nio.charset.Charset;

import com.github.geequery.core.util.Input.CsvInput;
import com.github.geequery.core.util.Input.LineInput;

public class TextSource {
	private final File root;
	private String[] extNamePattern;
	private Charset charset;
	
	
	private TextSource(File root) {
		this.root=root;
	}
	
	public static TextSource of(File file) {
		return new TextSource(file);
	}
	
	public TextSource extPattern(String... exts) {
		this.extNamePattern=exts;
		return this;
	}
	public TextSource charset(Charset charset) {
		this.charset=charset;
		return this;
	}
	public TextSource charset(String charsetName) {
		this.charset=Charset.forName(charsetName);
		return this;
	}
	
	public Input<String> readLines(){
		return new LineInput(this);
	};
	public Input<String[]> readCsv(){
		return new CsvInput(this);
	};
}
