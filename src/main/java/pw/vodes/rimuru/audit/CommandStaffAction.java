package pw.vodes.rimuru.audit;

import pw.vodes.rimuru.Main;

public class CommandStaffAction {
	
	public String affectedUser, executingUser, reason = "";
	public long time;
	public StaffActionType type;
	
	public CommandStaffAction() {}
	
	public CommandStaffAction(long time, String affectedUser, String executingUser, String reason, StaffActionType type) {
		this.affectedUser = affectedUser;
		this.executingUser = executingUser;
		this.time = time;
		this.type = type;
		this.reason = reason;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof CommandStaffAction) {
			var csa = (CommandStaffAction)o;
			var diff = Math.abs(time - csa.time);
			if(csa.executingUser.equalsIgnoreCase(Main.api.getYourself().getIdAsString())) {
				return affectedUser.equalsIgnoreCase(csa.affectedUser)
						&& diff < 3
						&& type == csa.type;
			} else {
				return affectedUser.equalsIgnoreCase(csa.affectedUser)
						&& executingUser.equals(csa.executingUser)
						&& diff < 3
						&& type == csa.type
						&& reason.equalsIgnoreCase(csa.reason);
			}
		}
		return super.equals(o);
	}

}
