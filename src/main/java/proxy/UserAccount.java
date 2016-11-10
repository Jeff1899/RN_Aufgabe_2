package proxy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class UserAccount {


	private String _name;
	private String _passwort;
	private String _host;
	private int _port;
	
	public UserAccount(String name, String passwort, String host, int port){
		_name = name;
		_passwort = passwort;
		_host = host;
		_port = port;
	}
	
	@Override
	public String toString(){
		return _name + " " + _passwort + " " + _host + " " + _port;
		
	}
	
	public static ArrayList<UserAccount> createUserAccounts(File accounts){
		   ArrayList<UserAccount> userList = new ArrayList<UserAccount>();
	        try {
	            BufferedReader rdr = new BufferedReader(new FileReader(accounts));
	            String input = "";
	            while ((input = rdr.readLine()) != null) {
	                String[] userdata = input.split(" ");
	                userList.add(new UserAccount(userdata[0], userdata[1], userdata[2], Integer.parseInt(userdata[3])));
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return userList;
	}
	
	public String get_name() {
		return _name;
	}

	public String get_passwort() {
		return _passwort;
	}

	public String get_host() {
		return _host;
	}

	public int get_port() {
		return _port;
	}
}
