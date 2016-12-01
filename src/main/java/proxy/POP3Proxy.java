package proxy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


import client.ProxyClient;
import mailbox.ProxyMessage;
import server.ProxyServer;

/**
 * 
 * @author Jeff
 *
 */
public class POP3Proxy {
	
	private boolean serving = true;
	
	private List<ProxyMessage> messagesList = new ArrayList<ProxyMessage>(); 
	
	public POP3Proxy(String[] args) {
		if (args.length != 3) {
			System.out.println("ERROR ..");
		}

		int port = Integer.parseInt(args[0]);
		String username = args[1];
		String password = args[2];
		
		Thread client = new Thread(new ProxyClient(this));
		client.run();
		
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Connection failed");
		}
		
		while (serving) {
			try {
				System.out.println("POP3-Server");
				Socket socket = server.accept();
				new ProxyServer(socket, username, password, this).start();
				
			} catch (IOException e) {
				try {
					server.close();
				} catch (IOException e1) {
					System.err.println("Server Socket cannot be closed");
				}
			}
		}	
	}

	public List<ProxyMessage> getMessagesList() {
		return messagesList;
	}

	public void setMessagesList(List<ProxyMessage> messagesList) {
		this.messagesList = messagesList;
	}
	

}
