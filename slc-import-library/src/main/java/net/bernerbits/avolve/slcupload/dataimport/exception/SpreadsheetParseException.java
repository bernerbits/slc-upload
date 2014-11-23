package net.bernerbits.avolve.slcupload.dataimport.exception;

import java.text.ParseException;

public class SpreadsheetParseException extends SpreadsheetImportException {

	private static final long serialVersionUID = -2501451238671642754L;

	public SpreadsheetParseException(ParseException cause) {
		super(cause);
	}

}
