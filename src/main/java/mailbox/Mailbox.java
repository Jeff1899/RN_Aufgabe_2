package mailbox;

import java.io.File;
import java.io.IOException;
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

import proxy.UserAccount;

public class Mailbox {
	private static final String USER_ACCOUNTS = "C:\\Users\\Biraj\\workspace\\RN_Aufgabe_2\\src\\main\\resources\\UserAccounts.txt";
	//private static final String USER_ACCOUNTS = "../RNP_Aufgabe2/src/main/resources/UserAccounts.txt";
	public ArrayList<UserAccount> userAccounts;
	public ArrayList<Message> messages = new ArrayList<>();
	public Map<UserAccount, ArrayList<Message>> userMessages = new HashMap<UserAccount, ArrayList<Message>>();

	public Mailbox() {
		userAccounts = UserAccount.createUserAccounts(new File(USER_ACCOUNTS));

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

	public void checkTime() {
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
