package pw.vodes.rimuru.command.commands;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;
import pw.vodes.rimuru.file.AutoRoles;

public class CommandAutorole extends Command {

	public CommandAutorole() {
		super("AutoRole", new String[] {"AutoRole", "AR", "auto-role"}, CommandType.admin);
		setUsage("`!ar <message_url> <role_id/mentioned role>`\n"
				+ "To add with message url and role\n"
				+ "`!ar <channel_id> <message_id> <role_id/mentioned role>`\n"
				+ "To add with channel + message ids and role\n"
				+ "`!ar list`\n"
				+ "To list current setup autoroles\n"
				+ "`!ar <remove/rem/delete/del> <index>`\n"
				+ "To delete an existing autorole with the index number from `list`");
	}

	@Override
	public void run(MessageCreateEvent event) {
		var args = getSplitMessage(event);
		
		if(args.get(1).startsWith("http") && (StringUtils.isNumeric(args.get(2)) || !event.getMessage().getMentionedRoles().isEmpty())) {
			try {
				var msg = Main.api.getMessageByLink(args.get(1)).get().get();
				var role = StringUtils.isNumeric(args.get(2)) ? Main.getServer().getRoleById(args.get(2)).get() : event.getMessage().getMentionedRoles().get(0);
				
				AutoRoles.addAutorole(msg.getServerTextChannel().get().getIdAsString(), msg.getIdAsString(), role.getIdAsString());
				event.getChannel().sendMessage("Autorole added!");
			} catch (Exception e) {
				event.getChannel().sendMessage("Invalid message url or role id!\n" + getUsage());
			}
		} else if(StringUtils.equalsAnyIgnoreCase(args.get(1), "delete", "remove", "rem", "del") && StringUtils.isNumeric(args.get(2))) {
			try {
				var ar = AutoRoles.getAutoroles().get(Integer.parseInt(args.get(2)));
				AutoRoles.removeAutoRole(ar);
				event.getChannel().sendMessage("Autorole removed!");
			} catch (Exception e) {
				event.getChannel().sendMessage("No autorole with that index found!");
			}
		} else if(StringUtils.equalsAnyIgnoreCase(args.get(1), "list", "l")) {
			if(AutoRoles.getAutoroles().isEmpty()) {
				event.getChannel().sendMessage("No autoroles.");
				return;
			}
			event.getChannel().sendMessage(getListEmbed());
			return;
		} else if(StringUtils.isNumeric(args.get(1)) && StringUtils.isNumeric(args.get(2)) && (StringUtils.isNumeric(args.get(3)) || !event.getMessage().getMentionedRoles().isEmpty())) {
			try {
				var msg = Main.getServer().getTextChannelById(args.get(1)).get().getMessageById(args.get(2)).get();
				var role = StringUtils.isNumeric(args.get(3)) ? Main.getServer().getRoleById(args.get(3)).get() : event.getMessage().getMentionedRoles().get(0);
				
				AutoRoles.addAutorole(msg.getServerTextChannel().get().getIdAsString(), msg.getIdAsString(), role.getIdAsString());
				event.getChannel().sendMessage("Autorole added!");
			} catch (Exception e) {
				event.getChannel().sendMessage("Invalid channel/message id or role id!\n" + getUsage());
			}
		} else {
			event.getChannel().sendMessage(getUsage());
			return;
		}
	}
	
	private EmbedBuilder getListEmbed() {
		var embed = new EmbedBuilder().setTitle("Autoroles");
		for(var ar : AutoRoles.getAutoroles()) {
			var role = Main.getServer().getRoleById(ar.role_id).get();
			embed.addField(AutoRoles.getAutoroles().indexOf(ar) + ". " + role.getName(), String.format("https://discord.com/channels/%s/%s/%s", Main.getServer().getIdAsString(), ar.channel_id, ar.message_id));
		}
		return embed;
	}

}
