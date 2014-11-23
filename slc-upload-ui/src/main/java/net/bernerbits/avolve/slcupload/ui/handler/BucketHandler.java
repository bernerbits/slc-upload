package net.bernerbits.avolve.slcupload.ui.handler;

import java.util.List;

import net.bernerbits.avolve.slcupload.model.RemoteFolder;

public interface BucketHandler {
	public void bucketsLoaded(List<RemoteFolder> folders);
}
