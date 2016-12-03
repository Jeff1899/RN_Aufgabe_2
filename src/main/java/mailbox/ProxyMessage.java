package mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

public class ProxyMessage {
	
	private String to;
	private String from;
	private String subject;
	private String messageID;
	private int messageNumber;
	private String date;
	private List<String> content = new ArrayList<>();
	private int size;
	
	private boolean deleteFlag = false;
	private boolean updateStateFlag = false;
	
	public ProxyMessage(ArrayList<String> list, int messageNumber){
		this.messageNumber = messageNumber;
		this.to = list.get(0).substring(4);
		this.from = list.get(1).substring(6);
		this.subject = list.get(2).substring(9);
		this.messageID = list.get(3).substring(12);
		this.date = list.get(4).substring(6);
		for(String l : list){
			System.out.println(l);
		}
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
		return messageID;
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

	public boolean isDeleteFlag() {
		return deleteFlag;
	}

	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}

	public int getMessageNumber() {
		return messageNumber;
	}

	public void setMessageNumber(int messageNumer) {
		this.messageNumber = messageNumer;
	}

	public boolean isUpdateStateFlag() {
		return updateStateFlag;
	}

	public void setUpdateStateFlag(boolean updateStateFlag) {
		this.updateStateFlag = updateStateFlag;
	}

}
