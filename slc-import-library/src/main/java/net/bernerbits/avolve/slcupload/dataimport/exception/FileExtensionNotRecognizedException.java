package net.bernerbits.avolve.slcupload.dataimport.exception;

public class FileExtensionNotRecognizedException extends SpreadsheetImportException {

	private static final long serialVersionUID = 129555689653905751L;
	private final String extension;

	public FileExtensionNotRecognizedException(String extension) {
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}

}
