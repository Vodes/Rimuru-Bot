package pw.vodes.rimuru.command.commands;

import java.time.Instant;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.audit.AuditLogs;
import pw.vodes.rimuru.audit.CommandStaffAction;
import pw.vodes.rimuru.audit.StaffActionType;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandBan extends Command {

	public CommandBan() {
		super("Ban", new String[] {"ban", "b"}, CommandType.mod);
		setUsage("`!ban <user_id/mentioned users> \"reason for ban\"`");
	}

	@Override
	public void run(MessageCreateEvent event) {
		var users = new ArrayList<User>();
		var args = getSplitMessage(event);
		
		if(!event.getMessage().getMentionedUsers().isEmpty()) {
			for(var u : event.getMessage().getMentionedUsers()) {
				var isAdminOrMod = Main.getServer().isAdmin(u);
				for(var role : Main.getConfig().getModRoles()) {
					if(role.hasUser(u))
						isAdminOrMod = true;
				}
				
				if(isAdminOrMod)
					continue;
				users.add(u);
			}
		} else if(StringUtils.isNumeric(args.get(1))) {
			try {
				var u = Main.api.getUserById(args.get(1)).get();
				if(Main.getServer().isMember(u)) {
					var isAdminOrMod = Main.getServer().isAdmin(u);
					for(var role : Main.getConfig().getModRoles()) {
						if(role.hasUser(u))
							isAdminOrMod = true;
					}
					if(!isAdminOrMod) {
						users.add(u);
					}
				} else {
					users.add(u);
				}
			} catch (Exception e) { e.printStackTrace(); }
		} else {
			event.getChannel().sendMessage(getUsage());
			return;
		}
		
		var reason = "";
		
		if(users.size() < 2 && !args.get(2).isBlank()) {
			reason = args.get(2);
		}
		
		for(var u : users) {
			AuditLogs.registerStaffAction(new CommandStaffAction(Instant.now().getEpochSecond()
			, u.getIdAsString()
			, event.getMessageAuthor().getIdAsString()
			, reason
			, StaffActionType.ban));
			Main.getServer().banUser(u);
		}
		event.getMessage().delete();
	}

}