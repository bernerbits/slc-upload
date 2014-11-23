package net.bernerbits.avolve.slcupload.model;

import java.util.List;

public abstract class RemoteFolder {
	public abstract String getPath();
	public abstract String getName();
	public abstract List<RemoteFolder> getChildren();
}
