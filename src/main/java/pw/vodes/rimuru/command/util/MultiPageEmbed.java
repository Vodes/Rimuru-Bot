package pw.vodes.rimuru.command.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

import pw.vodes.rimuru.Util;

public class MultiPageEmbed {
	
	// Page Management
	private ArrayList<EmbedBuilder> pages = new ArrayList<EmbedBuilder>();
	public int availablePages = 0;
	public int currentPage = 1;
	
	// Reaction Management
	public User user;
	public Message message;
	public ReactionAddListener listener;
	public boolean isOwnerOnly = false;
	public boolean autoRemove = false;
	
	public MultiPageEmbed(EmbedBuilder first, User user, boolean ownerOnly, boolean autoRemove) {
		this.addPage(first);
		this.user = user;
		this.isOwnerOnly = ownerOnly;
		this.autoRemove = autoRemove;
	}
	
	public void addPage(EmbedBuilder embed) {
		pages.add(embed);
		availablePages++;
	}
	
	public EmbedBuilder getCurrent() {
		return pages.get(currentPage - 1);
	}
	
	public void setCurrent(EmbedBuilder embed) {
		if(message != null)
			message.edit(embed.setFooter(String.format("Page %02d / %02d", currentPage, availablePages)));
	}
	
	public void addListener() {
		listener = new ReactionAddListener() {
			@Override
			public void onReactionAdd(ReactionAddEvent arg0) {
				if(arg0.getMessage().get().isServerMessage() && !arg0.getUser().get().isYourself() && autoRemove) {
					arg0.getMessage().get().removeReactionByEmoji(arg0.getUser().get(), arg0.getEmoji());
				}
				if(!arg0.getEmoji().isUnicodeEmoji() || arg0.getUser().get().isYourself()) {
					return;
				}
				if(isOwnerOnly && !arg0.getUser().get().isBotOwner()) {
					return;
				}
				if(arg0.getUser().get().getId() != user.getId()) {
					return;
				}
				String emote = arg0.getEmoji().asUnicodeEmoji().get();
				if(emote.equalsIgnoreCase("\u27A1") || emote.equalsIgnoreCase("\u2B05")) {
					if(emote.equalsIgnoreCase("\u2B05")) {
						if(currentPage > 1) {
							currentPage--;
							setCurrent(getCurrent());
						}
					} else {
						if(currentPage < availablePages) {
							currentPage++;
							setCurrent(getCurrent());
						}
					}
				}
			}
		};
		message.addReactionAddListener(listener).removeAfter(1, TimeUnit.MINUTES);
	}

	public void sendMessage(TextChannel textChannel) {
		try {
			message = textChannel.sendMessage(getCurrent().setFooter(String.format("Page %02d / %02d", currentPage, availablePages))).get();
			message.addReaction("\u2B05");
			message.addReaction("\u27A1");
			addListener();
			new Thread(() -> {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(message.isServerMessage()) {
					message.removeAllReactions();
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}