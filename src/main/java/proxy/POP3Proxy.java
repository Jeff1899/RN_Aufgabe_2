package proxy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	private static final String USER_ACCOUNTS = "../RNP_Aufgabe2/src/main/resources/UserAccounts.txt";
	private int _port;
	private ArrayList<UserAccount> userAccounts;
	private Map<UserAccount,ArrayList<Message>> userMessages = new HashMap<UserAccount, ArrayList<Message>>();
	
	public POP3Proxy(String[] args){
		if(args.length != 1){
			System.out.println("ERROR");
		}
		
		_port = Integer.parseInt(args[0]);
		userAccounts = UserAccount.createUserAccounts(new File(USER_ACCOUNTS));
		
		checkTime();
		ServerSocket server = null;
		try {
			server = new ServerSocket(3141);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(true){
			
			try {
				System.out.println("Wait for Thunderbird");
				Socket socket = server.accept();
				System.out.println("connect");
	            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
				
				
//				InputStream in = s.getInputStream();
//				InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
//				BufferedReader brServer = new BufferedReader(isr);
				
				String messageFromClient = in.readLine();
				System.out.println(messageFromClient);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	
	private ArrayList<Message> receivingEmails(UserAccount user){
		 //create properties field
	      Properties properties = new Properties();

	      properties.put("mail.pop3.host", user.get_host());
	      properties.put("mail.pop3.port", user.get_port());
	      properties.put("mail.pop3.starttls.enable", "true");
	      Session emailSession = Session.getDefaultInstance(properties);
	      
	      //create the POP3 store object and connect with the pop server
	      Store store;
	      Message[] messages = null;
	      ArrayList<Message> messageList = new ArrayList<Message>();
	      try {
	    	  store = emailSession.getStore("pop3s");
	    	  
	    	  store.connect(user.get_host(), user.get_port(), user.get_name(), user.get_passwort());
				
	    	  //create the folder object and open it
	    	  Folder emailFolder = store.getFolder("INBOX");
	    	  emailFolder.open(Folder.READ_ONLY);
				
	    	  // retrieve the messages from the folder in an array and print it
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
		        
	      	}catch (NoSuchProviderException e) {
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
				
				for(UserAccount user : userAccounts ){
					if(userMessages.containsKey(user)){
						userMessages.get(user).addAll(receivingEmails(user));
						System.out.println(userMessages.get(user).size());
					}else {
						userMessages.put(user, receivingEmails(user));
					}
				}
				System.out.println("Wait for 30 seconds");
			}
		}, 0, 30 * 1000); 
	}
}
