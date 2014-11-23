package net.bernerbits.avolve.slcupload;

import java.io.File;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public class FileTransferUtil {
	private static <T> T echo(String name, T object) {
		System.out.println(name + "=" + object);
		return object;
	}

	public synchronized static File getLatestFile(FileTransferObject transferObject) {
		String sourcePath = transferObject.getSourcePath();

		File firstVersion = new File(sourcePath);

		if (firstVersion.getParentFile() == null) {
			System.out.println(firstVersion.getAbsolutePath());
		}

		File[] versions = firstVersion.listFiles((f, n) -> n.matches(transferObject.getFileName() + "_V\\d+"));

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

	public static String getRemotePath(FileTransferObject transferObject) {
		File f = getLatestFile(transferObject);
		String path = f.getAbsolutePath().replace('\\', '/');
		path = path.substring(path.indexOf("/" + transferObject.getProjectId() + "/") + 1);
		return path;
	}
}
