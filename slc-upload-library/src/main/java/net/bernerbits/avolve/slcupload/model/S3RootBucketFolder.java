package net.bernerbits.avolve.slcupload.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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
		Map<String, S3BucketChildFolder> folders = new HashMap<>();
		
		ObjectListing listing = client.listObjects(bucket.getName());
		for (S3ObjectSummary object : getAllObjectSummaries(listing)) {
			if (object.getKey().contains("/"))
			{
				String folderName = object.getKey().substring(0, object.getKey().lastIndexOf("/"));
				addChild(folders, folderName);
			}
		}
	}

	private List<S3ObjectSummary> getAllObjectSummaries(ObjectListing listing) {
		List<S3ObjectSummary> summaries = new ArrayList<>(listing.getObjectSummaries());
		while (listing.isTruncated())
		{
			listing = client.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getObjectSummaries());
		}
		return summaries;
	}

	private void addChild(Map<String, S3BucketChildFolder> folders,
			String folderName) {
		S3Folder parent;
		String localFolderName;
		if (folderName.contains("/"))
		{
			String parentFolderName = folderName.substring(0, folderName.lastIndexOf("/"));
			if (!folders.containsKey(parentFolderName))
			{
				addChild(folders, parentFolderName);
			}
			localFolderName = folderName.substring(folderName.lastIndexOf("/") + 1);
			parent = folders.get(parentFolderName);
		}
		else
		{
			localFolderName = folderName;
			parent = this;
		}
		if(!folders.containsKey(folderName))
		{
			S3BucketChildFolder folder = new S3BucketChildFolder(parent, localFolderName);
			folders.put(folderName, folder);
			parent.getChildren().add(folder);
		}
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
