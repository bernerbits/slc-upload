package net.bernerbits.avolve.slcupload.dataimport.exception;

public class SpreadsheetFileNotFoundException extends SpreadsheetImportException {

	private static final long serialVersionUID = 129555689653905751L;
	private final String fileName;

	public SpreadsheetFileNotFoundException(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}
}
