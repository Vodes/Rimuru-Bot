package pw.vodes.rimuru.rss;

import java.util.ArrayList;

import pw.vodes.rimuru.rss.sub.RssField;

public class RssFeed {
	
	public String url = "";
	public String channel_id;
	public int refreshInterval = 15;
	public RssField urlField, thumbnailField;
	public ArrayList<RssField> fields = new ArrayList<>();
	public ArrayList<String> alreadyPosted = new ArrayList<String>();
	
	public boolean enabled = true;
	
	public void startChecking() {
		new Thread(() -> {
			while(enabled) {
				check();
				try {
					Thread.sleep(refreshInterval * 60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void check() {
		
	}

}
