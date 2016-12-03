package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Message;

import mailbox.ProxyMessage;
import proxy.POP3Proxy;
import proxy.UserAccount;



public class ProxyClient implements Runnable {

	
//	private static final String USER_ACCOUNTS = "C:\\Users\\Biraj\\workspace\\RN_Aufgabe_2\\src\\main\\resources\\UserAccounts.txt";
	private static final String USER_ACCOUNTS = "../RNP_Aufgabe2/src/main/resources/UserAccounts.txt";
	public ArrayList<UserAccount> userAccounts;
	
	private OutputStreamWriter out;
	BufferedReader reader;
	
	private Socket clientsocket;
	
	private boolean bOK = true;
	
	private POP3Proxy proxy;
	
	
	
	public ProxyClient(POP3Proxy proxy){
		this.proxy = proxy;
		userAccounts = UserAccount.createUserAccounts(new File(USER_ACCOUNTS));
	}
	
	private void connectUser() throws UnknownHostException, IOException{
			clientsocket = new Socket("localhost", 11000);
		
			out = new OutputStreamWriter(clientsocket.getOutputStream(), StandardCharsets.UTF_8);
			InputStreamReader in = new InputStreamReader(clientsocket.getInputStream(), StandardCharsets.UTF_8);
			reader = new BufferedReader(in);
	}

	private void authenticate(UserAccount user) throws IOException
	{
		write("USER " + user.get_name());
		write("PASS " + user.get_passwort());
	}
	
	private int getNumberOfMails() throws IOException {
		String[] values = write("STAT").split(" ");
		
		return Integer.parseInt(values[1]);
	}
	
	private void getMailsFromServer(int numberOfMails) throws IOException {
		for(int i = 1; i <= numberOfMails; i ++){
			String input;
			write("RETR " + i);
			ArrayList<String> message = new ArrayList<String>();
			while(!(input = readFromServer()).equals(".")){
				message.add(input);
			}
			
			String[] values = write("LIST " + 1).split(" ");
			System.out.println(values[0]);
			System.out.println(values[1]);
			System.out.println(values[2]);
			message.add(values[2]);
			proxy.getMessagesList().add(new ProxyMessage(message,proxy.getMessagesList().size() + 1 ));
//			messages.add(message);
			deleteMails(i);
		}
		quitConnection();
		disconnectUser();

	}
	
	private void deleteMails(int i) throws IOException {
		write("DELE " + i);
		
	}
	
	private void quitConnection() throws IOException{
		write("QUIT");
	}
	
	private String write(String line) throws IOException {
		if(bOK){
			System.out.println(line);
			line = line + "\r\n";
				out.write(line);
				out.flush();
		}else{
			
		}
		return readFromServer();
	}
	
	private String readFromServer() throws IOException {
		String line = "";

		line = reader.readLine();
		if(line == null){
		}
		if(line.startsWith("-ERR")){
			bOK = false;
		}
//		System.out.println(line);
			
		return line;
	}
	
	private void disconnectUser() throws IOException{
		clientsocket.close();
	}
			
	@Override
	public void run() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
				for (UserAccount user : userAccounts) {
					connectUser();
					readFromServer();
					authenticate(user);
					getMailsFromServer(getNumberOfMails());
					
					
				}
				System.out.println("Wait for 30s seconds");
				} catch (IOException e) {
					System.out.println("Something went terribly wrong");
				}
			}
		}, 0, 10 * 1000);
		
	}

//	@Override
//	public void run() {
//
//				try {
//				for (UserAccount user : userAccounts) {
//					connectUser();
//					readFromServer();
//					authenticate(user);
//					getMailsFromServer(getNumberOfMails());
//					
//					
//				}
//				System.out.println("Wait for 30 seconds");
//				} catch (IOException e) {
//					System.out.println("Something went terribly wrong");
//				}
//		
//	}
}