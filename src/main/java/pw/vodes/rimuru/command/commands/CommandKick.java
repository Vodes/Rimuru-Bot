package pw.vodes.rimuru.command.commands;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandKick extends Command {

	public CommandKick() {
		super("Kick", new String[] {"kick", "k"}, CommandType.mod);
		setUsage("`!kick <user_id/mentioned users>`");
	}

	@Override
	public void run(MessageCreateEvent event) {
		var users = new ArrayList<User>();
		var args = getSplitMessage(event);
		
		ArrayList<Role> modRoles = new ArrayList<Role>();
		for(var roleid : Main.getConfig().mod_roles) {
			modRoles.add(Main.getServer().getRoleById(roleid).get());
		}
		
		if(!event.getMessage().getMentionedUsers().isEmpty()) {
			for(var u : event.getMessage().getMentionedUsers()) {
				var isAdminOrMod = event.getMessageAuthor().isServerAdmin();
				for(var role : modRoles) {
					if(role.hasUser(event.getMessageAuthor().asUser().get()))
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
					var isAdminOrMod = event.getMessageAuthor().isServerAdmin();
					for(var role : modRoles) {
						if(role.hasUser(event.getMessageAuthor().asUser().get()))
							isAdminOrMod = true;
					}
					if(!isAdminOrMod) {
						users.add(u);
					}
				}
			} catch (Exception e) {}
		} else {
			event.getChannel().sendMessage(getUsage());
			return;
		}
		
		for(var u : users) {
			Main.getServer().kickUser(u);
			Main.getStaffActionChannel().sendMessage(new EmbedBuilder().setAuthor(event.getMessageAuthor())
					.setTitle("User kicked")
					.setDescription(u.getDiscriminatedName())
					.setThumbnail(u.getAvatar(4096).getUrl().toString())
					.setFooter("ID: " + u.getIdAsString()));
		}
		event.getMessage().delete();
	}

}
