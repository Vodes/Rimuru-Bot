package pw.vodes.rimuru.listeners;

import java.time.Instant;
import java.util.logging.LogRecord;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;

import pw.vodes.rimuru.LogRepeater;
import pw.vodes.rimuru.Main;

public class MemberJoinListener implements ServerMemberJoinListener {

	@Override
	public void onServerMemberJoin(ServerMemberJoinEvent event) {
		try {
			var embed = new EmbedBuilder().setAuthor("User joined")
					.setThumbnail(event.getUser().getAvatar(4096))
					.setFooter("ID: " + event.getUser().getIdAsString())
					.setTitle(event.getUser().getDiscriminatedName())
					.setDescription(String.format("%s\nCreated:\n%s (%s)", 
							event.getUser().getMentionTag(),
							getRelativeStamp(event.getUser().getCreationTimestamp()),
							getAbsoluteStamp(event.getUser().getCreationTimestamp())));
			try {
				Main.getConfig().getOtherLogChannel().sendMessage(embed);
			} catch (Exception e) {
				LogRepeater.embedsToSend.add(embed);
			}
		} catch (Exception e) {}
	}
	
	private String getRelativeStamp(Instant instant) {
		return String.format("<t:%d:R>", instant.getEpochSecond());
	}
	
	private String getAbsoluteStamp(Instant instant) {
		return String.format("<t:%d:d> <t:%d:t>", instant.getEpochSecond(), instant.getEpochSecond());
	}

}
