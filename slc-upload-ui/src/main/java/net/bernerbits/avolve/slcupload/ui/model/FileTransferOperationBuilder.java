package net.bernerbits.avolve.slcupload.ui.model;

import java.util.List;

import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;

import org.apache.log4j.Logger;
import org.eclipse.jdt.annotation.Nullable;

public class FileTransferOperationBuilder {

	private static Logger logger = Logger.getLogger(FileTransferOperationBuilder.class);

	private @Nullable String folderSource;

	private @Nullable RemoteFolder s3Destination;
	private @Nullable String folderDestination;

	private @Nullable Iterable<SpreadsheetRow> rows;

	private @Nullable List<FileTransferObject> convertedRows;

	public @Nullable String getFolderSource() {
		return folderSource;
	}

	public void setFolderSource(@Nullable String folderSource) {
		logger.debug("Folder Source changed: " + folderSource);
		this.folderSource = folderSource;
	}

	public @Nullable RemoteFolder getS3Destination() {
		return s3Destination;
	}

	public void setS3Destination(@Nullable RemoteFolder s3Destination) {
		logger.debug("Destination changed (s3): " + s3Destination);
		this.s3Destination = s3Destination;
		folderDestination = null;
	}

	public @Nullable String getFolderDestination() {
		return folderDestination;
	}

	public void setFolderDestination(@Nullable String folderDestination) {
		logger.debug("Destination changed (folder): " + folderDestination);
		this.folderDestination = folderDestination;
		s3Destination = null;
	}

	public @Nullable Iterable<SpreadsheetRow> getRows() {
		return rows;
	}

	public void setRows(@Nullable Iterable<SpreadsheetRow> rows) {
		logger.debug("Spreadsheet rows loaded");
		this.rows = rows;
		this.convertedRows = null;
	}

	public @Nullable List<FileTransferObject> getConvertedRows() {
		return convertedRows;
	}

	public void setConvertedRows(@Nullable List<FileTransferObject> convertedRows) {
		logger.debug("Spreadsheet rows converted");
		this.convertedRows = convertedRows;
	}

	public FileTransferOperation build() {
		logger.debug("Building file transfer operation");
		if (folderSource != null) {
			String folderSource = this.folderSource;
			if (convertedRows != null) {
				List<FileTransferObject> convertedRows = this.convertedRows;
				if (folderDestination != null) {
					return new FileSystemFileTransferOperation(folderSource, convertedRows, folderDestination);
				} else if (s3Destination != null) {
					return new S3FileTransferOperation(folderSource, convertedRows, s3Destination);
				} else {
					logger.warn("File transfer operation failed - destination not set");
					throw new IllegalStateException("Cannot build file transfer operation: destination is null");
				}
			} else {
				logger.warn("File transfer operation failed - converted rows not set");
				throw new IllegalStateException("Cannot build file transfer operation: converted rows is null");
			}
		} else {
			logger.warn("File transfer operation failed - source not set");
			throw new IllegalStateException("Cannot build file transfer operation: source is null");
		}
	}

}
