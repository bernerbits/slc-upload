package net.bernerbits.avolve.slcupload.callback;

import net.bernerbits.avolve.slcupload.FileTransferer;

public interface FileTransferStateChangeCallback {
	public void stateChange(FileTransferer.TransferState newState);
}
