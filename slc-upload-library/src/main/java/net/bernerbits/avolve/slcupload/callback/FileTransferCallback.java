package net.bernerbits.avolve.slcupload.callback;

import net.bernerbits.avolve.slcupload.FileTransfer;

import org.eclipse.jdt.annotation.NonNull;

public interface FileTransferCallback {
	void onFileTransfer(@SuppressWarnings("null") @NonNull FileTransfer transfer);
}
