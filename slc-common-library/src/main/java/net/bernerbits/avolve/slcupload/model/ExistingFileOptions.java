package net.bernerbits.avolve.slcupload.model;

public class ExistingFileOptions {
	private final boolean skip;
	private final boolean remember;

	public ExistingFileOptions(boolean skip, boolean remember) {
		this.skip = skip;
		this.remember = remember;
	}

	public boolean isSkip() {
		return skip;
	}

	public boolean isRemember() {
		return remember;
	}

}
