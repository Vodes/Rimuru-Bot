package pw.vodes.rimuru.rss;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import com.apptasticsoftware.rssreader.RssReader;
import com.fasterxml.jackson.annotation.JsonIgnore;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.Util;

public class Feed {
	
	public String name, url, serverID, channelID;
	public String regex;
	public List<FeedItem> items;
	public boolean firstCheck = true;
	
	@JsonIgnore
	private ServerTextChannel channel;
	
	@JsonIgnore
	private Pattern pattern;
	
	
	public Feed() {}

	public Feed(String name, String url, String regex, String serverID, String channelID) {
		this.name = name;
		this.url = url;
		this.regex = regex;
		this.serverID = serverID;
		this.channelID = channelID;
		this.items = new ArrayList<>();
	}
	
	@JsonIgnore
	public ServerTextChannel getChannel() {
		if(channel == null) {
			channel = Main.api.getServerById(serverID).get().getTextChannelById(channelID).get();
		}
		return channel;
	}
	
	@JsonIgnore
	public Pattern getRegexPattern() {
		if(pattern == null && regex != null && !regex.isBlank())
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return pattern;
	}
	
	public void check() {
		var reader = new RssReader();
		var feedURL = url;
		if(feedURL.toLowerCase().contains("u2.dmhy.org")) {
			if(Main.getConfig().getU2PassKey().isBlank())
				return;
			var matcher = Util.u2PasskeyURLPattern.matcher(feedURL);
			if(matcher.find()) {
				var toReplace = matcher.group(1);
				feedURL = feedURL.replace(toReplace, "passkey=" + Main.getConfig().getU2PassKey());
			}
		}
		try {
			var read = reader.read(feedURL).collect(Collectors.toList());
			
			for(var i : read) {
				var feedItem = FeedItem.ofItem(i);
				if(!items.contains(feedItem)) {
					var pat = getRegexPattern();
					if(pat != null) {
						if(!feedItem.getTitle().isBlank()) {
							var matcher = pat.matcher(feedItem.getTitle());
							if(matcher.find()) 
								items.add(feedItem);
						}
					} else 
						items.add(feedItem);
				}
			}
			
			items.sort((o1, o2) -> Long.compare(o2.publicationUnixTime(), o1.publicationUnixTime()));
			if(items.size() > 50) {
				items = items.subList(0, 49);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		postUnposted();
		if(firstCheck) {
			for(var item : items) {
				item.setWasPosted(true);
			}
			firstCheck = false;
			FeedManager.save();
		}
	}
	
	private void postUnposted() {
		var isU2 = url.toLowerCase().contains("u2.dmhy.org");
		var isNyaa = url.toLowerCase().contains("nyaa.si");
		for(var item : items) {
			if(item.wasPosted())
				continue;
			var embed = new EmbedBuilder();
			embed.setTitle(item.getTitle());
			
			if(isU2 || isNyaa) {
				embed.setAuthor(isU2 ? "U2" : "Nyaa", isU2 ? "https://u2.dmhy.org" : "https://nyaa.si", isU2 ? "https://i.imgur.com/lNorPYS.png" : "https://nyaa.si/static/favicon.png");
				if(isNyaa && !item.getDescription().isBlank()) {
					var matcher = Util.nyaaAutismPattern.matcher(item.getDescription());
					if(matcher.find()) {
						embed.setDescription(String.format("%s | %s", matcher.group(4), matcher.group(5)));
					}
				}
			}
			
			var image = item.getImage();
			if(image != null) {
				embed.setImage(image);
			}
			
			var url = item.getURLToPost();
			if(!url.isBlank())
				embed.setUrl(url);
			
			embed.setTimestamp(Instant.ofEpochSecond(item.publicationUnixTime()));
			getChannel().sendMessage(embed).thenRun(() -> item.setWasPosted(true));
			if(firstCheck) {
				if(Instant.ofEpochSecond(item.publicationUnixTime()).isBefore(Instant.now().minus(24, ChronoUnit.HOURS))) {
					break;
				}
			}
		}
	}
}
