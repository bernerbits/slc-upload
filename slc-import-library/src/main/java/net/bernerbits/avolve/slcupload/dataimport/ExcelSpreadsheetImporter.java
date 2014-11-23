package net.bernerbits.avolve.slcupload.dataimport;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelSpreadsheetImporter extends BaseExcelSpreadsheetImporter {

	protected Workbook openSpreadsheet(InputStream inputStream) throws IOException {
		return new XSSFWorkbook(inputStream);
	}

	protected FormulaEvaluator getFormulaEvaluator(Workbook wb) throws IOException {
		return new XSSFFormulaEvaluator((XSSFWorkbook) wb);
	}
}
