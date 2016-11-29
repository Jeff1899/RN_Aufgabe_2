package de.haw.rn.proxy.client.entities;

public class Account {
	
	private String email;
	private String pass;
	private String server;
	private int port;
	
	public Account(String email, String pass, String server, int port) {
		this.email = email;
		this.pass = pass;
		this.server = server;
		this.port = port;
	}

	public String getEmail() {
		return email;
	}

	public String getPass() {
		return pass;
	}

	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}
}
