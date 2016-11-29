package de.haw.rn.proxy.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.haw.rn.proxy.client.entities.Account;
import de.haw.rn.proxy.util.filemodder.AccountGetter;
import de.haw.rn.proxy.util.filemodder.MessageSaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Properties;

public class Pop3ProxyClient implements Runnable {

	private static final int DEFAULT_TIMEOUT = 30000;
	private final String TERMINATION = ".";

	private Socket socket;

	private BufferedReader reader;
	private BufferedWriter writer;

	private int timeout;

	public Pop3ProxyClient(){
		this.timeout = DEFAULT_TIMEOUT;
	}
	
	//timeout in sec übergeben
	public Pop3ProxyClient(int timeout) {
		this.timeout = timeout * 1000;
	}

	public void connect(String host, int port) throws UnknownHostException, IOException  {

//		socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
		socket = new Socket(host,port);

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

		System.out.println("Connected to host");
		String response = readLine();
		System.out.println(response);
	}

	public boolean isConnected() {
		return socket.isConnected();
	}

	public void disconnect() throws IOException {
		if (!isConnected()) {
			throw new IllegalStateException("No connection to host");
		}
		socket.close();
		reader = null;
		writer = null;

		System.out.println("Disconnected from the host");
	}

	private String readLine() throws IOException {
		String response = reader.readLine();

		if (response.startsWith("-ERR"))
			throw new RuntimeException("ErrorMEssage: " + response);
		return response;
	}

	private String send(String command) throws IOException {

		try {
			writer.write(command + "\n");
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return readLine();
	}

	public void login(String username, String password) throws IOException  {
		// byte[] name = username.getBytes(StandardCharsets.US_ASCII);
		// byte[] pass = password.getBytes(StandardCharsets.US_ASCII);
		send("USER " + username);
		send("PASS " + password);
	}
	
	public void deleteMessage(int i) throws IOException{
		send("DELE "+i);
	}

	private void quit() throws IOException  {
		send("QUIT");
	}

	public int getNumberOfMessages() throws IOException {
		String response = send("STAT");
		System.out.println("STATS -> "+response);
		String[] values = response.split(" ");
		return Integer.parseInt(values[1]);
	}

	public void saveMessage(int i) throws IOException {
		String response = send("RETR " + i);
		MessageSaver saver = new MessageSaver();
		StringBuilder sb = new StringBuilder();
		while (!(response = readLine()).equals(TERMINATION)) {
			if(response.startsWith(TERMINATION)){
				response = response.replaceFirst(TERMINATION, "");	//"." entfernen
			}
			sb.append(response + "\r\n");
		}

		saver.saveMessage(sb.toString());
	}
	
	public void list(int i) throws IOException{
		send("LIST "+i);
	}
	
	public void getMessagesForOneAccount(Account account) throws UnknownHostException, IOException {
		connect(account.getServer(), account.getPort());
		login(account.getEmail(), account.getPass());
		int count = getNumberOfMessages();
		for(int i = 1; i<=count; i++){
			saveMessage(i);
			deleteMessage(i); 
		}
		quit();
		disconnect();
	}
	
	public void getMessagesForAllAcoounts() throws IOException {
		List<Account> accountList = AccountGetter.getAllAccounts();
		for(Account acc : accountList){
			getMessagesForOneAccount(acc);
		}
	}

	@Override
	public void run() {
		while(true){
			try {
				getMessagesForAllAcoounts();
				Thread.sleep(timeout);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
