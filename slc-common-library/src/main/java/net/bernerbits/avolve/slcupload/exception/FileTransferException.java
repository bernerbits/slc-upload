package net.bernerbits.avolve.slcupload.exception;

public class FileTransferException extends Exception {

	private static final long serialVersionUID = -8924901181540566959L;

	public FileTransferException() {
		super();
	}

	public FileTransferException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileTransferException(String message) {
		super(message);
	}

	public FileTransferException(Throwable cause) {
		super(cause);
	}

}
