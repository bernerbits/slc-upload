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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

public class CSVExporter<T> {
	@SafeVarargs
	public final void export(Path path, List<T> rows, ColumnDefinition<T>... columns) throws FileExportException {
		try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
				CSVPrinter printer = new CSVPrinter(bw, CSVFormat.EXCEL.withQuoteMode(QuoteMode.MINIMAL))) {
			printer.printRecord(Arrays.stream(columns).map(ColumnDefinition::getHeader).collect(Collectors.toList()));
			printer.printRecords(rows.stream()
					.map((t) -> Arrays.stream(columns)
							.map((c) -> c.getContents(t))
							.collect(Collectors.toList()))
					.collect(Collectors.toList()));
		} catch (FileSystemException e) {
			throw new FileExportException(e.getReason(), e);
		} catch (IOException e) {
			throw new FileExportException(e.getMessage(), e);
		}
	}
}
