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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;

public class SpreadsheetImporter {

	private static Logger logger = Logger.getLogger(SpreadsheetImporter.class);

	private Map<String, ISpreadsheetImporter> importers = new HashMap<String, ISpreadsheetImporter>();

	public SpreadsheetImporter() {
		importers.put(".csv", new CsvSpreadsheetImporter());
		importers.put(".xls", new OldExcelSpreadsheetImporter());
		importers.put(".xlsx", new ExcelSpreadsheetImporter());
	}

	public Iterable<SpreadsheetRow> importSpreadsheet(String inputFile) throws SpreadsheetImportException {
		logger.debug("Importing spreadsheet: " + inputFile);
		if (!new File(inputFile).exists()) {
			logger.warn("Spreadsheet file not found: " + inputFile);
			throw new SpreadsheetFileNotFoundException(inputFile);
		}
		if (inputFile.contains(".")) {
			String extension = inputFile.substring(inputFile.lastIndexOf('.'));
			logger.debug("Spreadsheet extension: " + extension);
			ISpreadsheetImporter delegate = importers.get(extension.toLowerCase());
			if (delegate != null) {
				logger.debug("Delegating spreadsheet load: " + extension);
				return delegate.importSpreadsheet(inputFile);
			} else {
				logger.warn("No delegate found for file extension: " + extension);
				throw new FileExtensionNotRecognizedException(extension);
			}
		} else {
			logger.warn("Bad or no file extension: " + inputFile);
			throw new FileExtensionNotRecognizedException("");
		}
	}

	public void setImportersByExtension(Map<String, ISpreadsheetImporter> importers) {
		this.importers = importers;
	}

	public List<FileTransferObject> convertRows(Iterable<SpreadsheetRow> rows, ErrorHandler errorHandler)
			throws SpreadsheetImportException {
		logger.debug("Converting spreadsheet rows to pending transfers");
		Iterator<SpreadsheetRow> it = rows.iterator();
		if (!it.hasNext()) {
			return Collections.<FileTransferObject> emptyList();
		}

		SpreadsheetRow headerRow = it.next();

		int projectIdCol = headerRow.find("projectid");
		int sourcePathCol = headerRow.find("sourcepath");
		int fileNameCol = headerRow.find("filename");

		if (projectIdCol == -1 || sourcePathCol == -1 || fileNameCol == -1) {
			logger.warn("Could not convert spreadsheet - format is invalid");
			throw new InvalidFormatSpreadsheetException();
		}

		List<FileTransferObject> transferObjects = new ArrayList<>();
		
		while (it.hasNext()) {
			SpreadsheetRow row = it.next();
			try {
				if(rowHasColumns(row, projectIdCol, sourcePathCol, fileNameCol)) {
					String projectId = row.getValues()[projectIdCol];
					String sourcePath = row.getValues()[sourcePathCol];
					String fileName = row.getValues()[fileNameCol];
		
					if (!Strings.isNullOrEmpty(projectId) && !Strings.isNullOrEmpty(sourcePath)
							&& !Strings.isNullOrEmpty(fileName)) {
						transferObjects.add(new FileTransferObject(projectId, sourcePath, fileName));
					} else if (!fileName.isEmpty()) {
						logger.warn("Failed to convert row - projectID or sourcePath missing: " + StringUtils.join(row, "|"));
						errorHandler.handleError("Missing projectID or sourcePath", fileName);
					} else {
						logger.warn("Failed to convert row - fileName missing: " + StringUtils.join(row, "|"));
						errorHandler.handleError("Incomplete row", Arrays.toString(row.getValues()));
					}
				} else {
					logger.warn("Failed to convert row - not enough columns (" + row.getValues().length + ", expecting " + headerRow.getValues().length + "):" + StringUtils.join(row, "|"));
					errorHandler.handleError("Row is too short - " + row.getValues().length + " columns, expecting " + headerRow.getValues().length, Arrays.toString(row.getValues()));
				}
			} catch(RuntimeException e) {
				logger.warn("Failed to convert row - unexpected error: " + StringUtils.join(row, "|"), e);
				errorHandler.handleError("Unexpected error: " + e.toString(), Arrays.toString(row.getValues()));
			}
		}
		return transferObjects;
	}

	private static boolean rowHasColumns(SpreadsheetRow row, int... colIndexes) {
		int maxIndex = Arrays.stream(colIndexes).max().orElse(-1);
		if (maxIndex < row.getValues().length) {
			return true;
		} else {
			return false;
		}
	}

}
