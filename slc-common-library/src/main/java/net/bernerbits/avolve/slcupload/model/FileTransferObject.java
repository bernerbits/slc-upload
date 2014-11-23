package net.bernerbits.avolve.slcupload.model;

public class FileTransferObject {

	private String projectId;
	private String sourcePath;
	private String fileName;

	public FileTransferObject(String projectId, String sourcePath, String fileName) {
		this.projectId = projectId;
		this.sourcePath = sourcePath;
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}
	
	public String getProjectId() {
		return projectId;
	}
	
	public String getSourcePath() {
		return sourcePath;
	}
}
