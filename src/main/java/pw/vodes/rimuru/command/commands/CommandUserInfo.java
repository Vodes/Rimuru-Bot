package pw.vodes.rimuru.command.commands;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandUserInfo extends Command {

	public CommandUserInfo() {
		super("UserInfo", new String[] {"userinfo", "ui", "av", "avatar"}, CommandType.everyone);
		setUsage("!userinfo <user-id/mention> (--server/-s)");
	}
	
	@Override
	public void initSlashCommand() {
//		var command = SlashCommand.with(getName(), "Shows profile picture and other info about a user")
//				.setDefaultEnabledForEveryone().setEnabledInDms(true)
//				.addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "user", ""))
//				.createGlobal(Main.api);
	}

	@Override
	public void run(MessageCreateEvent event) {
		User user = null;
		var message = event.getMessageContent();
		var serverPfp = event.getMessageContent().toLowerCase().contains("--server") || event.getMessageContent().toLowerCase().contains("-s");
		if(serverPfp) {
			message = StringUtils.removeIgnoreCase(message, "--server").trim();
			message = StringUtils.removeIgnoreCase(message, "-s").trim();
		}
		Server serv = event.isServerMessage() ? event.getServer().get() : null;
		if(!event.getMessage().getMentionedUsers().isEmpty()) {
			user = event.getMessage().getMentionedUsers().get(0);
		} else {
			if(message.split(" ").length < 2 && !event.getMessage().getReferencedMessage().isPresent()) {
				sendInfo(event, event.getMessageAuthor().asUser().get(), serverPfp);
				return;
			}
			if(event.getMessage().getReferencedMessage().isPresent()) {
				sendInfo(event, event.getMessage().getReferencedMessage().get().getAuthor().asUser().get(), serverPfp);
				return;
			}
			CompletableFuture<User> userFu = event.getApi().getUserById(message.split(" ")[1]);
			if(userFu.isCancelled()) {
				event.getChannel().sendMessage("Cant find a user for that ID.");
				return;
			}
			try {
				if(userFu.get() == null) {
					event.getChannel().sendMessage("Cant find a user for that ID.");
					return;
				} else {
					user = userFu.get();
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		if(user == null) {
			if(message.split(" ")[1].replaceAll("\\D+", "").isEmpty()) {
				event.getChannel().sendMessage("IDs are only numbers.");
			} else {
				event.getChannel().sendMessage("Cant find a user for that ID.");
			}
			return;
		}
		sendInfo(event, user, serverPfp);
	}
	private static EmbedBuilder getBaseEmbed(MessageCreateEvent event, User user, Server serv) {
		DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
		EmbedBuilder builder = new EmbedBuilder();
		builder.setAuthor(user);
		Instant created = user.getCreationTimestamp();
		builder.addField("Created", timeFormat.format(created));
		if(event.isServerMessage() && serv.isMember(user)) {
			Instant joined = user.getJoinedAtTimestamp(serv).get();
			builder.addField("Joined", timeFormat.format(joined));
		}
		return builder;
	}
	
	public static void sendInfo(MessageCreateEvent event, User user, boolean server) {
		Server serv = event.isServerMessage() ? event.getServer().get() : null;

		var builder = getBaseEmbed(event, user, serv);
		
		var serverPfpURL = "";
		String defaultImageURL_ = (user.getAvatar().isAnimated() ? user.getAvatar(4096).getUrl().toString().replace(".png", ".gif") : user.getAvatar(4096).getUrl()) + "";

		
		if (serv != null) {
			var serverAvatarOp = user.getServerAvatar(serv, 4096);
			if (serverAvatarOp.isPresent()) {
				var serverAvatar = serverAvatarOp.get();
				serverPfpURL = (serverAvatar.isAnimated() ? serverAvatar.getUrl().toString().replace(".png", ".gif") : serverAvatar.getUrl()) + "";
				if(server) {
					defaultImageURL_ = serverPfpURL;
					serverPfpURL = "";
				}
			}
		}
		
		if(serverPfpURL.isEmpty()) {
			try {
				builder.setImage(new URL(defaultImageURL_).openStream(), defaultImageURL_.toLowerCase().contains("gif") ? "gif" : "png");
			} catch (Exception ex) {
				builder.setImage(defaultImageURL_);
			}
		} else
			builder.setImage(defaultImageURL_);

		builder.setFooter("ID: " + user.getIdAsString());
		
		final String defaultImageURL = defaultImageURL_;
		
		try {
			var msg = event.getChannel().sendMessage(builder).get();
			if(!serverPfpURL.isEmpty()) {
				msg.addReactions("\u2B05", "\u27A1");
				var serverpurl = serverPfpURL;
				msg.addReactionAddListener(new ReactionAddListener() {
					@Override
					public void onReactionAdd(ReactionAddEvent arg0) {
						if(arg0.getUser().get().getId() != event.getMessageAuthor().asUser().get().getId()) {
							return;
						}
						
						var embed = getBaseEmbed(event, user, serv);
						
						String emote = arg0.getEmoji().asUnicodeEmoji().get();
												
						if(emote.equalsIgnoreCase("\u27A1") || emote.equalsIgnoreCase("\u2B05")) {
							if(emote.equalsIgnoreCase("\u2B05")) {
								embed.setImage(defaultImageURL);
							} else {
								embed.setImage(serverpurl);	
							}
							embed.setFooter("ID: " + user.getIdAsString());
							msg.edit(embed);
						}
					}
				}).removeAfter(3, TimeUnit.MINUTES);
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
