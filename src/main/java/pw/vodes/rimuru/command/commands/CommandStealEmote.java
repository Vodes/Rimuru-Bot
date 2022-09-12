package pw.vodes.rimuru.command.commands;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Util;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandStealEmote extends Command {

	public CommandStealEmote() {
		super("Steal Emote", new String[] {"stealemote", "steal-emote", "steal", "se"}, CommandType.everyone);
		setUsage("`!steal <emote/list of emotes>`\nWill take emotes from the message and add to this server.\n"
				+ "You may also reply to a messsage containing custom emotes. This will only copy the first one.");
	}

	@Override
	public void run(MessageCreateEvent event) {
		if(!event.isServerMessage()) {
			return;
		}
		var server = event.getServer().get();
		
		if(!server.hasAnyPermission(event.getMessageAuthor().asUser().get(), PermissionType.MANAGE_EMOJIS)) {
			event.getChannel().sendMessage("You do not have the permission to manage emotes.");
			return;
		}
		
		var args = getSplitMessage(event);
		if(!args.get(1).isBlank() && !event.getMessage().getCustomEmojis().isEmpty()) {
			if(event.getMessage().getCustomEmojis().size() > 1) {
				for(var emote : event.getMessage().getCustomEmojis()) {
					var name = emote.getName();
					if(emoteWithNameExists(name, server)) 
						continue;
					
					var run = event.getServer().get().createCustomEmojiBuilder()
							.setImage(emote.getImage())
							.setName(name)
							.create();

					run.thenAccept(e -> event.getChannel().sendMessage("Added emote " + e.getMentionTag() + "!")).exceptionally(ex -> {
						Util.reportException(ex, this.getClass().getCanonicalName());
						event.getChannel().sendMessage("Failed to add emote!");
						return null;
					}).join();
				}
			} else {
				var emote = event.getMessage().getCustomEmojis().get(0);
				var name = emote.getName();
				
				if(emoteWithNameExists(name, server) && (args.get(2).isBlank() ? true : emoteWithNameExists(args.get(2), server))) {
					event.getChannel().sendMessage("An emote with that name already exists!");
					return;
				}

				var run = event.getServer().get().createCustomEmojiBuilder()
						.setImage(emote.getImage())
						.setName(args.get(2).isBlank() ? name : args.get(2))
						.create();

				run.thenAccept(e -> event.getChannel().sendMessage("Added emote " + e.getMentionTag() + "!")).exceptionally(ex -> {
					Util.reportException(ex, this.getClass().getCanonicalName());
					event.getChannel().sendMessage("Failed to add emote!");
					return null;
				}).join();
			}
		} else if(event.getMessage().getMessageReference().isPresent()) {
			var referenced = event.getMessage().getMessageReference().get().getMessage().get();
			if(referenced.getCustomEmojis().isEmpty()) {
				event.getChannel().sendMessage(getUsage());
				return;
			}
			
			var emote = referenced.getCustomEmojis().get(0);
			var name = emote.getName();
			
			if(emoteWithNameExists(name, server) && (args.get(1).isBlank() ? true : emoteWithNameExists(args.get(1), server))) {
				event.getChannel().sendMessage("An emote with that name already exists!");
				return;
			}
			
			var run = event.getServer().get().createCustomEmojiBuilder()
					.setImage(emote.getImage())
					.setName(args.get(1).isBlank() ? name : args.get(1))
					.create();
			
			run.thenAccept(e -> event.getChannel().sendMessage("Added emote " + e.getMentionTag() + "!")).exceptionally(ex -> {
				Util.reportException(ex, this.getClass().getCanonicalName());
				event.getChannel().sendMessage("Failed to add emote!");
				return null;
			}).join();
		} else {
			event.getChannel().sendMessage(getUsage());
		}
	}
	
	private boolean emoteWithNameExists(String name, Server serv) {
		for(var existing : serv.getCustomEmojis()) {
			if(name.equalsIgnoreCase(existing.getName())) {
				return true;
			}
		}
		return false;
	}

}
