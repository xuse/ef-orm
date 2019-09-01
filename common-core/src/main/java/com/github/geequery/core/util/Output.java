package com.github.geequery.core.util;

import java.util.function.Function;

public abstract class Output<T, A> {
	private Input<A> input;

	private Function<A, T> call;

	Output(Input<A> input){
		this.input=input;
	}
	
	static class StringOut<A> extends Output<String, A> {

		StringOut(Input<A> input) {
			super(input);
			// TODO Auto-generated constructor stub
		}

	}

	static class CsvOut<A> extends Output<String[], A> {

		CsvOut(Input<A> input) {
			super(input);
			// TODO Auto-generated constructor stub
		}

	}
	
	static class NoOut<A> extends Output<Void, A> {

		NoOut(Input<A> input) {
			super(input);
			// TODO Auto-generated constructor stub
		}

	}

	public Output<T, A> line(Function<A, T> call) {
		this.call = call;
		return this;
	}

}
