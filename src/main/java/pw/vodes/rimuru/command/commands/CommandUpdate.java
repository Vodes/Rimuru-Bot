package pw.vodes.rimuru.command.commands;

import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandUpdate extends Command {

	public CommandUpdate() {
		super("Update", new String[] {"update"}, CommandType.admin);
	}

	@Override
	public void run(MessageCreateEvent event) {
		if(Main.getUpdater().run(event)) {
			Main.getUpdater().restart();
		}
	}

}
