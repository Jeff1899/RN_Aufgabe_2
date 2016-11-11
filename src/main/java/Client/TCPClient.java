package Client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPClient extends Thread {

	private Socket clientsocket;
	private String host;
	private String port;
	private String password;

	public TCPClient(String host, String port) {
		this.host = host;
		this.port = port;
	}

	public void run() {
		try {
			clientsocket = new Socket(host, Integer.parseInt(port));
			clientsocket.setSoTimeout(3000);
			// Message From Server
			InputStream in = clientsocket.getInputStream();
			InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
			BufferedReader brServer = new BufferedReader(isr);

			// Message to server
			OutputStream out = clientsocket.getOutputStream();
			OutputStreamWriter osr = new OutputStreamWriter(out, StandardCharsets.UTF_8);
			BufferedWriter bw = new BufferedWriter(osr);

			BufferedReader brConsole = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));

			String str = null;
//			
			str = brServer.readLine();
			System.out.println(str);
			
//			str = brConsole.readLine();
			// Send Message to Socket
			System.out.println("CAPA");
			bw.write("CAPA");
			bw.newLine();
			bw.flush();
			while (true) {


				// Receive respone from Socket
				str = brServer.readLine();
				System.out.println(str);
				
//				System.out.println("Waiting for aInput!");
				// Message from Console

				str = brConsole.readLine();
				// Send Message to Socket
				bw.write(str);
				bw.newLine();
				bw.flush();

				if (str.contains("OK BYE")) {
					break;
				}

			}
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("java TCPClient <Hostadress> <Portnr>");
		}
		Thread clientthread = new TCPClient(args[0], args[1]);
		clientthread.run();

	}

}
