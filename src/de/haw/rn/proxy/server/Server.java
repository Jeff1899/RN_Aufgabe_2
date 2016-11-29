package de.haw.rn.proxy.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

	private final int MAX_CLIENTS = 2;

	public final int PORT;

	public Server(int port) {
		this.PORT = port;
	}

	@Override
	public void run() {
		try {
			ServerSocket server = new ServerSocket(PORT);
			System.out.println("Server gestartet auf Port " + PORT);
			Socket client;
			Thread[] activeClients = new Thread[MAX_CLIENTS];

			while (!server.isClosed() && !Thread.currentThread().isInterrupted()) {
				client = server.accept();
				Thread clientHandler = null;
				for (int i = 0; clientHandler == null && i < MAX_CLIENTS; i++) {
					
					if (activeClients[i]==null || !activeClients[i].isAlive()) {
						System.out.println("client " + client.getInetAddress() + " accepted");
						clientHandler = new Thread(new ClientHandler(client));
						clientHandler.start();
						activeClients[i] = clientHandler;
					}
				}
				if (clientHandler == null) {
					client.close();
				}

			}
			for (Thread clientThread:activeClients) {
				if (clientThread != null) {
					clientThread.interrupt();
				}
			}
			server.close();
			for (Thread clientThread:activeClients) {
				if (clientThread != null) {
					clientThread.join();
				}
			}

		} catch (IOException ioE) {
			System.err.println(ioE.getMessage());
//			e.printStackTrace();
		} catch (InterruptedException iE) {
			System.err.println(iE.getMessage());
//			e.printStackTrace();
		}

	}
}
