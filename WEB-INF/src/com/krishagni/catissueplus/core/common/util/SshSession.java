package com.krishagni.catissueplus.core.common.util;

import org.apache.commons.io.IOUtils;

import net.schmizz.sshj.SSHClient;

public class SshSession {
	private String host;

	private String user;

	private String password;

	private SSHClient client;

	public SshSession(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	public void connect() {
		client = new SSHClient();
		try {
			client.loadKnownHosts();
			client.connect(host);
			client.authPassword(user, password);
		} catch (Exception e) {
			throw new RuntimeException("Error connecting to remote host", e);
		}
	}

	public SftpUtil newSftp() {
		try {
			return new SftpUtil(client.newSFTPClient());
		} catch (Exception e) {
			throw new RuntimeException("Error creating SFTP client", e);
		}
	}

	public void close() {
		IOUtils.closeQuietly(client);
	}
}
