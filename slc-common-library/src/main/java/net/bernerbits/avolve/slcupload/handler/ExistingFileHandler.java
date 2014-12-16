package net.bernerbits.avolve.slcupload.handler;

import java.nio.file.Path;

import net.bernerbits.avolve.slcupload.model.ExistingFileOptions;

public interface ExistingFileHandler {
	ExistingFileOptions getOptions(Path existingFile);
}
