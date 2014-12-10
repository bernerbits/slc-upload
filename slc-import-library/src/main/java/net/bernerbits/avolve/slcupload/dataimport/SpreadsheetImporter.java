package net.bernerbits.avolve.slcupload.dataimport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.bernerbits.avolve.slcupload.dataimport.exception.FileExtensionNotRecognizedException;
import net.bernerbits.avolve.slcupload.dataimport.exception.InvalidFormatSpreadsheetException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetFileNotFoundException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetImportException;
import net.bernerbits.avolve.slcupload.dataimport.handler.ErrorHandler;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import com.google.common.base.Strings;

public class SpreadsheetImporter {

	private Map<String, ISpreadsheetImporter> importers = new HashMap<String, ISpreadsheetImporter>();

	public SpreadsheetImporter() {
		importers.put(".csv", new CsvSpreadsheetImporter());
		importers.put(".xls", new OldExcelSpreadsheetImporter());
		importers.put(".xlsx", new ExcelSpreadsheetImporter());
	}

	public Iterable<SpreadsheetRow> importSpreadsheet(String inputFile) throws SpreadsheetImportException {
		if (!new File(inputFile).exists()) {
			throw new SpreadsheetFileNotFoundException(inputFile);
		}
		if (inputFile.contains(".")) {
			String extension = inputFile.substring(inputFile.lastIndexOf('.'));
			ISpreadsheetImporter delegate = importers.get(extension.toLowerCase());
			if (delegate != null) {
				return delegate.importSpreadsheet(inputFile);
			} else {
				throw new FileExtensionNotRecognizedException(extension);
			}
		} else {
			throw new FileExtensionNotRecognizedException("");
		}
	}

	public void setImportersByExtension(Map<String, ISpreadsheetImporter> importers) {
		this.importers = importers;
	}

	public List<FileTransferObject> convertRows(Iterable<SpreadsheetRow> rows, ErrorHandler errorHandler)
			throws SpreadsheetImportException {
		Iterator<SpreadsheetRow> it = rows.iterator();
		if (!it.hasNext()) {
			return Collections.<FileTransferObject> emptyList();
		}

		SpreadsheetRow headerRow = it.next();

		int projectIdCol = headerRow.find("projectid");
		int sourcePathCol = headerRow.find("sourcepath");
		int fileNameCol = headerRow.find("filename");

		if (projectIdCol == -1 || sourcePathCol == -1 || fileNameCol == -1) {
			throw new InvalidFormatSpreadsheetException();
		}

		List<FileTransferObject> transferObjects = new ArrayList<>();
		while (it.hasNext()) {
			SpreadsheetRow row = it.next();
			String projectId = row.getValues()[projectIdCol];
			String sourcePath = row.getValues()[sourcePathCol];
			String fileName = row.getValues()[fileNameCol];

			if (!Strings.isNullOrEmpty(projectId) && !Strings.isNullOrEmpty(sourcePath)
					&& !Strings.isNullOrEmpty(fileName)) {
				transferObjects.add(new FileTransferObject(projectId, sourcePath, fileName));
			} else if (!fileName.isEmpty()) {
				errorHandler.handleError("Missing projectID or sourcePath", fileName);
			} else {
				errorHandler.handleError("Incomplete row", Arrays.toString(row.getValues()));
			}
		}
		return transferObjects;
	}

}
