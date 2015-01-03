package net.bernerbits.avolve.slcupload.dataexport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.bernerbits.avolve.slcupload.util.function.NullSafe;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.log4j.Logger;
import org.eclipse.jdt.annotation.NonNull;

public class CSVExporter<T> {
	private static Logger logger = Logger.getLogger(CSVExporter.class);

	@SafeVarargs
	public final void export(Path path, List<@NonNull T> rows, ColumnDefinition<T>... columns)
			throws FileExportException {
		try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
				CSVPrinter printer = new CSVPrinter(bw, CSVFormat.EXCEL.withQuoteMode(QuoteMode.MINIMAL))) {
			printer.printRecord(Arrays.stream(columns).map(ColumnDefinition::getHeader)
					.collect(NullSafe.toListCollector()));
			printer.printRecords(rows.stream()
					.map((t) -> Arrays.stream(columns).map((c) -> c.getContents(t)).collect(Collectors.toList()))
					.collect(Collectors.toList()));
		} catch (FileSystemException e) {
			logger.warn("Could not write CSV file " + path, e);
			throw new FileExportException(e.getReason(), e);
		} catch (IOException e) {
			logger.warn("Could not write CSV file " + path, e);
			throw new FileExportException(e.getMessage(), e);
		}
	}
}
