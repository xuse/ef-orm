package jef.tools.csvreader;

public interface Codec<T> {
	String toString(T t);

	T fromString(String s);
}
