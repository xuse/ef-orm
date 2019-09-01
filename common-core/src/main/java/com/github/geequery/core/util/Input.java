package com.github.geequery.core.util;

import com.github.geequery.core.util.Output.CsvOut;
import com.github.geequery.core.util.Output.NoOut;
import com.github.geequery.core.util.Output.StringOut;

public abstract class Input<T> {
	private TextSource source;

	Input(TextSource source) {
		this.source = source;
	}

	static class LineInput extends Input<String> {

		LineInput(TextSource source) {
			super(source);
		}

	}

	static class CsvInput extends Input<String[]> {

		CsvInput(TextSource source) {
			super(source);
		}

	}

	
	public Output<String,T> outputString() {
		return new StringOut<T>(this);
	}
	
	public Output<String[],T> outputCsv(){
		return new CsvOut<T>(this);
	}
	
	public Output<Void,T> noOutput(){
		return new NoOut<T>(this);
	}
}
