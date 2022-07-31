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
									AuditLogs.registerStaffAction(new CommandStaffAction(Instant.now().getEpochSecond()
											, user.getIdAsString()
											, Main.api.getYourself().getIdAsString()
											, "Unverified for 3+ days"
											, StaffActionType.kick));
								});
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
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
