package net.bernerbits.avolve.slcupload.ui.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Color;

public class ColoredText {
	private final Color color;
	private final String text;
	
	public ColoredText(@Nullable String text, @Nullable Color color) {
		if (text != null && color != null) {
			this.color = color;
			this.text = text;
		} else {
			throw new IllegalArgumentException("Text or color is null");
		}
	}

	public Color getColor() {
		return color;
	}

	public String getText() {
		return text;
	}
	
}
