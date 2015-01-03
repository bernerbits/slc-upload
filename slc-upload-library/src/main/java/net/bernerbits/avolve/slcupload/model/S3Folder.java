package net.bernerbits.avolve.slcupload.model;

import com.amazonaws.auth.AWSCredentials;

public abstract class S3Folder extends RemoteFolder {
	public abstract AWSCredentials getCredentials();

	public abstract String getBucketName();

	public abstract String getPrefix();

	@Override
	public String toString() {
		return getPath();
	}
}
