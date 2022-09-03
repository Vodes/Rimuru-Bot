package pw.vodes.rimuru.command.commands;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandStealEmote extends Command {

	public CommandStealEmote() {
		super("Steal Emote", new String[] {"stealemote", "steal-emote", "steal", "se"}, CommandType.everyone);
		setUsage("`!steal <emote/list of emotes>`\nWill take emotes from the message and add to this server.");
	}

	@Override
	public void run(MessageCreateEvent event) {
		if(!Main.getServer().hasAnyPermission(event.getMessageAuthor().asUser().get(), PermissionType.MANAGE_EMOJIS)) {
			event.getChannel().sendMessage("You do not have the permission to manage emotes.");
			return;
		}
		
		var args = getSplitMessage(event);
		if(!args.get(1).isBlank() && !event.getMessage().getCustomEmojis().isEmpty()) {
			if(event.getMessage().getCustomEmojis().size() > 1) {
				for(var emote : event.getMessage().getCustomEmojis()) {
					var name = emote.getName();
					if(emoteWithNameExists(name)) 
						continue;
					
					try {
						var run = Main.getServer().createCustomEmojiBuilder()
						.setImage(emote.getImage())
						.setName(name)
						.create();
						var result = run.join();
						
						if(run.isCompletedExceptionally())
							throw new Exception();
						
						event.getChannel().sendMessage("Added emote " + result.getMentionTag() + "!");
					} catch (Exception e) {
						e.printStackTrace();
						event.getChannel().sendMessage("Failed to add emote!");
					}
				}
			} else {
				var emote = event.getMessage().getCustomEmojis().get(0);
				var name = emote.getName();
				
				if(emoteWithNameExists(name) && (args.get(2).isBlank() ? true : emoteWithNameExists(args.get(2)))) {
					event.getChannel().sendMessage("An emote with that name already exists!");
					return;
				}
				
				try {
					var run = Main.getServer().createCustomEmojiBuilder()
					.setImage(emote.getImage())
					.setName(args.get(2).isBlank() ? name : args.get(2))
					.create();
					var result = run.join();
					
					if(run.isCompletedExceptionally())
						throw new Exception();
					
					event.getChannel().sendMessage("Added emote " + result.getMentionTag() + "!");
				} catch (Exception e) {
					e.printStackTrace();
					event.getChannel().sendMessage("Failed to add emote!");
				}
			}
		} else {
			event.getChannel().sendMessage(getUsage());
		}
	}
	
	private boolean emoteWithNameExists(String name) {
		for(var existing : Main.getServer().getCustomEmojis()) {
			if(name.equalsIgnoreCase(existing.getName())) {
				return true;
			}
		}
		return false;
	}

}
