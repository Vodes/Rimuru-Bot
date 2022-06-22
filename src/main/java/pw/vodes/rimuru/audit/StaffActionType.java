package pw.vodes.rimuru.audit;

public enum StaffActionType {

	kick("User kicked"),
	ban("User banned"),
	unban("User unbanned"),
	timeout("User muted");
	
	private final String messageTitle;
	
	private StaffActionType(String messageTitle) {
		this.messageTitle = messageTitle;
	}
	
	public String getMessageTitle() {
		return messageTitle;
	}
	
}
