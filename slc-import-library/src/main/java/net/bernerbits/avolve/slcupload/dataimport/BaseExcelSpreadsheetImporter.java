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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public abstract class BaseExcelSpreadsheetImporter implements ISpreadsheetImporter {

	public BaseExcelSpreadsheetImporter() {
		super();
	}

	@Override
	public List<SpreadsheetRow> importSpreadsheet(String fileName) throws SpreadsheetImportException {
		try (InputStream ssf = new BufferedInputStream(new FileInputStream(fileName))) {
			Workbook wb = openSpreadsheet(ssf);
			DataFormatter df = new DataFormatter();
			FormulaEvaluator eval = getFormulaEvaluator(wb);
			Sheet sheet = wb.getSheetAt(0);
			List<SpreadsheetRow> rows = new ArrayList<SpreadsheetRow>();
			for (Row currentRow : sheet) {
				List<String> rowValues = new ArrayList<>();
				for (Cell cell : currentRow) {
					while(rowValues.size()-1 < cell.getColumnIndex())
					{
						rowValues.add(null);
					}
					rowValues.add(cell.getColumnIndex(), df.formatCellValue(cell, eval));
				}
				SpreadsheetRow element = new SpreadsheetRow(rowValues.toArray(new String[0]));
				if (element != null) {
					rows.add(element);
				}
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

}