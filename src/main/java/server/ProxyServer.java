package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mailbox.ProxyMessage;
import proxy.POP3Proxy;

public class ProxyServer extends Thread {
	private String username;
	private String password;
	private boolean serving = true;
	private Socket socket;
	private STATE state = STATE.AUTH;
	private OutputStreamWriter out;
	private InputStreamReader in;
	private BufferedReader reader;
	private POP3Proxy proxy;

	public ProxyServer(Socket socket, String username, String password, POP3Proxy proxy) {
		this.socket = socket;
		this.username = username;
		this.password = password;
		this.proxy = proxy;
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
					transaction();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void authenticate() throws IOException {
		// String line = read();
		String line = reader.readLine();
		System.out.println("Authentification : " + line);
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

	private synchronized void transaction() throws IOException {
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
	private synchronized void handleStat()  {
		int size = 0;
		if (proxy.getMessagesList().size() > 0) {
			for (int i = 0; i < proxy.getMessagesList().size() ; i++) {
				size = proxy.getMessagesList().get(i).getSize();
			}
			write("+OK " + proxy.getMessagesList().size() + " " + size);
		} else {
			// Dann ist sie eben leer, muss ja kein Fehler sein TODO
			write("+OK maildrop empty");
		}
//		if (mailbox.messages.size() > 0) {
//			for (int i = 0; i < mailbox.messages.size(); i++) {
//				size = size + mailbox.messages.get(i).getSize();
//			}
//			write("+OK " + mailbox.messages.size() + " " + size);
//		} else {
//			write("-ERR maildrop empty");
//		}

	}

	/*
	 * param optional with param ok + mail with param or error if the msg nr
	 * is not there without param ok + all mail
	 */
	private synchronized void handleList(String line) throws IOException {
		Pattern pattern = Pattern.compile(".* ([0-9]+)");
		Matcher m = pattern.matcher(line);
		if (proxy.getMessagesList().size() > 0) {
			if (m.matches()) {
				int msgnr = Integer.parseInt(m.group(1));
				
				if (msgnr < proxy.getMessagesList().size()) {
					write("+OK" + msgnr + " " + proxy.getMessagesList().get(msgnr).getSize());
				} else {
					write("-ERR message not found");
				}
			} else {
				int size = 0;
				for(ProxyMessage message : proxy.getMessagesList()){
					size = size + message.getSize();
				}
				write("+OK " + proxy.getMessagesList().size() + " messages" + "( " + size +"octats)");
				for (int i = 0; i < proxy.getMessagesList().size(); i++) {
					write((i + 1) + " " + proxy.getMessagesList().get(i).getSize());
					
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
	private synchronized void handleRetr(String line) throws IOException {
		if (proxy.getMessagesList().size() > 0) {
			int msgnr = Integer.parseInt(line.substring(5));
			msgnr = msgnr - 1;
			if (msgnr < proxy.getMessagesList().size()) {
				ProxyMessage m = proxy.getMessagesList().get(msgnr);
				write("+OK");
				for(String str: m.getList()){
					System.out.println(str);
					write(str);
				}
				
//				write("From: " + "" + m.getFrom());
//				write("Date: " + "" + m.getDate());
//				write("Message-ID: " + "" + m.getMesId());
//				write("Subject: " + "" + m.getSubject());
//				write("To: " + "" + m.getFrom());
//				write("");
//				for(String str: m.getContent()){
//					write(str);
//				}
				write("."); // Am ende mit . enden
			}
		} else {
			write("-ERR maildrop empty");
		}
	}

	/*
	 * // param msg nr required
	 */
	private synchronized void handleDelete(String line) throws IOException {
		boolean contains = false;
		for (ProxyMessage message : proxy.getMessagesList()) {
//			TODO
			if (message.getMessageNumber() == Integer.parseInt(line.substring(5))) {
				contains = true;
				message.setDeleteFlag(true);
				System.out.println("Set FLAG " + message.isDeleteFlag());
				write("+Ok message marked for delete");
				break;
			}
		}
		if (contains == false) {
			write("-ERR only " + proxy.getMessagesList().size() + "are avialable");
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
	private synchronized void handleReset(String line) throws IOException {
		for (ProxyMessage message : proxy.getMessagesList()) {
//			TODO
//			message.setFlag(Flag.DELETED, false);
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

	private synchronized void handleUIDL(String line) throws IOException {
		Pattern pattern = Pattern.compile(".* ([0-9]+)");
		Matcher m = pattern.matcher(line);
		if (proxy.getMessagesList().size() > 0) {
			if (m.matches()) {
				int msgnr = Integer.parseInt(m.group(1));
				if (msgnr < proxy.getMessagesList().size()) {
					write("+OK" + msgnr + " " + proxy.getMessagesList().get(msgnr).getMesId());
				} else {
					write("-ERR no such message, only " + " " + proxy.getMessagesList().size() + "messages in maildrop");
				}
			} else {
				write("+OK List mail");
				for (int i = 0; i < proxy.getMessagesList().size(); i++) {
					write((i + 1) + " "  + proxy.getMessagesList().get(i).getMesId());
					System.out.println(proxy.getMessagesList().get(i).getMesId());
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
	private synchronized void handleQuitTrans() throws IOException {
		ArrayList<ProxyMessage> toRemove = new ArrayList<ProxyMessage>();
		for(ProxyMessage message : proxy.getMessagesList()){
			if(message.isDeleteFlag() && message.isUpdateStateFlag()){
				toRemove.add(message);
				System.out.println("Delete this MESSAGE");
			}else{
				message.setUpdateStateFlag(true);
				System.out.println("Message to UPDATE STATE");
			}
		}
		proxy.getMessagesList().removeAll(toRemove);
		socket.close();
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
