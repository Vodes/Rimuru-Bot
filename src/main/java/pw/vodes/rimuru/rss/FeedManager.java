package pw.vodes.rimuru.rss;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;

public class FeedManager {
	
	private static ArrayList<Feed> feeds = new ArrayList<Feed>();
	
	@SuppressWarnings("unchecked")
	public static void load() {
		if(Main.getFiles().exists("rssfeeds.json")) {
			try {
				feeds = (ArrayList<Feed>)Main.getMapper().readValue(Main.getFiles().read("rssfeeds.json"), Main.getMapper().getTypeFactory().constructCollectionType(ArrayList.class, Feed.class));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		startChecking();
	}
	
	public static void save() {
		try {
			Main.getFiles().write("rssfeeds.json", Main.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(feeds));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Feed> getFeeds() {
		return feeds;
	}
	
	private static void startChecking() {
		new Thread(() -> {
			while(true) {
				for(var feed : feeds) {
					feed.check();
					save();
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {}
				}
				try {
					Thread.sleep(600000);
				} catch (InterruptedException e) {}
			}
		}).start();
	}

}
