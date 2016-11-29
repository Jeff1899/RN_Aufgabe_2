package de.haw.rn.proxy.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import de.haw.rn.proxy.POP3_Proxy;

public class Session {
	
	private State state;
	private boolean quitted;
	private String user, password;
	private File[] mails;
	private int totalMailSize;
	private int mailCount;
	private HashSet<File> mailsToDelete;
	
	private final static char CR = 0x0D;
	private final static char LF = 0x0A;
	private final static String CRLF = "" + CR + LF;
	
	public Session() {
		changeState(State.AUTHORISATION);
		quitted = false;
		user = null;
		mailsToDelete = new HashSet<File>();
	}

	public String executeCommand(String commandString) {
		// Command aufteilen in Befehl und Argument
		String[] arguments = commandString.split(" ");
		String command = arguments[0];
		String argument;
		try {
			argument = arguments[1];
		} catch (IndexOutOfBoundsException e) {
			argument = null;
		}

		// Command ausführen
		switch (command.toUpperCase()) {
		case "USER":
			return user(argument);
		case "PASS":
			return pass(argument);
		case "STAT":
			return stat();
		case "LIST":
			return list(argument);
		case "RETR":
			return retr(argument);
		case "DELE":
			return dele(argument);
		case "NOOP":
			return noop();
		case "RSET":
			return rset();
		case "UIDL":
			return uidl(argument);
		case "QUIT":
			return quit();
		default:
			return "-ERR unknown command " + command;
		}
	}
	
	private void initializeMails() {
		File mailFolder = new File(POP3_Proxy.getWorkingDirectory() + "/mails");
		if (mailFolder.exists()) {
			this.mails = mailFolder.listFiles();

			mailCount = mails.length;

			// Die größe aller Mails ins Bytes
			totalMailSize = 0;
			for (File mail : mails) {
				totalMailSize += mail.length();
			}
		}
	}

	private String user(String user) {
		if (!(this.state == State.AUTHORISATION)) {
			return "-ERR no authorisation-state";
		}
		if (user == null) {
			return "-ERR user name required";
		}
		// Prüfen ob der Benutzername richtig ist
		try {
			File accountFile = new File(POP3_Proxy.getWorkingDirectory() + "/accounts/mailClientAccount");
			if (accountFile.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(accountFile));
				String line;
				String[] args;
				while ((this.user == null) && ((line = br.readLine()) != null)) {
					args = line.split(" ");
					this.user = args[0];
					this.password = args[1];

					if (!(user.equals(this.user))) {
						this.user = null;
						this.password = null;
					} else {
						br.close();
						return "+OK " + user + " is a valid mailbox";
					}
					br.close();
				}
			}
			return "-ERR never heard of " + user;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return "-ERR internal error occured. Contact the program-developer and tell him what happend";
		}

		// USER name
		// Arguments:
		// a string identifying a mailbox (required), which is of
		// significance ONLY to the server
		//
		// Restrictions:
		// may only be given in the AUTHORIZATION state after the POP3
		// greeting or after an unsuccessful USER or PASS command

		// Discussion:
		// To authenticate using the USER and PASS command
		// combination, the client must first issue the USER
		// command. If the POP3 server responds with a positive
		// status indicator ("+OK"), then the client may issue
		// either the PASS command to complete the authentication,
		// or the QUIT command to terminate the POP3 session. If
		// the POP3 server responds with a negative status indicator
		// ("-ERR") to the USER command, then the client may either
		// issue a new authentication command or may issue the QUIT
		// command.
		// The server may return a positive response even though no
		// such mailbox exists. The server may return a negative
		// response if mailbox exists, but does not permit plaintext
		// password authentication.

		// Possible Responses:
		// +OK name is a valid mailbox
		// -ERR never heard of mailbox name
		// Examples:
		// C: USER frated
		// S: -ERR sorry, no mailbox for frated here
		// ...
		// C: USER mrose
		// S: +OK mrose is a real hoopy frood
	}

	private String pass(String pass) {
		if (!(this.state == State.AUTHORISATION)) {
			return "-ERR maildrop already locked";
		}
		if (this.password == null) {
			return "-ERR user required";
		}
		if (pass == null) {
			return "-ERR password required";
		}
		// Prüfen ob der Benutzername richtig ist
		if (!(pass.equals(this.password))) {
			return "-ERR invalid password";
		} else {
			changeState(State.TRANSACTION);
			initializeMails();
			return "+OK maildrop locked and ready";
		}

		// PASS string
		// Arguments:
		// a server/mailbox-specific password (required)

		// Restrictions:
		// may only be given in the AUTHORIZATION state immediately
		// after a successful USER command

		// Discussion:
		// When the client issues the PASS command, the POP3 server
		// uses the argument pair from the USER and PASS commands to
		// determine if the client should be given access to the
		// appropriate maildrop.
		// Since the PASS command has exactly one argument, a POP3
		// server may treat spaces in the argument as part of the
		// password, instead of as argument separators.
		//
		// Possible Responses:
		// +OK maildrop locked and ready
		// -ERR invalid password
		// -ERR unable to lock maildrop
		//
		// Examples:
		// C: USER mrose
		// S: +OK mrose is a real hoopy frood
		// C: PASS secret
		// S: -ERR maildrop already locked
		// ...
		// C: USER mrose
		// S: +OK mrose is a real hoopy frood
		// C: PASS secret
		// S: +OK mrose’s maildrop has 2 messages (320 octets)
	}

	private String stat() {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		
		return String.format("+OK %d %d", mailCount, totalMailSize);

		// STAT
		// Arguments: none

		// Restrictions:
		// may only be given in the TRANSACTION state

		// Discussion:
		// The POP3 server issues a positive response with a line
		// containing information for the maildrop. This line is
		// called a "drop listing" for that maildrop.
		// In order to simplify parsing, all POP3 servers are
		// required to use a certain format for drop listings. The
		// positive response consists of "+OK" followed by a single
		// space, the number of messages in the maildrop, a single
		// space, and the size of the maildrop in octets. This memo
		// makes no requirement on what follows the maildrop size.
		// Minimal implementations should just end that line of the
		// response with a CRLF pair. More advanced implementations
		// may include other information.

		// NOTE: This memo STRONGLY discourages implementations
		// from supplying additional information in the drop
		// listing. Other, optional, facilities are discussed
		// later on which permit the client to parse the messages
		// in the maildrop.

		// Note that messages marked as deleted are not counted in
		// either total.

		// Possible Responses:
		// +OK nn mm

		// Examples:
		// C: STAT
		// S: +OK 2 320
	}

	private String list(String msg) {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		File mail = null;
		if (!(msg == null)) {
			try {
				mail = mails[Integer.parseInt(msg) - 1];
			} catch (IndexOutOfBoundsException ioobe) {
				return "-ERR no such message";
			} catch (NumberFormatException nfe) {
				return "-ERR [msg] has to be numeral";
			}
			if (mailsToDelete.contains(mail)) {
				return "-ERR message deleted";
			}
		}
		
		if (!(mail == null)) {
			return "+OK " + msg + " " + mail.length();
		} else {
			StringBuilder answer = new StringBuilder("+OK\r\n");
			for (int i = 0; i < mails.length; i++) {
				mail = mails[i];
				if (!(mailsToDelete.contains(mail))) {
					// Die mails werden als .txt gespeichert,
					// deshalb filtern wir mit >> mail.getName().split("\\.")[0]
					// das Dateiformat (.txt) heraus
					answer.append((i + 1)).append(" ").append(mail.length()).append(CRLF);
				}
			}
			answer.append(".");
			return answer.toString();
		}

		// LIST [msg]
		// Arguments:
		// a message-number (optional), which, if present, may NOT
		// refer to a message marked as deleted

		// Restrictions:
		// may only be given in the TRANSACTION state

		// Discussion:
		// If an argument was given and the POP3 server issues a
		// positive response with a line containing information for
		// that message. This line is called a "scan listing" for
		// that message.

		// If no argument was given and the POP3 server issues a
		// positive response, then the response given is multi-line.
		// After the initial +OK, for each message in the maildrop,
		// the POP3 server responds with a line containing
		// information for that message. This line is also called a
		// "scan listing" for that message. If there are no
		// messages in the maildrop, then the POP3 server responds
		// with no scan listings--it issues a positive response
		// followed by a line containing a termination octet and a
		// CRLF pair.

		// In order to simplify parsing, all POP3 servers are
		// required to use a certain format for scan listings. A
		// scan listing consists of the message-number of the
		// message, followed by a single space and the exact size of
		// the message in octets. Methods for calculating the exact
		// size of the message are described in the "Message Format"
		// section below. This memo makes no requirement on what
		// follows the message size in the scan listing. Minimal
		// implementations should just end that line of the response
		// with a CRLF pair. More advanced implementations may
		// include other information, as parsed from the message.

		// NOTE: This memo STRONGLY discourages implementations
		// from supplying additional information in the scan
		// listing. Other, optional, facilities are discussed
		// later on which permit the client to parse the messages
		// in the maildrop.

		// Note that messages marked as deleted are not listed.

		// Possible Responses:
		// +OK scan listing follows
		// -ERR no such message

		// Examples:
		// C: LIST
		// S: +OK 2 messages (320 octets)
		// S: 1 120
		// S: 2 200
		// S: .
		// ...
		// C: LIST 2
		// S: +OK 2 200
		// ...
		// C: LIST 3
		// S: -ERR no such message, only 2 messages in maildrop
	}

	private String retr(String msg) {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		if (msg == null) {
			return "-ERR no such message";
		}
		File mail = null;
		try {
			mail = mails[Integer.parseInt(msg) - 1];
		} catch (IndexOutOfBoundsException ioobe) {
			return "-ERR no such message";
		} catch (NumberFormatException nfe) {
			return "-ERR [msg] has to be numeral";
		}
		if (mailsToDelete.contains(mails)) {
			return "-ERR message deleted";
		}

		StringBuilder answer = new StringBuilder("+OK message follows").append(CRLF);

		// Mail aus Datei lesen
		try (BufferedReader mailReader = new BufferedReader(new FileReader(mail))) {
			String line;
			while ((line = mailReader.readLine()) != null) {
				if (line.startsWith("..")) {
					line = line.substring(1);
				}
				if (!line.equals(".")) {
					answer.append(line).append(CRLF);
				}
			}
		} catch (Exception e) {
			return "-ERR message lost";
		}
		
		answer.append(".");
		return answer.toString();
		// RETR msg
		// Arguments:
		// a message-number (required) which may NOT refer to a
		// message marked as deleted

		// Restrictions:
		// may only be given in the TRANSACTION state

		// Discussion:
		// If the POP3 server issues a positive response, then the
		// response given is multi-line. After the initial +OK, the
		// POP3 server sends the message corresponding to the given
		// message-number, being careful to byte-stuff the termination
		// character (as with all multi-line responses).

		// Possible Responses:
		// +OK message follows
		// -ERR no such message

		// Examples:
		// C: RETR 1
		// S: +OK 120 octets
		// S: <the POP3 server sends the entire message here>
		// S: .
	}

	private String dele(String msg) {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		if (msg == null) {
			return "-ERR no such message";
		}
		
		try {
			File mail = mails[Integer.parseInt(msg) - 1];
			if (mailsToDelete.add(mail)) {
				mailCount--;
				totalMailSize -= mail.length();
				return "+OK message " + msg + " deleted";
			} else {
				return "-ERR message " + msg + " already deleted";
			}
		} catch (IndexOutOfBoundsException ioobe) {
			return "-ERR no such message";
		} catch (NumberFormatException nfe) {
			return "-ERR [msg] has to be numeral";
		}
		// DELE msg
		// Arguments:
		// a message-number (required) which may NOT refer to a
		// message marked as deleted

		// Restrictions:
		// may only be given in the TRANSACTION state

		// Discussion:
		// The POP3 server marks the message as deleted. Any future
		// reference to the message-number associated with the message
		// in a POP3 command generates an error. The POP3 server does
		// not actually delete the message until the POP3 session
		// enters the UPDATE state.

		// Possible Responses:
		// +OK message deleted
		// -ERR no such message

		// Examples:
		// C: DELE 1
		// S: +OK message 1 deleted
		// ...
		// C: DELE 2
		// S: -ERR message 2 already deleted
	}

	private String noop() {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		return "+OK";

		// NOOP
		// Arguments: none

		// Restrictions:
		// may only be given in the TRANSACTION state

		// Discussion:
		// The POP3 server does nothing, it merely replies with a
		// positive response.

		// Possible Responses:
		// +OK

		// Examples:
		// C: NOOP
		// S: +OK
	}

	private String rset() {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		
		mailsToDelete.clear();
		mailCount = mails.length;
		totalMailSize = 0;
		for (File mail : mails) {
			totalMailSize += mail.length();
		}
		return "+OK maildrop has " + mailCount + " messages (" + totalMailSize + ")";
		
		// RSET
		// Arguments: none
		
		// Restrictions:
		// may only be given in the TRANSACTION state
		
		// Discussion:
		// If any messages have been marked as deleted by the POP3
		// server, they are unmarked. The POP3 server then replies
		// with a positive response.
		
		// Possible Responses:
		// +OK
		
		// Examples:
		// C: RSET
		// S: +OK maildrop has 2 messages (320 octets)
	}

	private String uidl(String msg) {
		if (!(this.state == State.TRANSACTION)) {
			return "-ERR no transaction-state";
		}
		File mail = null;
		if (!(msg == null)) {
			try {
				mail = mails[Integer.parseInt(msg) - 1];
			} catch (IndexOutOfBoundsException ioobe) {
				return "-ERR no such message";
			} catch (NumberFormatException nfe) {
				return "-ERR [msg] has to be numeral";
			}
			if (mailsToDelete.contains(mail)) {
				return "-ERR message deleted";
			}
		}
		
		if (!(mail == null)) {
			return "+OK " + msg + " " + mail.getName().split("\\.")[0];
		} else {
			StringBuilder answer = new StringBuilder("+OK\r\n");
			for (int i = 0; i < mails.length; i++) {
				mail = mails[i];
				if (!(mailsToDelete.contains(mail))) {
					// Die mails werden als .txt gespeichert,
					// deshalb filtern wir mit >> mail.getName().split("\\.")[0]
					// das Dateiformat (.txt) heraus
					answer.append((i + 1)).append(" ").append(mail.getName().split("\\.")[0]).append(CRLF);
				}
			}
			answer.append(".");
			return answer.toString();
		}

		// UIDL [msg]
		// Arguments:
		// a message-number (optional), which, if present, may NOT
		// refer to a message marked as deleted

		// Restrictions:
		// may only be given in the TRANSACTION state.

		// Discussion:
		// If an argument was given and the POP3 server issues a positive
		// response with a line containing information for that message.
		// This line is called a "unique-id listing" for that message.
		// If no argument was given and the POP3 server issues a positive
		// response, then the response given is multi-line. After the
		// initial +OK, for each message in the maildrop, the POP3 server
		// responds with a line containing information for that message.
		// This line is called a "unique-id listing" for that message.
		// In order to simplify parsing, all POP3 servers are required to
		// use a certain format for unique-id listings. A unique-id
		// listing consists of the message-number of the message,
		// followed by a single space and the unique-id of the message.
		// No information follows the unique-id in the unique-id listing.
		// The unique-id of a message is an arbitrary server-determined
		// string, consisting of one to 70 characters in the range 0x21
		// to 0x7E, which uniquely identifies a message within a
		// maildrop and which persists across sessions. This
		// persistence is required even if a session ends without
		// entering the UPDATE state. The server should never reuse an
		// unique-id in a given maildrop, for as long as the entity
		// using the unique-id exists.

		// Note that messages marked as deleted are not listed.
		// While it is generally preferable for server implementations
		// to store arbitrarily assigned unique-ids in the maildrop,
		// this specification is intended to permit unique-ids to be
		// calculated as a hash of the message. Clients should be able
		// to handle a situation where two identical copies of a
		// message in a maildrop have the same unique-id.

		// Possible Responses:
		// +OK unique-id listing follows
		// -ERR no such message

		// Examples:
		// C: UIDL
		// S: +OK
		// S: 1 whqtswO00WBw418f9t5JxYwZ
		// S: 2 QhdPYR:00WBw1Ph7x7
		// S: .
		// ...
		// C: UIDL 2
		// S: +OK 2 QhdPYR:00WBw1Ph7x7
		// ...
		// C: UIDL 3
		// S: -ERR no such message, only 2 messages in maildrop
	}

	private String quit() {
		if (this.state == State.AUTHORISATION) {
			quitted = true;
			return "+OK POP3 server signing off";
		} else if (this.state == State.TRANSACTION) {
			changeState(State.UPDATE);
			boolean errorOccurred = false;
			// Markierte Mails löschen
			for (File mail : mailsToDelete) {
				try {
					Files.delete(mail.toPath());
				} catch (IOException e) {
					errorOccurred = true;
				}
			}
			
			if (errorOccurred) {
				return "-ERR some deleted messages not removed";
			} else {
				quitted = true;
				initializeMails();
				return "+OK POP3 server signing off " + (mailCount == 0 ? "(maildrop empty)" : "(" + mailCount + " messages left)");
			}
		} else {
			return "-ERR";
		}
		
		// The UPDATE State
		// When the client issues the QUIT command from the TRANSACTION state,
		// the POP3 session enters the UPDATE state. (Note that if the client
		// issues the QUIT command from the AUTHORIZATION state, the POP3
		// session terminates but does NOT enter the UPDATE state.)
		// If a session terminates for some reason other than a client-issued
		// QUIT command, the POP3 session does NOT enter the UPDATE state and
		// MUST not remove any messages from the maildrop.

		// QUIT
		// Arguments: none

		// Restrictions: none
		
		// Discussion:
		// The POP3 server removes all messages marked as deleted
		// from the maildrop and replies as to the status of this
		// operation. If there is an error, such as a resource
		// shortage, encountered while removing messages, the
		// maildrop may result in having some or none of the messages
		// marked as deleted be removed. In no case may the server
		// remove any messages not marked as deleted.
		// Whether the removal was successful or not, the server
		// then releases any exclusive-access lock on the maildrop
		// and closes the TCP connection.

		// Possible Responses:
		// +OK
		// -ERR some deleted messages not removed

		// Examples:
		// C: QUIT
		// S: +OK dewey POP3 server signing off (maildrop empty)
		// ...
		// C: QUIT
		// S: +OK dewey POP3 server signing off (2 messages left)
		
		// AUTHORIZATION state:
		// QUIT
		// Arguments: none
		// Restrictions: none
		// Possible Responses:
		// +OK
		// Examples:
		// C: QUIT
		// S: +OK dewey POP3 server signing off
	}
	
	private void changeState(State s) {
		this.state = s;
		System.out.println("State: " + this.state);
	}
	
	public boolean isQuitted() {
		return quitted;
	}
	
 	private enum State {
		AUTHORISATION,
		// The AUTHORIZATION State
		// Once the TCP connection has been opened by a POP3 client, the POP3
		// server issues a one line greeting. This can be any positive
		// response. An example might be:
		// S:
		// +OK POP3 server ready
		// The POP3 session is now in the AUTHORIZATION state. The client must
		// now identify and authenticate itself to the POP3 server. Two
		// possible mechanisms for doing this are described in this document,
		// the USER and PASS command combination and the APOP command. Both
		// mechanisms are described later in this document. Additional
		// authentication mechanisms are described in [RFC1734]. While there is
		// no single authentication mechanism that is required of all POP3
		// servers, a POP3 server must of course support at least one
		// authentication mechanism.
		// Once the POP3 server has determined through the use of any
		// authentication command that the client should be given access to the
		// appropriate maildrop, the POP3 server then acquires an exclusive-
		// access lock on the maildrop, as necessary to prevent messages from
		// being modified or removed before the session enters the UPDATE state.
		// If the lock is successfully acquired, the POP3 server responds with a
		// positive status indicator. The POP3 session now enters the
		// TRANSACTION state, with no messages marked as deleted. If the
		// maildrop cannot be opened for some reason (for example, a lock can
		// not be acquired, the client is denied access to the appropriate
		// maildrop, or the maildrop cannot be parsed), the POP3 server responds
		// with a negative status indicator. (If a lock was acquired but the
		// POP3 server intends to respond with a negative status indicator, the
		// POP3 server must release the lock prior to rejecting the command.)
		// After returning a negative status indicator, the server may close the
		// connection. If the server does not close the connection, the client
		// may either issue a new authentication command and start again, or the
		// client may issue the QUIT command.
		// After the POP3 server has opened the maildrop, it assigns a message-
		// number to each message, and notes the size of each message in octets.
		// The first message in the maildrop is assigned a message-number of
		// "1", the second is assigned "2", and so on, so that the nth message
		// in a maildrop is assigned a message-number of "n". In POP3 commands
		// and responses, all message-numbers and message sizes are expressed in
		// base-10 (i.e., decimal).
		TRANSACTION,
		// The TRANSACTION State
		// Once the client has successfully identified itself to the POP3 server
		// and the POP3 server has locked and opened the appropriate maildrop,
		// the POP3 session is now in the TRANSACTION state. The client may now
		// issue any of the following POP3 commands repeatedly. After each
		// command, the POP3 server issues a response. Eventually, the client
		// issues the QUIT command and the POP3 session enters the UPDATE state.
		// Here are the POP3 commands valid in the TRANSACTION state:
		// STAT, LIST, RETR, DELE, NOOP, RSET
		UPDATE
		// The UPDATE State
		// When the client issues the QUIT command from the TRANSACTION state,
		// the POP3 session enters the UPDATE state. (Note that if the client
		// issues the QUIT command from the AUTHORIZATION state, the POP3
		// session terminates but does NOT enter the UPDATE state.)
		// If a session terminates for some reason other than a client-issued
		// QUIT command, the POP3 session does NOT enter the UPDATE state and
		// MUST not remove any messages from the maildrop.
	}
}
