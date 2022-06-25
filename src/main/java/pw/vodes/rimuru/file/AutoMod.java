package pw.vodes.rimuru.file;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.file.sub.AutoModFile;
import pw.vodes.rimuru.file.sub.AutoModFile.Punishment;
import pw.vodes.rimuru.file.sub.AutoRole;

public class AutoMod {
	
	private static List<AutoModFile> automods = new ArrayList<AutoModFile>();
	
	@SuppressWarnings("unchecked")
	public static void load() {
		if(Main.getFiles().exists("automod.json")) {
			try {
				automods = (ArrayList<AutoModFile>)Main.getMapper().readValue(Main.getFiles().read("automod.json"), Main.getMapper().getTypeFactory().constructCollectionType(ArrayList.class, AutoModFile.class));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void save() {
		try {
			Main.getFiles().write("automod.json", Main.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(automods));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	public static void add(AutoModFile am) {
		automods.add(am);
		save();
	}
	
	public static List<AutoModFile> getAutomods() {
		return automods;
	}
	
	public static void remove(AutoModFile am) {
		automods.remove(am);
		save();
	}
	
	public static MessageCreateListener getAutomodListener() {
		ArrayList<TextChannel> excludedChannels = new ArrayList<TextChannel>();
		for(var channel : Main.getServer().getTextChannels()) {
			for(var cat : Main.getConfig().getAutomodExcludedCategories()) {
				if(channel.asCategorizable().get().getCategory().get().getName().equalsIgnoreCase(cat)) {
					excludedChannels.add(channel);
				}
			}
		}


		return new MessageCreateListener() {
			
			@Override
			public void onMessageCreate(MessageCreateEvent event) {
				var isAdminOrMod = event.getMessageAuthor().isServerAdmin();
				for(var role : Main.getConfig().getModRoles()) {
					if(role.hasUser(event.getMessageAuthor().asUser().get()))
						isAdminOrMod = true;
				}
				
				if(isAdminOrMod)
					return;
				
				for(var chan : excludedChannels) {
					if(event.getChannel().getId() == chan.getId()) {
						return;
					}
				}
				
				for(var am : AutoMod.getAutomods()) {
					try {
						var pattern = Pattern.compile(am.filteredSequence, Pattern.CASE_INSENSITIVE);
						if(pattern.matcher(event.getMessageContent()).find() || StringUtils.containsIgnoreCase(event.getMessageContent(), am.filteredSequence)) {
							var embed = new EmbedBuilder().setTitle("Automod");
							embed.setAuthor(event.getMessageAuthor());
							embed.setUrl(event.getMessageLink().toString());
							embed.setFooter("UserID: " + event.getMessageAuthor().getIdAsString());
							if(am.punishment == null) {
								embed.setDescription("Message deleted:\n```" + event.getMessageContent() + "```");
								event.getMessage().delete();
							} else {
								if(am.punishment == Punishment.KICK) {
									Main.getServer().kickUser(event.getMessageAuthor().asUser().get(), "Automod");
									event.getMessage().delete();
									embed.setDescription("Kicked & Message deleted:\n```" + event.getMessageContent() + "```");
								} else if(am.punishment == Punishment.BAN) {
									Main.getServer().banUser(event.getMessageAuthor().getId(), 0, "Automod");
									event.getMessage().delete();
									embed.setDescription("Banned & Message deleted:\n```" + event.getMessageContent() + "```");
								} else {
									Main.getServer().timeoutUser(event.getMessageAuthor().asUser().get(), Duration.ofMinutes(Long.parseLong(am.punishmentVal)), "Automod");
									event.getMessage().delete();
									embed.setDescription("Timeouted & Message deleted:\n```" + event.getMessageContent() + "```");
								}
							}
							Main.getLogChannel().sendMessage(embed);
							break;
						}
					} catch (Exception e) {}
				}
			}
		};
	}

}
