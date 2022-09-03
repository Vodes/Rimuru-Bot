package pw.vodes.rimuru.file;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageEditListener;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.file.sub.AutoModFile;
import pw.vodes.rimuru.file.sub.AutoModFile.Punishment;

public class AutoMod {
	
	private static List<AutoModFile> automods = new ArrayList<AutoModFile>();
	private static ArrayList<TextChannel> excludedChannels = new ArrayList<TextChannel>();
	
	@SuppressWarnings("unchecked")
	public static void load() {
		if(Main.getFiles().exists("automod.json")) {
			try {
				automods = (ArrayList<AutoModFile>)Main.getMapper().readValue(Main.getFiles().read("automod.json"), Main.getMapper().getTypeFactory().constructCollectionType(ArrayList.class, AutoModFile.class));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		
		for(var channel : Main.getServer().getTextChannels()) {
			for(var cat : Main.getConfig().getAutomodExcludedCategories()) {
				if(channel.asCategorizable().get().getCategory().isPresent()) {
					if(channel.asCategorizable().get().getCategory().get().getName().equalsIgnoreCase(cat)) {
						excludedChannels.add(channel);
					}
				}
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
	
	public static MessageCreateListener getAutomodCreateListener() {
		return new MessageCreateListener() {
			@Override
			public void onMessageCreate(MessageCreateEvent event) {
				checkMessage(event.getMessage(), event.getMessageAuthor(), event.getMessageContent());
			}
		};
	}
	
	public static MessageEditListener getAutomodEditListener() {
		return new MessageEditListener() {

			@Override
			public void onMessageEdit(MessageEditEvent event) {
				var msg = event.getMessage().get();
				checkMessage(msg, msg.getAuthor(), msg.getContent());				
			}
		};
	}
	
	private static void checkMessage(Message message, MessageAuthor author, String content) {
		var isAdminOrMod = author.isServerAdmin();
		for(var role : Main.getConfig().getModRoles()) {
			if(role.hasUser(author.asUser().get()))
				isAdminOrMod = true;
		}
		
		if(isAdminOrMod)
			return;
		
		for(var chan : excludedChannels) {
			if(message.getChannel().getId() == chan.getId()) {
				return;
			}
		}
		
		for(var am : AutoMod.getAutomods()) {
			try {
				var pattern = Pattern.compile(am.filteredSequence, Pattern.CASE_INSENSITIVE);
				if(pattern.matcher(content).find() || StringUtils.containsIgnoreCase(content, am.filteredSequence)) {
					var embed = new EmbedBuilder().setTitle("Automod");
					embed.setAuthor(author);
					embed.setUrl(message.getLink().toString());
					embed.setFooter("UserID: " + author.getIdAsString());
					if(am.punishment == null) {
						embed.setDescription("Message deleted:\n```" + content + "```");
						message.delete();
					} else {
						if(am.punishment == Punishment.KICK) {
							Main.getServer().kickUser(author.asUser().get(), "Automod");
							message.delete();
							embed.setDescription("Kicked & Message deleted:\n```" + content + "```");
						} else if(am.punishment == Punishment.BAN) {
							Main.getServer().banUser(author.getId(), 0, "Automod");
							message.delete();
							embed.setDescription("Banned & Message deleted:\n```" + content + "```");
						} else {
							Main.getServer().timeoutUser(author.asUser().get(), Duration.ofMinutes(Long.parseLong(am.punishmentVal)), "Automod");
							message.delete();
							embed.setDescription("Timeouted & Message deleted:\n```" + content + "```");
						}
					}
					Main.getConfig().getStaffActionChannel().sendMessage(embed);
					break;
				}
			} catch (Exception e) {}
		}
	}

}
