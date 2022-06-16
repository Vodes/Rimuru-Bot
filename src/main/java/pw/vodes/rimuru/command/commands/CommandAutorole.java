package pw.vodes.rimuru.command.commands;

import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandAutorole extends Command {

	public CommandAutorole() {
		super("AutoRole", new String[] {"AutoRole", "AR", "auto-role"}, CommandType.admin);
	}

	@Override
	public void run(MessageCreateEvent event) {
		
	}

}
