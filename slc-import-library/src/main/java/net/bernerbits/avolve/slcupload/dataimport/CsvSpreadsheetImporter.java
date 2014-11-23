package net.bernerbits.avolve.slcupload.dataimport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetFileNotFoundException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetImportException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import com.google.common.collect.Iterables;

public class CsvSpreadsheetImporter implements ISpreadsheetImporter {

	@Override
	public Iterable<SpreadsheetRow> importSpreadsheet(String fileName) throws SpreadsheetImportException {
		try {
			CSVParser parser = CSVParser.parse(new File(fileName), Charset.forName("UTF-8"), CSVFormat.EXCEL);
			return Iterables.transform(parser, (input) -> {
				List<String> rowValues = new ArrayList<>();
				for (int i = 0; i < input.size(); i++) {
					rowValues.add(input.get(i));
				}
				return new SpreadsheetRow(rowValues.toArray(new String[0]));
			});
		} catch (FileNotFoundException e) {
			throw new SpreadsheetFileNotFoundException(e.getMessage());
		} catch (IOException e) {
			throw new Error("Unexpected IO error when reading file: " + fileName, e);
		}
	}

}
