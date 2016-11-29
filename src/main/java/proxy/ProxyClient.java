package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Message;

import proxy.POP3Proxy.POP3ServerThread;

public class ProxyClient implements Runnable {

	
//	private static final String USER_ACCOUNTS = "C:\\Users\\Biraj\\workspace\\RN_Aufgabe_2\\src\\main\\resources\\UserAccounts.txt";
	private static final String USER_ACCOUNTS = "../RNP_Aufgabe2/src/main/resources/UserAccounts.txt";
	public ArrayList<UserAccount> userAccounts;
	
	private OutputStreamWriter out;
	private InputStreamReader in;
	BufferedReader reader;
	
	private Socket clientsocket;
	
	private boolean bOK = true;
	
	public ProxyClient(){
		userAccounts = UserAccount.createUserAccounts(new File(USER_ACCOUNTS));
		try {
			clientsocket = new Socket("localhost", 11000);
		
			out = new OutputStreamWriter(clientsocket.getOutputStream(), StandardCharsets.UTF_8);
			in = new InputStreamReader(clientsocket.getInputStream(), StandardCharsets.UTF_8);
			reader = new BufferedReader(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void authenticate(UserAccount user)
	{
		write("USER " + user.get_name());
		write("PASS " + user.get_passwort());
	}
	
	private int getNumberOfMails() {
		String[] values = write("STAT").split(" ");
		return Integer.parseInt(values[1]);
	}
	
	private void getMailsFromServer(int numberOfMails) {
		for(int i = 1; i <= numberOfMails; i ++){
			String input;
			write("RETR " + i);
			while(!(input = readFromServer()).equals(".")){
				input = input;
			}
			deleteMails(i);
		}
		quitConnection();
	}
	
	private void deleteMails(int i) {
		write("DELE " + i);
		
	}
	
	private void quitConnection(){
		write("QUIT");
	}
	
	private String write(String line) {
		if(bOK){
			System.out.println(line);
			line = line + "\r\n";
			try {
				out.write(line);
				out.flush();

			} catch (IOException e) {
			}
		}else{
			
		}
		return readFromServer();
	}
	
	private String readFromServer() {
		String line = "";
		try {

			line = reader.readLine();
			if(line.startsWith("-ERR")){
				bOK = false;
			}
			System.out.println(line);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}

	@Override
	public void run() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				for (UserAccount user : userAccounts) {
					System.out.println(user.get_name());
					readFromServer();
					authenticate(user);
					getMailsFromServer(getNumberOfMails());
				}
				System.out.println("Wait for 30 seconds");
			}
		}, 0, 30 * 1000);
		
	}


}
