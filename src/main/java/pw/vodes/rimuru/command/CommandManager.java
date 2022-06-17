package pw.vodes.rimuru.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.commands.CommandClear;
import pw.vodes.rimuru.command.commands.CommandUserInfo;

public class CommandManager {
	
	private ArrayList<Command> commands = new ArrayList<Command>();
	private ArrayList<Role> modRoles = new ArrayList<Role>();
	
	public CommandManager init() {
		commands.add(new CommandUserInfo());
		commands.add(new CommandClear());
		
		for(var roleid : Main.getConfig().mod_roles) {
			modRoles.add(Main.getServer().getRoleById(roleid).get());
		}
		
		loadStatus();
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
				if(StringUtils.startsWith(msg, Main.getConfig().command_prefix + alias) && hasPerms(cmd, event.getMessageAuthor().asUser().get())) {
					cmd.run(event);
					break;
				}
			}
		}
	}
	
	private boolean hasPerms(Command cmd, User user) {
		if(Main.getServer().isAdmin(user)) {
			return true;
		}
		var returnB = false;
		if(cmd.getType() == CommandType.everyone) {
			returnB = true;
		} else if(cmd.getType() == CommandType.mod) {
			for(var role : modRoles) {
				if(role.hasUser(user)) {
					returnB = true;
				}
			}
		}
		return returnB;
	}
	
}
