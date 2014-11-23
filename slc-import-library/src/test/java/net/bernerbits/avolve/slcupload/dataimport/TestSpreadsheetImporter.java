package net.bernerbits.avolve.slcupload.dataimport;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import net.bernerbits.avolve.slcupload.dataimport.exception.FileExtensionNotRecognizedException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class TestSpreadsheetImporter {

	private static File csvSheetFile;
	private static File oldExcelFile;
	private static File newExcelFile;
	private static File textFile;
	
	@Mock
	private ISpreadsheetImporter oldExcelImporter;
	@Mock
	private ISpreadsheetImporter newExcelImporter;
	@Mock
	private ISpreadsheetImporter csvImporter;

	private SpreadsheetImporter importer;

	@BeforeClass
	public static void createTempFiles() throws IOException
	{
		newExcelFile = File.createTempFile("test.", ".xlsx");
		oldExcelFile = File.createTempFile("test.", ".xls");
		csvSheetFile = File.createTempFile("test.", ".csv");
		textFile = File.createTempFile("test.", ".txt");
		
		newExcelFile.deleteOnExit();
		oldExcelFile.deleteOnExit();
		csvSheetFile.deleteOnExit();
		textFile.deleteOnExit();
	}
	
	@Before
	public void setUp() {
		importer = new SpreadsheetImporter();

		importer.setImportersByExtension(ImmutableMap.<String, ISpreadsheetImporter> builder()
				.put(".xls", oldExcelImporter).put(".xlsx", newExcelImporter).put(".csv", csvImporter).build());
	}

	@Test(expected = FileExtensionNotRecognizedException.class)
	public void importThrowsExceptionIfExtensionNotRecognized() throws Exception {
		importer.importSpreadsheet(textFile.getCanonicalPath());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void importDelegatesToCorrectImporter() throws Exception {
		Iterable<SpreadsheetRow> newExcel = mock(Iterable.class);
		Iterable<SpreadsheetRow> oldExcel = mock(Iterable.class);
		Iterable<SpreadsheetRow> csvSheet = mock(Iterable.class);

		when(newExcelImporter.importSpreadsheet(newExcelFile.getCanonicalPath())).thenReturn(newExcel);
		when(oldExcelImporter.importSpreadsheet(oldExcelFile.getCanonicalPath())).thenReturn(oldExcel);
		when(csvImporter.importSpreadsheet(csvSheetFile.getCanonicalPath())).thenReturn(csvSheet);

		Iterable<SpreadsheetRow> newExcelResult = importer.importSpreadsheet(newExcelFile.getCanonicalPath());
		Iterable<SpreadsheetRow> oldExcelResult = importer.importSpreadsheet(oldExcelFile.getCanonicalPath());
		Iterable<SpreadsheetRow> csvSheetResult = importer.importSpreadsheet(csvSheetFile.getCanonicalPath());

		verify(newExcelImporter).importSpreadsheet(newExcelFile.getCanonicalPath());
		verify(oldExcelImporter).importSpreadsheet(oldExcelFile.getCanonicalPath());
		verify(csvImporter).importSpreadsheet(csvSheetFile.getCanonicalPath());

		assertEquals(newExcel, newExcelResult);
		assertEquals(oldExcel, oldExcelResult);
		assertEquals(csvSheet, csvSheetResult);
	}
}
