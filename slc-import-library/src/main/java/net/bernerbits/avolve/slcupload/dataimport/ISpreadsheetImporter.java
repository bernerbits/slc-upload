package net.bernerbits.avolve.slcupload.dataimport;

import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetImportException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;

public interface ISpreadsheetImporter {

	Iterable<SpreadsheetRow> importSpreadsheet(String fileName) throws SpreadsheetImportException;

}
