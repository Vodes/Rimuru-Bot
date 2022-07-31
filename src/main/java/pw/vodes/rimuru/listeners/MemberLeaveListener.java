package pw.vodes.rimuru.listeners;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;

import pw.vodes.rimuru.LogRepeater;
import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.audit.AuditLogs;

public class MemberLeaveListener implements ServerMemberLeaveListener {

	@Override
	public void onServerMemberLeave(ServerMemberLeaveEvent event) {
		new Thread(() -> {
			try {
				Thread.sleep(750);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			AuditLogs.check();
			if(AuditLogs.userWasKickedOrBanned(event.getUser())) {
				return;
			}
			try {
				var embed = new EmbedBuilder().setAuthor("User left")
						.setThumbnail(event.getUser().getAvatar(4096))
						.setFooter("ID: " + event.getUser().getIdAsString())
						.setTitle(event.getUser().getDiscriminatedName())
						.setDescription(event.getUser().getMentionTag());
				try {
					Main.getConfig().getOtherLogChannel().sendMessage(embed);
				} catch (Exception e) {
					LogRepeater.embedsToSend.add(embed);
				}
			} catch (Exception e2) {}
		}).start();
	}
}
