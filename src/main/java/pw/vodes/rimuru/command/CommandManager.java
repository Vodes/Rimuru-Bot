package pw.vodes.rimuru.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.commands.CommandAutomod;
import pw.vodes.rimuru.command.commands.CommandAutorole;
import pw.vodes.rimuru.command.commands.CommandBan;
import pw.vodes.rimuru.command.commands.CommandClear;
import pw.vodes.rimuru.command.commands.CommandHelp;
import pw.vodes.rimuru.command.commands.CommandKick;
import pw.vodes.rimuru.command.commands.CommandRSS;
import pw.vodes.rimuru.command.commands.CommandRestart;
import pw.vodes.rimuru.command.commands.CommandStealEmote;
import pw.vodes.rimuru.command.commands.CommandUpdate;
import pw.vodes.rimuru.command.commands.CommandUserInfo;

public class CommandManager {
	
	private ArrayList<Command> commands = new ArrayList<Command>();
	
	public CommandManager init() {
		commands.add(new CommandUserInfo());
		commands.add(new CommandClear());
		commands.add(new CommandAutorole());
		commands.add(new CommandAutomod());
		commands.add(new CommandBan());
		commands.add(new CommandKick());
		commands.add(new CommandStealEmote());
		commands.add(new CommandHelp());
		commands.add(new CommandRSS());
		commands.add(new CommandUpdate());
		commands.add(new CommandRestart());
		loadStatus();
		
		for(var cmd : commands) {
			if(cmd.isEnabled()) {
				cmd.initSlashCommand();
			}
		}
		return this;
	}
	
	public void createStatusFile() {
		var list = new ArrayList<CommandStatus>();
		for(var cmd : commands) {
			var status = new CommandStatus();
			status.name = cmd.getName();
			status.enabled = cmd.isEnabled();
			list.add(status);
		}
		try {
			Main.getFiles().write("commands.json", Main.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(list));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadStatus() {
		if(!Main.getFiles().exists("commands.json")) {
			createStatusFile();
			return;
		}
		try {
			var list = (List<CommandStatus>)Main.getMapper().readValue(Main.getFiles().read("commands.json"), Main.getMapper().getTypeFactory().constructCollectionType(List.class, CommandStatus.class));
			for(var status : list) {
				for(var cmd : commands) {
					if(cmd.getName().equalsIgnoreCase(status.name))
						cmd.setEnabled(status.enabled);
				}
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	public void tryRunCommand(MessageCreateEvent event) {
		var msg = event.getMessageContent();
		for(var cmd : commands) {
			if(!cmd.isEnabled())
				continue;
			for(var alias : cmd.getAlias()) {
				if(StringUtils.startsWithIgnoreCase(msg, Main.getConfig().getCommandPrefix() + alias) && hasPerms(cmd, event.getMessageAuthor().asUser().get())) {
					cmd.run(event);
					break;
				}
			}
		}
	}
	
	public ArrayList<Command> getCommands() {
		return commands;
	}
	
	private boolean hasPerms(Command cmd, User user) {
		if(Main.getServer().isAdmin(user) || user.getId() == Main.api.getOwnerId()) {
			return true;
		}
		var returnB = false;
		if(cmd.getType() == CommandType.everyone) {
			returnB = true;
		} else if(cmd.getType() == CommandType.mod) {
			for(var role : Main.getConfig().getModRoles()) {
				if(role.hasUser(user)) {
					returnB = true;
				}
			}
		}
		return returnB;
	}
	
}
