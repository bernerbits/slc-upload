package net.bernerbits.avolve.slcupload;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public class S3FileTransfer extends FileTransfer {

	private static ThreadLocal<AWSCredentials> creds = new ThreadLocal<>();
	private static ThreadLocal<AmazonS3Client> client = new ThreadLocal<>();

	private AWSCredentials credentials;
	private String bucket;
	private String prefix;

	public S3FileTransfer(String folderSource, AWSCredentials credentials, String bucket, String prefix,
			FileTransferObject transferObject) {
		super(folderSource, transferObject);
		this.credentials = credentials;
		this.bucket = bucket;
		this.prefix = prefix;
		this.status = "";
	}

	@Override
	public String getDestination() throws FileTransferException {
		return bucket + "/" + prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath();
	}

	@Override
	public void transfer() {
		try {
			if (!getFile().exists()) {
				status = "File does not exist";
				return;
			}
			try {
				getClient(credentials).putObject(bucket, prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath(),
						getFile());
				status = "OK";
			} catch (IllegalArgumentException e) {
				status = "Input error: " + e.getMessage();
			} catch (AmazonS3Exception e) {
				status = "Upload error: " + e.getMessage();
			}
		} catch (FileTransferException e) {
			status = e.getMessage();
		}
	}

	private static AmazonS3Client getClient(AWSCredentials credentials) {
		if (client.get() == null || creds.get() != credentials) {
			creds.set(credentials);
			client.set(new AmazonS3Client(credentials));
		}
		return client.get();
	}

}
