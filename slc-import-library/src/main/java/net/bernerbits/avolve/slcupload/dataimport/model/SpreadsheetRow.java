package net.bernerbits.avolve.slcupload.dataimport.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class SpreadsheetRow {

	private static Logger logger = Logger.getLogger(SpreadsheetRow.class);

	private final String[] values;

	public SpreadsheetRow(String... values) {
		this.values = values;
		if (logger.isTraceEnabled()) {
			logger.trace("ROW: " + StringUtils.join(values, "|"));
		}
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
