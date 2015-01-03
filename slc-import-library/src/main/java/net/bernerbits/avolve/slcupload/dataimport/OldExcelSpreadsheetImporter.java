package net.bernerbits.avolve.slcupload.dataimport;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

public class OldExcelSpreadsheetImporter extends BaseExcelSpreadsheetImporter {

	@Override
	protected Workbook openSpreadsheet(InputStream inputStream) throws IOException {
		return new HSSFWorkbook(inputStream);
	}

	@Override
	protected FormulaEvaluator getFormulaEvaluator(Workbook wb) throws IOException {
		return new HSSFFormulaEvaluator((HSSFWorkbook) wb);
	}

	@Override
	protected String getDebugDescription() {
		return "MS Excel Legacy";
	}
}
