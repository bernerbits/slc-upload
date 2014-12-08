package net.bernerbits.avolve.slcupload.dataexport;

import java.util.function.Function;

public class ColumnDefinition<T> {
	private final String header;
	private final Function<T, String> contentFunction;
	
	public ColumnDefinition(String header, Function<T, String> contentFunction) {
		this.header = header;
		this.contentFunction = contentFunction;
	}
	
	public String getHeader() {
		return header;
	}
	
	public String getContents(T element) {
		return contentFunction.apply(element);
	}
}
