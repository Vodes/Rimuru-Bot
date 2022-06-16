package pw.vodes.rimuru.command;

import org.javacord.api.event.message.MessageCreateEvent;

public abstract class Command {
	
	private String name;
	private String[] alias;
	private CommandType type;
	private String usage;
	
	public Command(String name, String[] alias, CommandType type) {
		this.name = name;
		this.alias = alias;
		this.type = type;
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

}
