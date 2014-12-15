package net.bernerbits.avolve.slcupload.ui.model;

import org.eclipse.swt.graphics.Color;

public class ColoredText {
	private final Color color;
	private final String text;
	
	public ColoredText(String text, Color color) {
		this.color = color;
		this.text = text;
	}

	public Color getColor() {
		return color;
	}

	public String getText() {
		return text;
	}
	
}
