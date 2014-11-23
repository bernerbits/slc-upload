package net.bernerbits.avolve.slcupload.model;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;

public class S3BucketChildFolder extends S3Folder {

	private S3Folder parent;
	private String name;
	private List<RemoteFolder> children;

	public S3BucketChildFolder(S3Folder parent, String name) {
		this.parent = parent;
		this.name = name;
		this.children = new ArrayList<>();
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
}
