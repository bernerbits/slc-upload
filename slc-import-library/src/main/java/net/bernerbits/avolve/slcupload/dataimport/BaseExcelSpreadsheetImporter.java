package net.bernerbits.avolve.slcupload.dataimport;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetFileNotFoundException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetImportException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.jdt.annotation.Nullable;

public abstract class BaseExcelSpreadsheetImporter implements ISpreadsheetImporter {

	private static Logger logger = Logger.getLogger(BaseExcelSpreadsheetImporter.class);

	public BaseExcelSpreadsheetImporter() {
		super();
	}

	@Override
	public List<SpreadsheetRow> importSpreadsheet(String fileName) throws SpreadsheetImportException {
		logger.debug("Importing " + getDebugDescription() + " spreadsheet: " + fileName);
		try (InputStream ssf = new BufferedInputStream(new FileInputStream(fileName))) {
			Workbook wb = openSpreadsheet(ssf);

			if (wb.getNumberOfSheets() > 1 && logger.isEnabledFor(Level.WARN)) {
				logger.warn("Workbook has multiple sheets. This application only supports single-sheet workbooks."
						+ "Defaulting to the first sheet named " + wb.getSheetAt(0).getSheetName());
			}

			DataFormatter df = new DataFormatter();
			FormulaEvaluator eval = getFormulaEvaluator(wb);
			Sheet sheet = wb.getSheetAt(0);
			List<SpreadsheetRow> rows = new ArrayList<SpreadsheetRow>();
			for (Row currentRow : sheet) {
				List<@Nullable String> rowValues = new ArrayList<>();
				for (Cell cell : currentRow) {
					while (rowValues.size() - 1 < cell.getColumnIndex()) {
						rowValues.add(null);
					}
					rowValues.add(cell.getColumnIndex(), df.formatCellValue(cell, eval));
				}
				SpreadsheetRow element = new SpreadsheetRow(rowValues.toArray(new String[] {}));
				rows.add(element);
			}
			return rows;
		} catch (FileNotFoundException e) {
			throw new SpreadsheetFileNotFoundException(e.getMessage());
		} catch (IOException e) {
			throw new Error("Unexpected IO error when reading file: " + fileName, e);
		} finally {
			System.gc();
		}
	}

	protected abstract FormulaEvaluator getFormulaEvaluator(Workbook wb) throws IOException;

	protected abstract Workbook openSpreadsheet(InputStream inputStream) throws IOException;

	protected abstract String getDebugDescription();

}