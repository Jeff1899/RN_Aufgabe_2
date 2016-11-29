package de.haw.rn.proxy.util.filemodder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.haw.rn.proxy.POP3_Proxy;
import de.haw.rn.proxy.client.entities.Account;

public class AccountGetter {
	
	private final static int COUNT_ARGS = 4;
	private final static String DIR = POP3_Proxy.getWorkingDirectory() + "/accounts";
	private final static String FILENAME = "account.txt";
	
	public static List<Account> getAllAccounts() throws IOException{
		List<Account> accountList = new ArrayList<>();
		
		File file = new File(DIR,FILENAME);
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		String line;
		while((line = br.readLine()) != null){
			try{
				String[] arr = line.split(" ");
				if(arr.length != COUNT_ARGS){
					continue;
				}
				for(String s : arr){
					System.out.println(s);
				}
				
				String name = arr[0];
				String pass = arr[1];
				String host = arr[2];
				int port = Integer.parseInt(arr[3]);
				
				Account account = new Account(name,pass,host,port);
				accountList.add(account);
			}catch(Exception e){
				System.out.println("ERROR: SyntaxFehler in der Accountkonfigurationsdatei");
			}
			
		}
		br.close();
		return accountList;
		
	}

}
