package pw.vodes.rimuru.file.sub;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AutoModFile {
	
	public String filteredSequence;
	public Punishment punishment;
	public String punishmentVal;
	
	public AutoModFile() {
		
	}
	
	public AutoModFile(String filteredSequence, Punishment punishment, String punishmentVal) {
		this.filteredSequence = filteredSequence;
		this.punishment = punishment;
		this.punishmentVal = punishmentVal;
	}
	
	public static enum Punishment {
		
		TIMEOUT, KICK, BAN;
		
		public static Punishment get(String s) {
			if(s.equalsIgnoreCase("kick")) {
				return KICK;
			} else if(s.equalsIgnoreCase("ban")) {
				return BAN;
			} else if(s.equalsIgnoreCase("timeout")) {
				return TIMEOUT;
			}
			return null;
		}
		
	}

}
