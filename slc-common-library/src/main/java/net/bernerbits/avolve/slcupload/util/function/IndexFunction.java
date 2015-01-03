package net.bernerbits.avolve.slcupload.util.function;

import java.util.function.ToIntFunction;

public class IndexFunction<T> implements ToIntFunction<T> {

	public static <T> IndexFunction<T> function() {
		return new IndexFunction<T>();
	}

	private int index = 0;

	@Override
	public int applyAsInt(T value) {
		return index++;
	}

}
