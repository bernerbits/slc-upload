package net.bernerbits.avolve.slcupload.ui.util;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;

import com.google.common.base.Function;

public class ClosureColumnLabelProvider<T> extends ColumnLabelProvider {
	private final Function<T, String> labelFunction;
	
	private final Function<T, Color> foregroundFunction;
	
	public ClosureColumnLabelProvider(Function<T, String> labelFunction) {
		this.labelFunction = labelFunction;
		this.foregroundFunction = null;
	}	
	
	public ClosureColumnLabelProvider(Function<T, String> labelFunction, Function<T, Color> foregroundFunction) {
		this.labelFunction = labelFunction;
		this.foregroundFunction = foregroundFunction;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getText(Object element) {
		return labelFunction.apply((T) element);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Color getForeground(Object element) {
		if (foregroundFunction == null) {
			return super.getForeground(element);
		}
		else {
			return foregroundFunction.apply((T) element);
		}
	}
}
