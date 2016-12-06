package mailbox;

import java.util.ArrayList;

public class ProxyMessage {

	private String messageID;

	private int messageNumber;
	private ArrayList<String> list;
	private int size;	
	private boolean deleteFlag = false;
	private boolean updateStateFlag = false;
	
	public ProxyMessage(ArrayList<String> list, int messageNumber, int size){
		this.list = list;
		this.messageNumber = messageNumber;
		this.messageID = list.get(3).substring(12);
		this.size = size;	}

	public String getMesId() {
		return messageID;
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

	public boolean isUpdateStateFlag() {
		return updateStateFlag;
	}
	public void setUpdateStateFlag(boolean updateStateFlag) {
		this.updateStateFlag = updateStateFlag;
	}

	public ArrayList<String> getList() {
		return list;
	}
}
