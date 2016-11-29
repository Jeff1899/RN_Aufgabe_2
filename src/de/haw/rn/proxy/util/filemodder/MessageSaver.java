package de.haw.rn.proxy.util.filemodder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import de.haw.rn.proxy.POP3_Proxy;

public class MessageSaver {
	
	private final static String DIR = POP3_Proxy.getWorkingDirectory() + "/mails";

	public MessageSaver() throws IOException {
	}
	
	public void saveMessage(String message) throws IOException{
		String uid = UUID.randomUUID().toString();
		File file = new File(POP3_Proxy.getWorkingDirectory() + "/mails",uid+".txt");
		File dir = new File("mails");
		if(!dir.exists()){
			dir.mkdir();
		}
		
		while(file.exists()){
			uid = UUID.randomUUID().toString();
			file = new File(DIR,uid+".txt");
		}
		
		FileWriter writer = new FileWriter(file);
		
		file.createNewFile();
		writer.write(message);
		writer.close();
	}
}
