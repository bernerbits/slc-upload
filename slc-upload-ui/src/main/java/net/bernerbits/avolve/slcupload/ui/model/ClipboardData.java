package net.bernerbits.avolve.slcupload.ui.model;

import org.eclipse.swt.dnd.Transfer;

public class ClipboardData {
	private final Object[] data;
	private final Transfer[] transfers;

	public ClipboardData(Object[] data, Transfer[] transfers) {
		this.data = data;
		this.transfers = transfers;
	}

	public Object[] getData() {
		return data;
	}

	public Transfer[] getTransfers() {
		return transfers;
	}

}
