package net.bernerbits.avolve.slcupload;

import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.exception.FileTransferMissingFileException;
import net.bernerbits.avolve.slcupload.handler.ExistingFileHandler;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.util.function.NullSafe;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3FileTransfer extends RealFileTransfer {

	private static ThreadLocal<AWSCredentials> creds = new ThreadLocal<>();
	private static ThreadLocal<AmazonS3Client> client = new ThreadLocal<>();
	private static final Map<String, SoftReference<Set<String>>> objCache = new ConcurrentHashMap<>();

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
		if (!Files.exists(getPath())) {
			status = "File does not exist";
			return;
		}
		try {
			String remoteDest = prefix + (prefix.isEmpty() ? "" : "/") + getRemotePath();
			boolean skip = false;
			Set<String> summaries = getSummaries();
			
			if (summaries.contains(remoteDest.toLowerCase())) {
				skip = existingFileHandler.getOptions(getPath()).isSkip();
			}

			if (!skip) {
				getClient(credentials).putObject(bucket, remoteDest, getPath().toFile());
				summaries.add(remoteDest);
				status = "\u2713";
			} else {
				status = "Skipped - File exists";
			}
		} catch (IllegalArgumentException e) {
			status = "Input error: " + e.getMessage();
		} catch (AmazonS3Exception e) {
			status = "Upload error: " + e.getMessage();
		}
	}

	private Set<String> getSummaries() {
		synchronized(objCache)
		{
			SoftReference<Set<String>> ref = objCache.get(bucket);
			Set<String> summaries = ref == null ? null : ref.get();
			if (summaries == null) {
				ObjectListing listing = getClient(credentials).listObjects(bucket);
				summaries = new ConcurrentSkipListSet<>(listing.getObjectSummaries().stream()
						.map((os) -> os.getKey().toLowerCase()).collect(NullSafe.toListCollector()));
				while (listing.isTruncated()) {
					listing = getClient(credentials).listNextBatchOfObjects(listing);
					summaries.addAll(listing.getObjectSummaries().stream()
							.map((os) -> os.getKey().toLowerCase()).collect(NullSafe.toListCollector()));
				}
				ref = new SoftReference<>(summaries);
				objCache.put(bucket, ref);
			}
			return summaries;
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
			return new S3FileTransfer(folderSource, credentials, bucket, prefix, transferObject, handler);
		} catch (FileTransferMissingFileException e) {
			return new ErrorFileTransfer(e.getMessage(), e.getPath());
		} catch (FileTransferException e) {
			return new ErrorFileTransfer(e.getMessage(), transferObject.getSourcePath());
		}
	}

}
