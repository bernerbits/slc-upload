package net.bernerbits.avolve.slcupload.dataimport.exception;

public abstract class SpreadsheetImportException extends Exception {

	private static final long serialVersionUID = 4791020862647918412L;

	public SpreadsheetImportException() {
		super();
	}

	public SpreadsheetImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public SpreadsheetImportException(String message) {
		super(message);
	}

	public SpreadsheetImportException(Throwable cause) {
		super(cause);
	}

}
