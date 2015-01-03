package net.bernerbits.avolve.slcupload.dataimport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetFileNotFoundException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetImportException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.log4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class CsvSpreadsheetImporter implements ISpreadsheetImporter {

	private static Logger logger = Logger.getLogger(CsvSpreadsheetImporter.class);

	@Override
	public Iterable<SpreadsheetRow> importSpreadsheet(String fileName) throws SpreadsheetImportException {
		logger.debug("Importing CSV file: " + fileName);
		try {
			CSVParser parser = CSVParser.parse(Paths.get(fileName).toUri().toURL(), StandardCharsets.UTF_8,
					CSVFormat.EXCEL.withIgnoreEmptyLines(true));

			return Lists.newArrayList(Iterables.transform(parser,
					(input) -> new SpreadsheetRow(Iterables.toArray(input, String.class))));
		} catch (FileNotFoundException e) {
			throw new SpreadsheetFileNotFoundException(e.getMessage());
		} catch (IOException e) {
			throw new Error("Unexpected IO error when reading file: " + fileName, e);
		}
	}

}
