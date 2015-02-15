package net.bernerbits.avolve.slcupload.model;

import net.bernerbits.avolve.slcupload.state.TaskDescriptor;

import org.apache.log4j.Logger;

public class FileTransferObject implements TaskDescriptor {

	private static Logger logger = Logger.getLogger(FileTransferObject.class);

	private String projectId;
	private String sourcePath;
	private String fileName;

	public FileTransferObject(String projectId, String sourcePath, String fileName) {
		if (logger.isTraceEnabled()) {
			logger.trace("New FTO: Project ID=" + projectId + " Source Path=" + sourcePath + " File Name=" + fileName);
		}
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
