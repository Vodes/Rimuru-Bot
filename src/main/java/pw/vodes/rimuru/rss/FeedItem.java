package pw.vodes.rimuru.rss;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.apptasticsoftware.rssreader.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;

import pw.vodes.rimuru.Util;

public class FeedItem {
	
	private boolean wasPosted = false;
	private String title, author, category, description, guid, link, pubdate;
	
	
	@JsonIgnore
	public String getImage() {
		if(!this.getDescription().isBlank()) {
			var matcher = Util.imageUrlPattern.matcher(this.getDescription());
			if(matcher.find()) {
				return matcher.group();
			}
		}
		return null;
	}
	
	@JsonIgnore
	public long publicationUnixTime() {
		if(!this.getPubDate().isBlank()) {
			var time = ZonedDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(this.getPubDate()));
			return time.toEpochSecond();
		}
		return 0;
	}
	
	@JsonIgnore
	public String getURLToPost() {
		if(!getLink().isBlank()) {
			// Return GUID because the link on nyaa rss is the download url for the .torrent file
			if(this.getLink().toLowerCase().contains("nyaa.si")) {
				return this.getGuid();
			}
			return this.getLink();
		} 
		return "";
	}
	
	public boolean wasPosted() {
		return wasPosted;
	}
	
	public void setWasPosted(boolean wasPosted) {
		this.wasPosted = wasPosted;
	}
	
	public static FeedItem ofItem(Item item) {
		var feeditem = new FeedItem();
		var title = item.getTitle().orElse("");
		title = title.replace("&quot;", "\"").replace("&amp;", "&");
		feeditem.setTitle(title);
		feeditem.setAuthor(item.getAuthor().orElse(""));
		feeditem.setCategory(item.getCategory().orElse(""));
		feeditem.setDescription(item.getDescription().orElse(""));
		feeditem.setGuid(item.getGuid().orElse(""));
		feeditem.setLink(item.getLink().get());
		feeditem.setPubDate(item.getPubDate().get());
		return feeditem;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof FeedItem) {
			boolean isU2 = false, sameLink = false, sameTitle = false;
			if(!getLink().isBlank() && !((FeedItem)o).getLink().isBlank()) {
				sameLink = getLink().trim().equalsIgnoreCase(((FeedItem)o).getLink());
				isU2 = getLink().toLowerCase().contains("u2.dmhy.org");
			}
			if(!getTitle().isBlank() && !((FeedItem)o).getTitle().isBlank()) {
				sameTitle = getTitle().trim().equalsIgnoreCase(((FeedItem)o).getTitle());
			}
			
			if(isU2)
				return sameLink || sameTitle;
			else
				return sameLink;
		}
		return super.equals(o);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getPubDate() {
		return pubdate;
	}

	public void setPubDate(String pubdate) {
		this.pubdate = pubdate;
	}

	public boolean isWasPosted() {
		return wasPosted;
	}
	
	

}
