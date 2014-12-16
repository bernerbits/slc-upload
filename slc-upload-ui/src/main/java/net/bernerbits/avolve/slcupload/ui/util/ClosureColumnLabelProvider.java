package net.bernerbits.avolve.slcupload.ui.util;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.base.Function;

public class ClosureColumnLabelProvider<T> extends ColumnLabelProvider {
	private final Function<T, String> labelFunction;

	private @Nullable final Function<T, Color> foregroundFunction;

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
	public String getText(@Nullable Object element) {
		return labelFunction.apply((T) element);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @NonNull Color getForeground(@Nullable Object element) {
		if (foregroundFunction != null) {
			return foregroundFunction.apply((T) element);
		} else {
			@Nullable
			Color defaultForeground = super.getForeground(element);
			return defaultForeground != null ? defaultForeground : SWTResourceManager.getColor(new RGB(0, 0, 0));
		}
	}
}
