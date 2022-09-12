package pw.vodes.rimuru.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Util;

public abstract class Command {
	
	private String name;
	private String[] alias;
	private CommandType type;
	private String usage;
	private boolean enabled = true;
	
	public Command(String name, String[] alias, CommandType type) {
		this.name = name;
		this.alias = alias;
		this.type = type;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUsage() {
		return usage;
	}
	
	public void setUsage(String usage) {
		this.usage = usage;
	}
	
	public String[] getAlias() {
		return alias;
	}
	
	public CommandType getType() {
		return type;
	}
	
	public abstract void run(MessageCreateEvent event);
	
	public void initSlashCommand() {
		
	}
	
	public List<String> getSplitMessage(MessageCreateEvent event){
		var list = new ArrayList<String>();
		var m = Util.messageSplitPattern.matcher(event.getMessageContent());
		while(m.find()) {
			var s = m.group(0);
			s = StringUtils.removeStart(s, "\"");
			s = StringUtils.removeEnd(s, "\"");
			s = s.replaceAll("\\\"", "\"");
			list.add(s);
		}
		// Add empty strings for convenience lmao
		while(list.size() < 8) {
			list.add("");
		}
		return list;
	}
	
	public void log(String title, String message) {
		
	}

}
