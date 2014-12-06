package net.bernerbits.avolve.slcupload;

import java.nio.file.Files;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.exception.FileTransferMissingFileException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public class S3FileTransfer extends RealFileTransfer {

	private static ThreadLocal<AWSCredentials> creds = new ThreadLocal<>();
	private static ThreadLocal<AmazonS3Client> client = new ThreadLocal<>();

	private AWSCredentials credentials;
	private String bucket;
	private String prefix;

	public S3FileTransfer(String folderSource, AWSCredentials credentials, String bucket, String prefix,
			FileTransferObject transferObject) throws FileTransferException {
		super(folderSource, transferObject);
		this.credentials = credentials;
		this.bucket = bucket;
		this.prefix = prefix;
		this.status = "";
	}

	@Override
	public String calculateDestination() {
		return bucket + "/" + prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath();
	}

	@Override
	public void transfer() {
		if (!Files.exists(getPath())) {
			status = "File does not exist";
			return;
		}
		try {
			getClient(credentials).putObject(bucket, prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath(),
					getPath().toFile());
			status = "\u2713";
		} catch (IllegalArgumentException e) {
			status = "Input error: " + e.getMessage();
		} catch (AmazonS3Exception e) {
			status = "Upload error: " + e.getMessage();
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
			FileTransferObject transferObject) {
		try {
			return new S3FileTransfer(folderSource, credentials, bucket, prefix, transferObject);
		} catch (FileTransferMissingFileException e) {
			return new ErrorFileTransfer(e.getMessage(), e.getPath());
		} catch (FileTransferException e) {
			return new ErrorFileTransfer(e.getMessage(), transferObject.getSourcePath());
		}
	}

}
