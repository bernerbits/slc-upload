package net.bernerbits.avolve.slcupload;

import java.nio.file.Files;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.exception.FileTransferMissingFileException;
import net.bernerbits.avolve.slcupload.handler.ExistingFileHandler;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public class S3FileTransfer extends RealFileTransfer {

	private static Logger logger = Logger.getLogger(S3FileTransfer.class);

	private static ThreadLocal<AWSCredentials> creds = new ThreadLocal<>();
	private static ThreadLocal<AmazonS3Client> client = new ThreadLocal<>();

	private AWSCredentials credentials;
	private String bucket;
	private String prefix;

	private ExistingFileHandler existingFileHandler;

	public S3FileTransfer(String folderSource, AWSCredentials credentials, String bucket, String prefix,
			FileTransferObject transferObject, ExistingFileHandler handler) throws FileTransferException {
		super(folderSource, transferObject);
		this.credentials = credentials;
		this.bucket = bucket;
		this.prefix = prefix;
		this.status = "";
		this.existingFileHandler = handler;
	}

	@Override
	public String calculateDestination() {
		return bucket + "/" + prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath();
	}

	@Override
	public void transfer() {
		if (logger.isTraceEnabled()) {
			logger.trace("EXECUTING TRANSFER: " + getPath() + " -> " + getDestination());
		}
		if (!Files.exists(getPath())) {
			logger.warn("Error transferring file " + getPath() + " to " + getDestination() + ": " + getPath()
					+ " does not exist.");
			status = "File does not exist";
			return;
		}
		try {
			String remoteDest = prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath();
			boolean skip = false;

			if (remoteDestExists(remoteDest)) {
				skip = existingFileHandler.getOptions(getPath()).isSkip();
			}

			if (!skip) {
				getClient(credentials).putObject(bucket, remoteDest, getPath().toFile());
				status = "\u2713";
			} else {
				status = "Skipped - File exists";
			}
		} catch (IllegalArgumentException e) {
			logger.warn("Error transferring file " + getPath() + " to " + getDestination(), e);
			status = "Input error: " + e.getMessage();
		} catch (AmazonS3Exception e) {
			logger.warn("Error transferring file " + getPath() + " to " + getDestination(), e);
			status = "Upload error: " + e.getMessage();
		}
	}

	private boolean remoteDestExists(String remoteDest) {
		try {
			getClient(credentials).getObjectMetadata(bucket, remoteDest);
			return true;
		} catch (AmazonServiceException e) {
			if (e.getStatusCode() == 404) {
				return false;
			}
			throw e;
		}
	}

	private static AmazonS3Client getClient(AWSCredentials credentials) {
		if (client.get() == null || creds.get() != credentials) {
			creds.set(credentials);
			client.set(new AmazonS3Client(credentials));
		}
		return client.get();
	}

	public static FileTransfer create(String folderSource, AWSCredentials credentials, String bucket, String prefix,
			FileTransferObject transferObject, ExistingFileHandler handler) {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("New S3 file transfer: " + transferObject.getSourcePath() + " -> " + bucket + "/" + prefix
						+ "/" + transferObject.getFileName());
			}
			return new S3FileTransfer(folderSource, credentials, bucket, prefix, transferObject, handler);
		} catch (FileTransferMissingFileException e) {
			logger.warn("Missing file " + transferObject.getSourcePath(), e);
			return new ErrorFileTransfer(e.getMessage(), e.getPath());
		} catch (FileTransferException e) {
			logger.warn("Error transferring file " + transferObject.getSourcePath(), e);
			return new ErrorFileTransfer(e.getMessage(), transferObject.getSourcePath());
		}
	}

}
