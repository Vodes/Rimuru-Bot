package pw.vodes.rimuru.command.commands;

import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandRestart extends Command {

	public CommandRestart() {
		super("Restart", new String[] {"restart", "reboot"}, CommandType.mod);
	}

	@Override
	public void run(MessageCreateEvent event) {
		Main.getConfigFile().updateConfig.restart_trigger = event.getMessageLink().toString();
		Main.getFiles().saveConfig();
		Main.getUpdater().restart();
	}

}
