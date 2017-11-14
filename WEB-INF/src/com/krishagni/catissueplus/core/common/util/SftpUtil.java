package com.krishagni.catissueplus.core.common.util;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;

public class SftpUtil implements Closeable {
	public static class File {
		private String path;

		private String name;

		private boolean directory;

		private long atime;

		private long mtime;

		private long size;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isDirectory() {
			return directory;
		}

		public void setDirectory(boolean directory) {
			this.directory = directory;
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
	}

	private ChannelSftp channel;

	public SftpUtil(ChannelSftp channel) {
		this.channel = channel;
	}

	public void put(String localPath, String remotePath) {
		try {
			channel.put(localPath, remotePath);
		} catch (Throwable t) {
			throw new RuntimeException("Error uploading file " + localPath + " to " + remotePath, t);
		}
	}

	public void get(String remotePath, String localPath) {
		try {
			channel.get(remotePath, localPath);
		} catch (Throwable t) {
			throw new RuntimeException("Error downloading file " + remotePath + " to " + localPath, t);
		}
	}

	public List<File> ls(String remotePath) {
		try {
			Vector<ChannelSftp.LsEntry> remoteFiles = channel.ls(remotePath);

			List<File> result = new ArrayList<>();
			for (ChannelSftp.LsEntry remoteFile : remoteFiles) {
				if (remoteFile.getFilename().equals(".") || remoteFile.getFilename().equals("..")) {
					continue;
				}

				File file = new File();
				file.setDirectory(remoteFile.getAttrs().isDir());
				file.setName(remoteFile.getFilename());
				file.setPath(remotePath + "/" + remoteFile.getFilename());
				file.setAtime(remoteFile.getAttrs().getATime());
				file.setMtime(remoteFile.getAttrs().getMTime());
				file.setSize(remoteFile.getAttrs().getSize());
				result.add(file);
			}

			return result;
		} catch (Throwable t) {
			throw new RuntimeException("Error listing remote files " + remotePath, t);
		}
	}

	public void rm(String remotePath) {
		try {
			channel.rm(remotePath);
		} catch (Throwable t) {
			throw new RuntimeException("Error deleting remote file " + remotePath, t);
		}
	}

	@Override
	public void close() {
		try {
			if (channel != null) {
				channel.disconnect();
			}
		} catch (Throwable t) {

		}
	}
}
