package net.bernerbits.avolve.slcupload.model;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;

public class S3BucketChildFolder extends S3Folder {

	private S3Folder parent;
	private String name;
	private List<RemoteFolder> children;
	private AmazonS3Client client;
	
	public S3BucketChildFolder(S3Folder parent, String name, AmazonS3Client client) {
		this.parent = parent;
		this.name = name;
		this.client = client;
		this.children = null;
	}

	@Override
	public String getPath() {
		return parent.getPath() + "/" + getName();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<RemoteFolder> getChildren() {
		if (children == null)
		{
			children = loadChildren();
		}
		return children;
	}

	@Override
	public String getBucketName() {
		return parent.getBucketName();
	}

	@Override
	public AWSCredentials getCredentials() {
		return parent.getCredentials();
	}
	
	@Override
	public String getPrefix() {
		String prefix = parent.getPrefix();
		return prefix + (prefix.isEmpty() ? "" : "/") + getName();
	}
	
	private List<RemoteFolder> loadChildren() {
		List<RemoteFolder> children = new ArrayList<>();

		ListObjectsRequest req = new ListObjectsRequest(getBucketName(), getPrefix() + "/", "", "/", 1000);
		ObjectListing listing = client.listObjects(req);
		for (String prefix : getAllCommonPrefixes(listing)) {
			prefix = prefix.substring(0, prefix.length() - 1);
			addChild(children, prefix.substring(prefix.lastIndexOf("/")+1));
		}
		
		return children;
	}

	private List<String> getAllCommonPrefixes(ObjectListing listing) {
		List<String> summaries = new ArrayList<>(listing.getCommonPrefixes());
		while (listing.isTruncated()) {
			listing = client.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getCommonPrefixes());
		}
		return summaries;
	}

	private void addChild(List<RemoteFolder> children, String folderName) {
		S3BucketChildFolder folder = new S3BucketChildFolder(this, folderName, client);
		children.add(folder);
	}

}
