package com.krishagni.catissueplus.core.common.util;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;

public class SftpUtil {
	public static class File {
		private String path;

		private String type;

		private long atime;

		private long mtime;

		private long size;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public long getAtime() {
			return atime;
		}

		public void setAtime(long atime) {
			this.atime = atime;
		}

		public long getMtime() {
			return mtime;
		}

		public void setMtime(long mtime) {
			this.mtime = mtime;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		public static File from(RemoteResourceInfo rri) {
			File file = new File();
			file.setPath(rri.getPath());
			file.setType(rri.getAttributes().getType().name());
			file.setAtime(rri.getAttributes().getAtime());
			file.setMtime(rri.getAttributes().getMtime());
			file.setSize(rri.getAttributes().getSize());
			return file;
		}
	}

	private SFTPClient client;

	public SftpUtil(SFTPClient client) {
		this.client = client;
	}

	public void put(String localPath, String remotePath) {
		try {
			client.put(new FileSystemFile(localPath), remotePath);
		} catch (Exception e) {
			throw new RuntimeException("Error uploading file " + localPath + " to " + remotePath, e);
		}
	}

	public void get(String remotePath, String localPath) {
		try {
			client.get(remotePath, new FileSystemFile(localPath));
		} catch (Exception e) {
			throw new RuntimeException("Error downloading file " + remotePath + " to " + localPath, e);
		}
	}

	public List<File> ls(String remotePath) {
		try {
			List<RemoteResourceInfo> remoteFiles = client.ls(remotePath);
			return remoteFiles.stream().map(File::from).collect(Collectors.toList());
		} catch (Exception e) {
			throw new RuntimeException("Error listing remote files " + remotePath, e);
		}
	}

	public void rm(String remotePath) {
		try {
			client.rm(remotePath);
		} catch (Exception e) {
			throw new RuntimeException("Error deleting remote file " + remotePath, e);
		}
	}

	public void close() {
		IOUtils.closeQuietly(client);
	}
}
