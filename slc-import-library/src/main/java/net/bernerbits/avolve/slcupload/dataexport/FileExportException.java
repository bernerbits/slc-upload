package net.bernerbits.avolve.slcupload.dataexport;

import org.eclipse.jdt.annotation.Nullable;

public class FileExportException extends Exception {

	private static final long serialVersionUID = -7594478323480876316L;

	public FileExportException(@Nullable String message, Throwable cause) {
		super(message, cause);
	}

}
