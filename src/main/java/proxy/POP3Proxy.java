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

import javax.mail.Flags.Flag;
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
				new POP3ServerThread(socket).start();
				System.out.println("connect");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class POP3ServerThread extends Thread {
		private boolean serving = true;
		private Socket socket;
		private String currentState = "AUTH";
		BufferedWriter outtoThunderbird;
		BufferedReader in;

		POP3ServerThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				outtoThunderbird = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				outtoThunderbird.write("+OK POP3 server ready\n\r");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				while (serving) {
					// authentication state
					if (currentState.equals("AUTH")) {
						authenticate();

					} // Transaction State
					else if (currentState.equals("TRANS")) {
						transaction();
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// Our server doesnot support AUTH and CAPA
		private void authenticate() throws IOException {
			String line = in.readLine();
			System.out.println(line);
			if (line.startsWith("CAPA") || line.startsWith("AUTH")) {
				outtoThunderbird.write("-ERR AUTH CAPA not supported");
			} else if (line.startsWith("USER ")) {
				// check if there is user with the name
				UserAccount user = null;
				user = checkUser(line.substring(5));
				if (user != null) {
					outtoThunderbird.write("Give Password");
					line = in.readLine();
					if (checkPassword(user, line.substring(5))) {
						outtoThunderbird.write("Connection sucessful");
						currentState = "TRANS";
					} else {
						outtoThunderbird.write("-ERR Wrong Password");
					}
				} else if (line.startsWith("QUIT ")) {
					outtoThunderbird.write(" +OK POP3 server signing off");
				} else {
					outtoThunderbird.write("-ERR Wrong Username");
				}
			}
		}

		private UserAccount checkUser(String name) {
			for (UserAccount user : userAccounts) {
				if (user.get_name().equals(name)) {
					return user;
				}
			}
			return null;
		}

		private boolean checkPassword(UserAccount user, String password) {
			if (user.get_passwort().equals(password)) {
				return true;
			}
			return false;
		}

		private void transaction() throws IOException, MessagingException {
			String line = in.readLine();
			if (line.startsWith("STAT ")) { // OK + nr of msg in maildrop + " "
											// + size of mail drop in octets
				outtoThunderbird.write("+OK " + messages.size() + "size of maildrop in octals");
			} else if (line.startsWith("LIST ")) { // param optional
				// with param //ok + mail with param or error if the msg nr is
				// not there
				// whithout param //ok + all mail
				handleList();
			} else if (line.startsWith("RETR ")) { // RETR msg ->msg is num of
													// message( param required)
				handleRetr(line);
			} else if (line.startsWith("DELE ")) { // param msg nr required
				handleDelete(line);
				outtoThunderbird.write("Ok del");
			} else if (line.startsWith("NOOP ")) {// The POP3 server does
				outtoThunderbird.write("+OK"); // nothing, it merely
												// replies with a positive
												// response

			} else if (line.startsWith("RSET")) { // no param
				// Unmark the message that are marked as deleted
				handleReset(line);
			} else if (line.startsWith("UIDL")) {
				handleUIDL(line);
				// if with paramater(msg nr)
				// if msg nr is there OK + message nr + " " + unique id from
				// message
				// if msg nr is not there -ERR no such message, only 2 messages
				// in maildrop
				// without parameter
				// multi line, +OK as first line and each line with message nr +
				// " " + unique id from message
				outtoThunderbird.write("handle UIDl");
			} else if (line.startsWith("QUIT ")) {
				currentState = "UPDATE"; // in translation state Quit command
											// will change the state to UPDATE
				outtoThunderbird.write("+OK Server signing off");
				serving = false;
			}

		}

		private void handleList() throws IOException {
			if(messages.size() > 0){
				for(Message message : messages){
					outtoThunderbird.write(message.toString());
				}
			}else{
				
			}
			outtoThunderbird.write("+OK List mail");

		}

		private void handleRetr(String line) throws IOException {
			outtoThunderbird.write("+OK ---octals");
			int messageNr = Integer.parseInt(line.substring(6));
			if(messages.size() > messageNr){ 
				outtoThunderbird.write(messages.get(messageNr).toString());
			}else{
				outtoThunderbird.write("-ERR no such message, only " + messages.size() + " messages in maildrop");
			}
		}

		private void handleReset(String line) throws MessagingException, IOException {
			for (Message message : messages) {
				message.setFlag(Flag.DELETED, false);
			}
			outtoThunderbird.write("ok");
		}

		private void handleDelete(String line) throws IOException, MessagingException {
			boolean contains = false;
			for (Message message : messages) {
				if (message.getMessageNumber() == Integer.parseInt(line.substring(5))) {
					contains = true;
					message.setFlag(Flag.DELETED, true);
					outtoThunderbird.write("+Ok deleted");
				}
			}
			if (contains == false) {
				outtoThunderbird.write("-ERR only " + messages.size() + "are avialable");
			}
		}

		private void handleUIDL(String line) throws IOException {
			if (line.length() > 5) {
				for (Message message : messages) {
					if (message.getMessageNumber() == Integer.parseInt(line.substring(5))) {
						outtoThunderbird.write("+Ok " + message.getMessageNumber() + " " + message.hashCode());
					}
				}
				outtoThunderbird.write("-ERR no such message, only " + messages.size() + " messages in maildrop");

			} else {
				outtoThunderbird.write("+OK");
				for (Message message : messages) {
					outtoThunderbird.write(message.getMessageNumber() + " " + message.hashCode()); // unique
																									// id
																									// from
																									// messsage
																									// =>
																									// hashcode
																									// ?
				}
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
				System.out.println(message.toString());
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
			e.printStackTrace();
		} catch (MessagingException e) {
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
					// messages.addAll(receivingEmails(user));
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
