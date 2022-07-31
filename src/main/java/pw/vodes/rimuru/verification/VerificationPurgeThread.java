package pw.vodes.rimuru.verification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.audit.AuditLogs;
import pw.vodes.rimuru.audit.CommandStaffAction;
import pw.vodes.rimuru.audit.StaffActionType;

public class VerificationPurgeThread extends Thread {
	
	@Override
	public void run() {
		while(true) {
			if(Main.getConfig().shouldPurgeUnverified()) {
				try {
					var purgeTime = Instant.now().minus(3, ChronoUnit.DAYS);
					for(var user : Main.getServer().getMembers()) {
						if(shouldKick(user)) {
							if(user.getJoinedAtTimestamp(Main.getServer()).get().isBefore(purgeTime)) {
								Main.getServer().kickUser(user, "Unverified for 3+ days.").thenRun(() -> {
									var embed = new EmbedBuilder().setTitle(StaffActionType.kick.getMessageTitle())
											.setFooter("ID: " + user.getIdAsString())
											.setAuthor(Main.api.getYourself())
											.setThumbnail(user.getAvatar(4096))
											.setDescription("Unverified for 3+ days");
									Main.getConfig().getOtherLogChannel().sendMessage(embed);
								});
							}
						}
					}
				} catch (Exception e) {}
			}
			try {
				Thread.sleep(600000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean shouldKick(User user) {
		if(Main.getConfig().getVerificationRole().hasUser(user) || 
				user.isBot() || 
				Main.getServer().isOwner(user) || Main.getServer().hasPermission(user, PermissionType.ADMINISTRATOR)) {
			return false;
		}
		return true;
	}

}
