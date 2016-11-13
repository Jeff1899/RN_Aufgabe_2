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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	// private static final String USER_ACCOUNTS =
	// "../RNP_Aufgabe2/src/main/resources/UserAccounts.txt";
	private int port;
	private String username;
	private String password;
	private ArrayList<UserAccount> userAccounts;
	private ArrayList<Message> messages = new ArrayList<>();
	public static Map<UserAccount, ArrayList<Message>> userMessages = new HashMap<UserAccount, ArrayList<Message>>();

	public POP3Proxy(String[] args) {
		if (args.length != 3) {
			System.out.println("ERROR");
		}

		port = Integer.parseInt(args[0]);
		username = args[1];
		password = args[2];
		userAccounts = UserAccount.createUserAccounts(new File(USER_ACCOUNTS));

		checkTime();
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Connection failed");
		}

		while (true) {
			try {
				System.out.println("Wait for Thunderbird");
				Socket socket = server.accept();
				new POP3ServerThread(socket).start();
				System.out.println("connect");
			} catch (IOException e) {
				try {
					server.close();
				} catch (IOException e1) {
					System.err.println("Server Socket cannot be closed");
				}
			}
		}
	}

	class POP3ServerThread extends Thread {
		private boolean serving = true;
		private Socket socket;
		private STATE state = STATE.AUTH;
		OutputStreamWriter out;
		InputStreamReader in;
		BufferedReader reader;

		POP3ServerThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
				write("+OK server is ready");
				in = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
				reader = new BufferedReader(in);
				while (serving) {
					if (state == STATE.AUTH) {
						authenticate();
					} // Transaction State
					else if (state == STATE.TRANS) {
						System.out.println("In trans state");
						transaction();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			}

		}

		private void authenticate() throws IOException {
			// String line = read();
			String line = reader.readLine();
			System.out.println(line);
			if (line.startsWith("CAPA") || line.startsWith("AUTH")) {
				handleCapa();
			} else if (line.startsWith("USER ")) {
				System.out.println("in User");
				boolean userCorrect = handleUser(line);
				if (userCorrect) {
					// String line = read();
					line = reader.readLine();
					handlePassword(line);
				}
			} else if (line.startsWith("QUIT")) {
				handleQuit();
			} else {
				handleUnknownCommand();
			}
		}

		private void handleCapa() {
			write("-ERR CAPA ist shit");
			System.out.println("-ERR CAPA ist shit");
		}

		private synchronized boolean handleUser(String line) {
			if (line.substring(5).equals(username)) {
				write("+OK Please enter password");
				System.out.println("+OK Please enter password");
				return true;
			} else {
				write("-ERR Wrong Username");
				return false;
			}

		}

		private synchronized void handlePassword(String line) {
			if (line.substring(5).equals(password)) {
				write("+OK mailbox locked and ready");
				System.out.println("+OK mailbox locked and ready");
				state = STATE.TRANS;
			} else {
				write("-ERR Wrong Password");
			}

		}

		private synchronized void handleQuit() {
			write("-ERR mailbox closing");
		}

		private void handleUnknownCommand() {
			write("-ERR Unknown Command");
		}

		private synchronized void transaction() throws IOException, MessagingException {
			// String line = read();
			String line = reader.readLine();
			System.out.println(line);
			if (line.startsWith("STAT")) {
				handleStat();
			} else if (line.startsWith("LIST")) {
				handleList(line);
			} else if (line.startsWith("RETR")) {
				handleRetr(line);
			} else if (line.startsWith("DELE")) {
				handleDelete(line);
			} else if (line.startsWith("NOOP")) {
				handleNoop();
			} else if (line.startsWith("RSET")) {
				handleReset(line);
			} else if (line.startsWith("UIDL")) {
				handleUIDL(line);
			} else if (line.startsWith("QUIT")) {
				handleQuitTrans();
			}

		}

		/*
		 * OK + nr of msg in maildrop + " " + // + size of mail drop in octets
		 */
		private synchronized void handleStat() throws MessagingException {
			int size = 0;
			if (messages.size() > 0) {
				for (int i = 0; i > messages.size(); i++) {
					size = size + messages.get(i).getSize();
				}
				write("+OK " + messages.size() + size + "size of maildrop in octals");
				System.out.println("+OK " + messages.size() + size + "size of maildrop in octals");
			} else {
				write("-ERR maildrop empty");
			}

		}

		/*
		 * param optional with param ok + mail with param or error if the msg nr
		 * is not there without param ok + all mail
		 */
		private synchronized void handleList(String line) throws IOException, MessagingException {
			Pattern pattern = Pattern.compile(".* ([0-9]+)");
			Matcher m = pattern.matcher(line);
			if (messages.size() > 0) {
				if (m.matches()) {
					int msgnr = Integer.parseInt(m.group(1));
					if (msgnr < messages.size()) {
						write("+OK" + msgnr + " " + messages.get(msgnr).getSize());
					} else {
						write("-ERR message not found");
					}
				} else {
					System.out.println("i am here");
					write("+OK List mail");
					System.out.println("+OK List mail");
					for (int i = 0; i < messages.size(); i++) {
						write(i + " " + messages.get(i).getSize());
						System.out.println(i + " " + messages.get(i).getSize());
					}
					System.out.println(".");
					write(".");
				}
			} else {
				write("-ERR maildrop empty");
			}

		}

		/*
		 * RETR msg ->msg is num of message( param required)
		 */
		private synchronized void handleRetr(String line) throws IOException, MessagingException {
			if (messages.size() > 0) {
				int msgnr = Integer.parseInt(line.substring(5));
				if (msgnr < messages.size()) {
					Message m = messages.get(msgnr);
					write("+OK" + m.getSize() + "Octats");
					write(m.getDescription()); // Hier kommt ganze Mail
					write("."); // Am ende mit . enden
				}
			} else {
				write("-ERR maildrop empty");
			}
		}

		/*
		 * // param msg nr required
		 */
		private synchronized void handleDelete(String line) throws IOException, MessagingException {
			boolean contains = false;
			for (Message message : messages) {
				if (message.getMessageNumber() == Integer.parseInt(line.substring(5))) {
					contains = true;
					message.setFlag(Flag.DELETED, true);
					write("+Ok deleted");
				}
			}
			if (contains == false) {
				write("-ERR only " + messages.size() + "are avialable");
			}
		}

		/*
		 * Just reply with positive response
		 */
		private synchronized void handleNoop() {
			write("+OK");
		}

		/*
		 * Unmark the message that are marked as deleted no param
		 */
		private synchronized void handleReset(String line) throws MessagingException, IOException {
			for (Message message : messages) {
				message.setFlag(Flag.DELETED, false);
			}
			write("+ok");
		}

		/*
		 * if with paramater(msg nr) if msg nr is there OK + message nr + " " +
		 * unique id from message if msg nr is not there -ERR no such message,
		 * only 2 messages in maildrop without parameter multi line, +OK as
		 * first line and each line with message nr + " " + unique id from
		 * message
		 */

		private synchronized void handleUIDL(String line) throws IOException, MessagingException {
			Pattern pattern = Pattern.compile(".* ([0-9]+)");
			Matcher m = pattern.matcher(line);
			if (messages.size() > 0) {
				if (m.matches()) {
					int msgnr = Integer.parseInt(m.group(1));
					if (msgnr < messages.size()) {
						write("+OK" + msgnr + " " + messages.get(msgnr).getReceivedDate());
					} else {
						write("-ERR no such message, only " + " " + messages.size() +  "messages in maildrop");
					}
				} else {
					write("+OK List mail");
					System.out.println("+OK List mail");
					for (int i = 0; i < messages.size(); i++) {
						write(i + " " + messages.get(i).getSize());
						System.out.println(i + " " + messages.get(i).getReceivedDate());
					}
					System.out.println(".");
					write(".");
				}
			} else {
				write("-ERR maildrop empty");
			}

		}

		/*
		 * in translation state Quit command will change the state to UPDATE
		 * delete any marked message and quit
		 */
		private synchronized void handleQuitTrans() {
			state = STATE.UPDATE;
			serving = false;
		}

		/*
		 * Read each char till a line ends
		 */
		private String read() {
			int i;
			StringBuilder sb = new StringBuilder("");
			try {
				sb = new StringBuilder();
				while (0 <= (i = in.read()) && i != '\r' && i != '\n') {
					sb.append((char) i);
					System.out.println(sb.toString());
				}

			} catch (IOException e) {

			}
			System.out.println("end of read");
			return sb.toString();
		}

		private void write(String line) {
			line = line + "\r\n";
			try {
				out.write(line);
				out.flush();
			} catch (IOException e) {
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
				System.out.println(message.getSize() + "in octats");
				System.out.println(message.toString());
				System.out.println("Message " + (i + 1));
				System.out.println("Subject: " + message.getSubject());
				System.out.println("From: " + message.getFrom()[0]);
				try {
					System.out.println("Text: " + message.getContent().toString());
				} catch (IOException e) {
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
