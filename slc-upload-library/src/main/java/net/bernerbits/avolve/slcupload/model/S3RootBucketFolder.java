package net.bernerbits.avolve.slcupload.model;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;

public class S3RootBucketFolder extends S3Folder {

	private Bucket bucket;
	private AWSCredentials creds;
	private AmazonS3Client client;
	private List<RemoteFolder> children;

	public S3RootBucketFolder(Bucket bucket, AWSCredentials creds, AmazonS3Client client) {
		this.bucket = bucket;
		this.client = client;
		this.creds = creds;
		generateChildren();
	}

	@Override
	public String getPath() {
		return getName();
	}

	@Override
	public String getName() {
		return bucket.getName();
	}

	@Override
	public List<RemoteFolder> getChildren() {
		return children;
	}

	private void generateChildren() {
		children = new ArrayList<>();

		ListObjectsRequest req = new ListObjectsRequest(bucket.getName(), "", "", "/", 1000);
		ObjectListing listing = client.listObjects(req);
		for (String prefix : getAllCommonPrefixes(listing)) {
			addChild(prefix.substring(0, prefix.length() - 1));
		}
	}

	private List<String> getAllCommonPrefixes(ObjectListing listing) {
		List<String> summaries = new ArrayList<>(listing.getCommonPrefixes());
		while (listing.isTruncated()) {
			listing = client.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getCommonPrefixes());
		}
		return summaries;
	}

	private void addChild(String folderName) {
		S3BucketChildFolder folder = new S3BucketChildFolder(this, folderName, client);
		children.add(folder);
	}

	@Override
	public AWSCredentials getCredentials() {
		return creds;
	}

	@Override
	public String getBucketName() {
		return bucket.getName();
	}

	@Override
	public String getPrefix() {
		return "";
	}

}
