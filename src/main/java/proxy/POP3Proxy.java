package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;

import mailbox.Mailbox;

/**
 * 
 * @author Jeff
 *
 */
public class POP3Proxy {

	private int port;
	private String username;
	private String password;
	private ArrayList<Message> messages = new ArrayList<>();
	public static Map<UserAccount, ArrayList<Message>> userMessages = new HashMap<UserAccount, ArrayList<Message>>();
	public int clientAnzahl = 0;
	public boolean serving = true;
	public Mailbox mailbox;

	public POP3Proxy(String[] args) {
		if (args.length != 3) {
			System.out.println("ERROR");
		}

		port = Integer.parseInt(args[0]);
		username = args[1];
		password = args[2];
		mailbox = new Mailbox();
	    mailbox.checkTime();
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Connection failed");
		}

		while (serving) {
			try {
				System.out.println("POP3-Proxy");
				if (clientAnzahl <= 3) {
					Socket socket = server.accept();
					new POP3ServerThread(socket).start();
					clientAnzahl++;
				}
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
						System.out.println("TRANS");
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
				boolean userCorrect = handleUser(line);
				if (userCorrect) {
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
			write("-ERR CAPA");
		}

		private synchronized boolean handleUser(String line) {
			if (line.substring(5).equals(username)) {
				write("+OK Please enter password");
				return true;
			} else {
				write("-ERR Wrong Username");
				return false;
			}

		}

		private synchronized void handlePassword(String line) {
			if (line.substring(5).equals(password)) {
				write("+OK mailbox locked and ready");
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
			String line = reader.readLine();
			System.out.println("TRANSACTION : " + line);
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
			if (mailbox.messages.size() > 0) {
				for (int i = 0; i < mailbox.messages.size(); i++) {
					size = size + mailbox.messages.get(i).getSize();
				}
				write("+OK " + mailbox.messages.size() + " " + size);
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
			if (mailbox.messages.size() > 0) {
				if (m.matches()) {
					int msgnr = Integer.parseInt(m.group(1));
					if (msgnr < mailbox.messages.size()) {
						write("+OK" + msgnr + " " + mailbox.messages.get(msgnr).getSize());
					} else {
						write("-ERR message not found");
					}
				} else {
					write("+OK " + mailbox.messages.size() + " messages");
					for (int i = 0; i < mailbox.messages.size(); i++) {
						write((i + 1) + " " + mailbox.messages.get(i).getSize());
					}
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
			if (mailbox.messages.size() > 0) {
				int msgnr = Integer.parseInt(line.substring(5));
				msgnr = msgnr - 1;
				if (msgnr < mailbox.messages.size()) {
					Message m = mailbox.messages.get(msgnr);

					write("+OK " + m.getSize() + " Octats");
					write("" + m.getFrom()[0]);
					write("rnwise2016@gmail.com");
					write(m.getSubject());
					write(m.getContent().toString()); // Hier kommt ganze Mail
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
			for (Message message : mailbox.messages) {
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
			for (Message message : mailbox.messages) {
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
			if (mailbox.messages.size() > 0) {
				if (m.matches()) {
					int msgnr = Integer.parseInt(m.group(1));
					if (msgnr < messages.size()) {
						write("+OK" + msgnr + " " + messages.get(msgnr).getReceivedDate());
					} else {
						write("-ERR no such message, only " + " " + mailbox.messages.size() + "messages in maildrop");
					}
				} else {
					write("+OK List mail");
					for (int i = 0; i < mailbox.messages.size(); i++) {
						write((i + 1) + " " + mailbox.messages.get(i).getMessageNumber());
					}
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
			// System.err.println(line);
			line = line + "\r\n";
			try {
				out.write(line);
				out.flush();
			} catch (IOException e) {
			}
		}
	}
}
