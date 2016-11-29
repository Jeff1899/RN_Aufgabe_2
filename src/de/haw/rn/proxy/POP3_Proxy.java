package de.haw.rn.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import de.haw.rn.proxy.client.Pop3ProxyClient;
import de.haw.rn.proxy.server.Server;

public class POP3_Proxy {
	
	private static String workingDirectory;
	public static String getWorkingDirectory(){
		return workingDirectory;
	}

	public static void main(String[] args) {
		// Argumente: port working-directory [update-interval-seconds)]
		final int maxArgsCount = 3;
		try {
			// Fehlerprüfung
			if (args.length > maxArgsCount) {
				throw new Exception("Falsche Parameteranzahl");
			}

			// Argumente parsen
			// PORT
			int port = 0;
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfE) {
				throw new Exception("Falscher Wert für einen Port. Port-Nummer besteht nur aus Zahlen [0-9]");
			}

			// Working-Directory
			workingDirectory = args[1];
			if (workingDirectory.endsWith("/") || workingDirectory.endsWith("\\")) {
				workingDirectory = workingDirectory.substring(0, workingDirectory.length() -1);
			}
			File wd = new File(workingDirectory);
			if (!wd.exists()) {
				throw new Exception("Pfad nicht gefunden: " + workingDirectory);
			}

			// UpdateInterval
			int updateInterval = 30;
			try {
				updateInterval = Integer.parseInt(args[2]);
			} catch (IndexOutOfBoundsException ioobE) {
				// Update-Intervall wurde nicht übergeben. Standard bleibt erhalten -so do nothing
			} catch (NumberFormatException nfE) {
				throw new Exception("[Update-Intervall] muss numerisch sein");
			}
			

			// Programm-Start
			System.out.println("Working-Directory: " + workingDirectory);
			// Server
			Server server = new Server(port);
			Thread serverThread = new Thread(server);
			serverThread.start();

			// Client
			Pop3ProxyClient client;
			client = new Pop3ProxyClient(updateInterval);
			Thread clientThread = new Thread(client);
			clientThread.start();
			
			// auf exit Command warten
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line;
			while (!((line = br.readLine()) == null) && !(line.equals("exit"))) {
			}
			
			// Auf beendigung laufender Threads warten
			System.out.println("pop3-Proxy wird beendet");
			serverThread.interrupt();
			clientThread.interrupt();
			serverThread.join();
			clientThread.join();
			System.out.println("pop3-Proxy beendet");
		} catch (Exception e) {
			// Fehlerausgabe
			System.err.println(e.getMessage());
		}
	}
}
