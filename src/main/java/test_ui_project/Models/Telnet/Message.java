
package test_ui_project.Models.Telnet;
public class Message {
	public Message(int code, String text) {
		this.code = code;
		this.text = text;
	}

	private int code;
	private String text;

	public int getCode() {
		return code;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return code + ": " + text;
	}


}
