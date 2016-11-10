package proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * 
 * @author Jeff
 *
 */
public class POP3Proxy {

	private static final String USER_ACCOUNTS = "C:\\Users\\Biraj\\workspace\\RN_Aufgabe_2\\src\\main\\resources\\UserAccounts.txt";
	private int _port;
	private ArrayList<UserAccount> userAccounts;
	private ArrayList<Message> messages;
	public static Map<UserAccount, ArrayList<Message>> userMessages = new HashMap<UserAccount, ArrayList<Message>>();

	public POP3Proxy(String[] args) {
		if (args.length != 1) {
			System.out.println("ERROR");
		}

		_port = Integer.parseInt(args[0]);
		userAccounts = UserAccount.createUserAccounts(new File(USER_ACCOUNTS));

		checkTime();
		ServerSocket server = null;
		try {
			server = new ServerSocket(1050);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while (true) {

			try {
				System.out.println("Wait for Thunderbird");
				Socket socket = server.accept();
				new POP3ServerThread(socket);
				System.out.println("connect");
				// Hier kommt die ganze POP3 Protocal
				

				// InputStream in = s.getInputStream();
				// InputStreamReader isr = new InputStreamReader(in,
				// StandardCharsets.UTF_8);
				// BufferedReader brServer = new BufferedReader(isr);

				// String messageFromClient = in.readLine();
				// System.out.println(messageFromClient);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class POP3ServerThread extends Thread {

		private Socket socket;
		private String currentState = "AUTH";
		BufferedWriter outtoThunderbird;
		BufferedReader in;
		POP3ServerThread(Socket socket) {
			this.socket = socket;
		}
		@Override
		public void run(){
			try {
				outtoThunderbird = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				outtoThunderbird.write("+OK POP3 server ready\n\r");
				in = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			
				while(true){
					
					if (currentState == "AUTH") {
                        authenticate();
                    } //Transaction State
                    else if (currentState == "TRANS") {
                        transaction();
                    }
				}
			
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}
		//Our server doesnot support AUTH and CAPA
		private void authenticate() throws IOException {
			String line = in.readLine();
			if(line.startsWith("CAPA") || line.startsWith("AUTH")){
				outtoThunderbird.write("-ERR AUTH CAPA not supported");
			}else if(line.startsWith("USER ")){
				//check if there is user with the name
				if(line.substring(5).equals(userAccounts.get(1).get_name())){
					outtoThunderbird.write("Give Password");
					line = in.readLine();
					if(line.substring(5).equals(userAccounts.get(1).get_passwort())){
						outtoThunderbird.write("Connection sucessful");
						currentState = "TRANS";
						
					}
				}else if(line.startsWith("QUIT ")){
					outtoThunderbird.write(" +OK dewey POP3 server signing off");
				}
			}
		}
		
		private void transaction() throws IOException {
				String line = in.readLine();
				if(line.startsWith("STAT ")){
					outtoThunderbird.write("+OK " + messages.size());
				}else if(line.startsWith("LIST ")){
					outtoThunderbird.write("+OK List mail");
				}else if(line.startsWith("RETR ")){  //RETR msg  ->msg is num of  message(required)
					outtoThunderbird.write("+OK ---octals");
					outtoThunderbird.write(messages.get(Integer.parseInt(line.substring(6))).toString());
				}else if(line.startsWith("DELE")){
					outtoThunderbird.write("Handle delete");
				}else if(line.startsWith("NOOP ")){
					outtoThunderbird.write("OK NOOP");
				}else if(line.startsWith("RSE")){
					outtoThunderbird.write("Handle reset");
				}else if(line.startsWith("UIDL")){
					outtoThunderbird.write("handle UIDl");
				}else if(line.startsWith("QUIT ")){
					outtoThunderbird.write("handleQuit");
				}
				
		}
		
		
		
		
		
		
		
	}

	private ArrayList<Message> receivingEmails(UserAccount user) {
		// create properties field
		Properties properties = new Properties();

		properties.put("mail.pop3.host", user.get_host());
		properties.put("mail.pop3.port", user.get_port());
		properties.put("mail.pop3.starttls.enable", "true");
		Session emailSession = Session.getDefaultInstance(properties);

		// create the POP3 store object and connect with the pop server
		Store store;
		Message[] messages = null;
		ArrayList<Message> messageList = new ArrayList<Message>();
		try {
			store = emailSession.getStore("pop3s");

			store.connect(user.get_host(), user.get_port(), user.get_name(), user.get_passwort());

			// create the folder object and open it
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_ONLY);

			// retrieve the messages from the folder in an array and print
			// it
			messages = emailFolder.getMessages();
			System.out.println("messages.length---" + messages.length);

			for (int i = 0; i < messages.length; i++) {
				Message message = messages[i];
				System.out.println("Message " + (i + 1));
				System.out.println("Subject: " + message.getSubject());
				System.out.println("From: " + message.getFrom()[0]);
				try {
					System.out.println("Text: " + message.getContent().toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			emailFolder.close(false);
			store.close();

		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Collections.addAll(messageList, messages);
		return messageList;
	}

	private void checkTime() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {

				for (UserAccount user : userAccounts) {
					messages.addAll(receivingEmails(user));
					if (userMessages.containsKey(user)) {
						userMessages.get(user).addAll(receivingEmails(user));
						System.out.println(userMessages.get(user).size());
					} else {
						userMessages.put(user, receivingEmails(user));
					}
				}
				System.out.println("Wait for 30 seconds");
			}
		}, 0, 30 * 1000);
	}
}
