package net.bernerbits.avolve.slcupload.exception;

public class FileTransferMissingFileException extends FileTransferException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8965142297857127066L;

	private String path;
	
	public FileTransferMissingFileException(String path) {
		super("File does not exist.");
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
