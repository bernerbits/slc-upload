package net.bernerbits.avolve.slcupload;

import java.io.File;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public class FileTransferUtil {
	public synchronized static File getLatestFile(String folderSource, FileTransferObject transferObject)
			throws FileTransferException {
		String sourcePath = transferObject.getSourcePath();
		sourcePath = sourcePath.replace('\\', '/');
		int projectIdLocation = sourcePath.indexOf("/" + transferObject.getProjectId() + "/");
		if (projectIdLocation == -1) {
			throw new FileTransferException("Project Id (" + transferObject.getProjectId() + ") is not part of the file path.");
		}
		sourcePath = sourcePath.substring(projectIdLocation).replace('/', File.separatorChar);

		File firstVersion = new File(folderSource + sourcePath);

		// Sourcepath is for a versioned file
		if (firstVersion.getParentFile().getName().equals(transferObject.getFileName())
				&& firstVersion.getName().startsWith(transferObject.getFileName())) {
			firstVersion = firstVersion.getParentFile();
		}

		File[] versions = firstVersion.listFiles((f, n) -> n.startsWith(transferObject.getFileName())
				&& n.matches(".*_V\\d+"));

		int latestVersion = 1;
		File latestFile = firstVersion;
		if (versions != null) {
			for (File fileVersion : versions) {
				int version = Integer.parseInt(fileVersion.getName().substring(
						fileVersion.getName().lastIndexOf("_V") + 2));
				if (version > latestVersion) {
					latestVersion = version;
					latestFile = fileVersion;
				}
			}
		}

		return new File(latestFile, transferObject.getFileName());
	}

	public static String getRemotePath(String folderSource, FileTransferObject transferObject)
			throws FileTransferException {
		File f = getLatestFile(folderSource, transferObject);
		String path = f.getAbsolutePath().replace('\\', '/');
		path = path.substring(path.indexOf("/" + transferObject.getProjectId() + "/") + 1);
		return path;
	}
}
