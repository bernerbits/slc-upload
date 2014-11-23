package net.bernerbits.avolve.slcupload;

import java.util.List;
import java.util.stream.Collectors;

import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.model.S3RootBucketFolder;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

public class S3Connection {

	private final List<Bucket> buckets;
	private final AmazonS3Client client;
	private final AWSCredentials creds;

	public S3Connection(AWSCredentials creds, AmazonS3Client client, List<Bucket> buckets) {
		this.creds = creds;
		this.client = client;
		this.buckets = buckets;
	}

	public List<RemoteFolder> listRemoteFolders() {
		return buckets.stream().map((b) -> new S3RootBucketFolder(b, creds, client)).collect(Collectors.toList());
	}

}
