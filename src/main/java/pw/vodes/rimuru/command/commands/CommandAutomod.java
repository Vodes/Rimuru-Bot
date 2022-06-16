package pw.vodes.rimuru.command.commands;

import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandAutomod extends Command {

	public CommandAutomod() {
		super("Automod", new String[] {"automod", "filter"}, CommandType.mod);
	}

	@Override
	public void run(MessageCreateEvent event) {
		
	}

}
