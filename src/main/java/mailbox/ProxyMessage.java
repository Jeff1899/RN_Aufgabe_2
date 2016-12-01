package mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

public class ProxyMessage {
	
	private String to;
	private String from;
	private String subject;
	private String mesId;
	private String date;
	private List<String> content = new ArrayList<>();
	private int size;
	
	public ProxyMessage(ArrayList<String> list){
		this.to = list.get(0).substring(4);
		this.from = list.get(1).substring(6);
		this.subject = list.get(2).substring(10);
		this.mesId = list.get(3).substring(12);
		this.date = list.get(4).substring(6);
		this.size = Integer.parseInt(list.get(list.size() - 1));
	
		content = list.subList(10, list.size() -1);
	}

	public String getTo() {
		return to;
	}

	public String getFrom() {
		return from;
	}

	public String getSubject() {
		return subject;
	}

	public String getMesId() {
		return mesId;
	}

	public String getDate() {
		return date;
	}

	public List<String> getContent() {
		return content;
	}

	public int getSize() {
		return size;
	}

}
