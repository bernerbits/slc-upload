package net.bernerbits.avolve.slcupload.ui;

import java.util.function.Supplier;

import net.bernerbits.util.ErrorReporting;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SLCUploadUI {

	public static void main(String[] args) {
		PropertyConfigurator.configure(SLCUploadUI.class.getResourceAsStream("/log4j.properties"));

		Logger logger = Logger.getLogger(SLCUploadUI.class);
		logger.info("Application started");

		Display display = new Display();
		Shell shell = new SLCUploadShell(display);

		shell.open();

		Thread.setDefaultUncaughtExceptionHandler((th, t) -> handleUncaughtError(display, shell, t, th));

		logger.debug("Starting the event loop.");
		while (!tc(() -> shell.isDisposed(), display, shell)) {
			if (tc(() -> display.readAndDispatch(), display, shell)) {
				tc(() -> display.sleep(), display, shell);
			}
		}

		display.dispose();
	}

	private static boolean tc(Supplier<Boolean> func, Display display, Shell shell) {
		try {
			return func.get();
		} catch (Throwable t) {
			handleUncaughtError(display, shell, t, Thread.currentThread());
			return false;
		}
	}

	private static void handleUncaughtError(Display display, Shell shell, Throwable t, Thread th) {
		display.asyncExec(() -> ErrorDialog.openError(shell, SLCUploadShell.APP_DISPLAY_NAME,
				"An unexpected error occurred.",
				new Status(IStatus.ERROR, SLCUploadShell.APP_DISPLAY_NAME, t.getMessage(), t.getCause())));

		ErrorReporting.report(t, "Uncaught", th.getName());
	}
}
