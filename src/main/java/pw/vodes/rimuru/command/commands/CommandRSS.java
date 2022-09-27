package pw.vodes.rimuru.command.commands;

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;
import pw.vodes.rimuru.rss.Feed;
import pw.vodes.rimuru.rss.FeedManager;

public class CommandRSS extends Command {

	public CommandRSS() {
		super("RSS", new String[] {"rss", "feeds"}, CommandType.admin);
		setUsage("`rss add <name> <feed-URL> <channel/channel-ID> \"<regex>\"`\n"
				+ "Adds a feed to the specified channel and filters with the (optional) regex\n"
				+ "Make sure to not have any passkeys in there lmao\n"
				+ "`rss delete <number of feed>`\n"
				+ "Deletes the feed\n"
				+ "`rss update-link/link <number of feed> <link>`\n"
				+ "Changes the link of a feed\n"
				+ "`rss list`\n"
				+ "Lists the current feeds (including numbers needed for deletion)");
	}

	@Override
	public void run(MessageCreateEvent event) {
		var args = getSplitMessage(event);
		
		if(args.get(1).equalsIgnoreCase("add")) {
			if(args.size() < 5) {
				event.getChannel().sendMessage(getUsage());
				return;
			}
			ServerTextChannel channel;
			if(event.getMessage().getMentionedChannels().isEmpty()) {
				if(StringUtils.isNumeric(args.get(4))) {
					channel = event.getServer().get().getChannelById(args.get(4)).get().asServerTextChannel().get();
				} else if (args.get(4).startsWith("http")){
					Message message;
					try {
						message = Main.api.getMessageByLink(args.get(4)).get().get();
					} catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
						e.printStackTrace();
						return;
					}
					channel = message.getServerTextChannel().get();
				} else {
					event.getChannel().sendMessage(getUsage());
					return;
				}
			} else {
				channel = event.getMessage().getMentionedChannels().get(0).asServerTextChannel().get();
			}
			FeedManager.getFeeds().add(new Feed(args.get(2), args.get(3), args.get(5), channel.getServer().getIdAsString(), channel.getIdAsString()));
			FeedManager.save();
			event.getChannel().sendMessage(String.format("Feed added to %s", channel.getMentionTag()));
		} else if(StringUtils.equalsAnyIgnoreCase(args.get(1), "delete", "remove", "rem", "del")) {
			if(!StringUtils.isNumeric(args.get(2))) {
				event.getChannel().sendMessage("Please specify a feed number.");
				return;
			}
			try {
				FeedManager.getFeeds().remove(Integer.parseInt(args.get(2)) - 1);
				FeedManager.save();
				event.getChannel().sendMessage("Feed removed.");
			} catch (Exception e) {
				event.getChannel().sendMessage("Failed to remove feed with that index. Maybe it doesn't exist?");
			}
		} else if(StringUtils.equalsAnyIgnoreCase(args.get(1), "update-link", "link")) {
			if(!StringUtils.isNumeric(args.get(2))) {
				event.getChannel().sendMessage("Please specify a feed number.");
				return;
			}
			if(args.get(3).isBlank()) {
				event.getChannel().sendMessage("Please specify a link.");
				return;
			}
			try {
				FeedManager.getFeeds().get(Integer.parseInt(args.get(2)) - 1).url = args.get(3);
				FeedManager.save();
				event.getChannel().sendMessage("Link updated.");
			} catch (Exception e) {
				event.getChannel().sendMessage("Failed to update feed with that index. Maybe it doesn't exist?");
			}
		} else if(StringUtils.equalsAnyIgnoreCase(args.get(1), "list", "l")) {
			event.getChannel().sendMessage(getListEmbed());
		} else {
			event.getChannel().sendMessage(getUsage());
		}
	}
	
	private EmbedBuilder getListEmbed() {
		var embed = new EmbedBuilder();
		embed.setTitle("List of RSS Feeds");
		var feeds = FeedManager.getFeeds();
		for(var feed : feeds) {
			embed.addField(String.format("%d: %s", feeds.indexOf(feed) + 1, feed.name), String.format("in: %s\n%s", feed.getChannel().getMentionTag(), feed.url));
		}
		return embed;
	}

}
