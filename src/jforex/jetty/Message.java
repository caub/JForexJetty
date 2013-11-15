package jforex.jetty;

public class Message {

	String type;
	Object data;
	
	public Message(String type, Object data) {
		this.type = type;
		this.data = data;
	}
	
}
