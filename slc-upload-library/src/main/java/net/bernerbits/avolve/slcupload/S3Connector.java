package net.bernerbits.avolve.slcupload;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

public class S3Connector {

	public @Nullable S3Connection connect(String awsKey, String awsSecret) {
		AWSCredentials creds = new BasicAWSCredentials(awsKey, awsSecret);
		AmazonS3Client client = new AmazonS3Client(creds);
		try
		{
			List<Bucket> buckets = client.listBuckets();
			return new S3Connection(creds, client, buckets);
		} catch (AmazonClientException ex) {
			ex.printStackTrace();
			return null;
		}
	}

}
