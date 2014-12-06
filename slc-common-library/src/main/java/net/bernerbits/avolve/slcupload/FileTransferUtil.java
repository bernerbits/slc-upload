package net.bernerbits.avolve.slcupload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.exception.FileTransferMissingFileException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import org.jdesktop.swingx.util.OS;

public class FileTransferUtil {
	public synchronized static Path getLatestFile(String folderSource, FileTransferObject transferObject)
			throws FileTransferException {
		String sourcePath = transferObject.getSourcePath();

		String invalidChars;
		if (OS.isWindows()) {
			invalidChars = ":*?\"<>|";
		} else if (OS.isMacOSX()) {
			invalidChars = ":";
		} else { // assume Unix/Linux
			invalidChars = "";
		}

		if (sourcePath.chars().anyMatch((c) -> (invalidChars.indexOf(c) >= 0) // OS-invalid
				|| (c < '\u0020') // ctrls
				|| (c > '\u007e' && c < '\u00a0') // ctrls
				|| Character.getType(c) == Character.OTHER_SYMBOL
		)) {
			throw new FileTransferException("Source path contains invalid characters.");
		}

		sourcePath = sourcePath.replace('\\', '/');
		int projectIdLocation = sourcePath.indexOf("/" + transferObject.getProjectId() + "/");
		if (projectIdLocation == -1) {
			throw new FileTransferException("Project Id (" + transferObject.getProjectId()
					+ ") is not part of the file path.");
		}
		sourcePath = sourcePath.substring(projectIdLocation).replace('/', File.separatorChar);

		Path firstVersion = Paths.get(folderSource + sourcePath);

		// Sourcepath is for a versioned file
		if (firstVersion.getParent().getFileName().toString().equals(transferObject.getFileName())
				&& firstVersion.getFileName().toString().startsWith(transferObject.getFileName())) {
			firstVersion = firstVersion.getParent();
		}

		List<Path> versions = StreamSupport
				.stream(firstVersion.spliterator(), true)
				.filter((f) -> f.getFileName().startsWith(transferObject.getFileName())
						&& f.getFileName().toString().matches(".*_V\\d+")).collect(Collectors.toList());

		int latestVersion = 1;
		Path latestFile = firstVersion;
		if (versions != null) {
			for (Path fileVersion : versions) {
				int version = Integer.parseInt(fileVersion.getFileName().toString()
						.substring(fileVersion.getFileName().toString().lastIndexOf("_V") + 2));
				if (version > latestVersion) {
					latestVersion = version;
					latestFile = fileVersion;
				}
			}
		}

		Path targetPath = latestFile.resolve(transferObject.getFileName()).normalize().toAbsolutePath();
		try {
			return targetPath.toRealPath();
		} catch (FileNotFoundException | NoSuchFileException e) {
			throw new FileTransferMissingFileException(targetPath.toString());
		} catch (IOException e) {
			throw new FileTransferException(e.getMessage(), e);
		}
	}

	public static String getRemotePath(String folderSource, FileTransferObject transferObject)
			throws FileTransferException {
		Path f = getLatestFile(folderSource, transferObject);
		String path = f.toString().replace('\\', '/');
		path = path.substring(path.indexOf("/" + transferObject.getProjectId() + "/") + 1);
		return path;
	}

}
