package de.haw.rn.proxy.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

	private static final int MAX_COMMAND_LENGHT = 128;
	private Socket client;
	
	private final char CR = 13; //ASCII carriage return;
	private final char LF = 10; //ASCII line feed (new line)
	private final String CRLF = "" + CR + LF;

	public ClientHandler(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		Session session = new Session();

		try {
			PrintWriter clientWriter = new PrintWriter(client.getOutputStream());
			System.out.println("answer: +OK Hello World");
			clientWriter.println("+OK Hello World!");
			clientWriter.flush();
			String commandString;
			while ((commandString = verifyNextCommand(client)) != null) {
				String answer = session.executeCommand(commandString);
				clientWriter.print(answer);
				clientWriter.print(CRLF);
				clientWriter.flush();
				System.out.print("answer: ");
				System.out.println(answer);
				System.out.println();
				if (session.isQuitted()) {
					System.out.println("client quitted");
					client.close();
					session = null;
					break;
				}
			}
			// Verbindung unterbrochen
			System.out.println("connection lost");
			session = null;
		} catch (IOException e) {

		}
	}

	private String verifyNextCommand(Socket client) throws IOException {
		int nextChar;
		StringBuilder command = new StringBuilder();
		StringBuilder commandInt = new StringBuilder();
		InputStream in = client.getInputStream();
		
		int cl = -1; //InputStream.read() connection (to client) lost

		while ((nextChar = in.read()) != LF && nextChar != cl && (commandInt.append(nextChar + " ") != null)) {

			if (nextChar == CR) {
				nextChar = in.read();
				commandInt.append(nextChar);
				break;
			} else {
				if (command.length() + 1 > MAX_COMMAND_LENGHT) {
					throw new IOException("Befehl ist zu lang. Max Befehlslänge ist " + MAX_COMMAND_LENGHT);
				}
			}
			command.append((char) nextChar);
		}
		System.out.println("recieved: " + command + "\t- ASCII: " + commandInt);
		if (command.toString().equals("")) {
			return null;
		} else {
			return command.toString();
		}
	}
}
