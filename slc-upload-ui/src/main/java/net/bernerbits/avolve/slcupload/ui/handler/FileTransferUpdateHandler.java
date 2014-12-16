package net.bernerbits.avolve.slcupload.ui.handler;

import net.bernerbits.avolve.slcupload.FileTransfer;

import org.eclipse.jdt.annotation.NonNull;

public interface FileTransferUpdateHandler {
	void notifyFileTransfer(@SuppressWarnings("null") @NonNull FileTransfer transfer);
}
