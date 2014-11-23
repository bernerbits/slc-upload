package net.bernerbits.avolve.slcupload.ui.handler;

import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;

public interface SpreadsheetOpenedHandler {
	public void spreadsheetOpened(Iterable<SpreadsheetRow> rows);
}
