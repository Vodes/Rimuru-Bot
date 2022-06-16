package pw.vodes.rimuru.command;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.commands.CommandUserInfo;

public class CommandManager {
	
	private ArrayList<Command> commands = new ArrayList<Command>();
	private ArrayList<Role> modRoles = new ArrayList<Role>();
	
	public CommandManager init() {
		commands.add(new CommandUserInfo());
		
		for(var roleid : Main.getConfig().mod_roles) {
			modRoles.add(Main.getServer().getRoleById(roleid).get());
		}
		return this;
	}
	
	public void tryRunCommand(MessageCreateEvent event) {
		var msg = event.getMessageContent();
		for(var cmd : commands) {
			for(var alias : cmd.getAlias()) {
				if(StringUtils.startsWith(msg, Main.getConfig().command_prefix + alias)) {
					cmd.run(event);
					break;
				}
			}
		}
	}
	
	private boolean hasPerms(Command cmd, User user) {
		if(cmd.getType() == CommandType.everyone) {
			return true;
		} else if(cmd.getType() == CommandType.mod) {
			for(var role : modRoles) {
				if(role.hasUser(user)) {
					return true;
				}
			}
		} else {
			if(Main.getServer().isAdmin(user)) {
				return true;
			}
		}
		return false;
	}

}
