package de.haw.rn.proxy.client.entities;

public class Message {
	
	private String message;
	private int size;
	private int id;
	
	public Message(int id, String message, int size) {
		this.message = message;
		this.size = size;
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public int getSize() {
		return size;
	}

	public int getId() {
		return id;
	}
	
}
