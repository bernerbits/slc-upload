package net.bernerbits.avolve.slcupload.dataimport.model;

public class SpreadsheetRow {

	private final String[] values;

	public SpreadsheetRow(String... values) {
		this.values = values;
	}

	public String[] getValues() {
		return values;
	}

	public int find(String string) {
		int col = 0;
		for (String value : values) {
			if (value != null && value.toLowerCase().replaceAll("[^a-z0-9]", "").equals(string.toLowerCase())) {
				return col;
			}
			col++;
		}
		return -1;
	}

}
