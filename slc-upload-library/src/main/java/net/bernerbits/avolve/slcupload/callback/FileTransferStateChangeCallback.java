package net.bernerbits.avolve.slcupload.callback;

import net.bernerbits.avolve.slcupload.state.ExecutionState;

public interface FileTransferStateChangeCallback {
	public void stateChange(ExecutionState newState);
}
