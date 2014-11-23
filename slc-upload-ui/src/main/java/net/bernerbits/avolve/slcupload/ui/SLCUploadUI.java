package net.bernerbits.avolve.slcupload.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SLCUploadUI {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new SLCUploadShell(display);

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}
}
