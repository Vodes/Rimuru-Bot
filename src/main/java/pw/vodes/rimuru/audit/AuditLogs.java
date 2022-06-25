package pw.vodes.rimuru.audit;

import java.time.Instant;
import java.util.ArrayList;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;

public class AuditLogs {

	private static ArrayList<CommandStaffAction> staffActions = new ArrayList<CommandStaffAction>();

	public static ArrayList<CommandStaffAction> getStaffActions() {
		return staffActions;
	}

	public static void registerStaffAction(CommandStaffAction action) {
		if (!hasStaffAction(action)) {
			getStaffActions().add(action);
			try {
				Main.getStaffActionChannel().sendMessage(getEmbedForAction(action));
			} catch (Exception e) {}
			save();
		}
	}
	
	public static boolean userWasKickedOrBanned(User user) {
		for (var sa : getStaffActions()) {
			if(sa.affectedUser.equalsIgnoreCase(user.getIdAsString())) {
				if(Math.abs(sa.time - Instant.now().getEpochSecond()) < 3) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static EmbedBuilder getEmbedForAction(CommandStaffAction action) throws Exception {
		var u1 = Main.api.getUserById(action.executingUser).get();
		var u2 = Main.api.getUserById(action.affectedUser).get();
		return new EmbedBuilder().setTitle(action.type.getMessageTitle())
				.setFooter("ID: " + action.affectedUser)
				.setAuthor(u1)
				.setThumbnail(u2.getAvatar(4096))
				.setDescription(u2.getDiscriminatedName() + (action.reason.isBlank() ? "" : "\nReason:\n```" + action.reason + "\n```"));
	}

	private static boolean hasStaffAction(CommandStaffAction action) {
		for (var sa : getStaffActions()) {
			if (sa.equals(action)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static void load() {
		if (Main.getFiles().exists("staffactions.json")) {
			try {
				staffActions = (ArrayList<CommandStaffAction>) Main.getMapper().readValue(Main.getFiles().read("staffactions.json"),
						Main.getMapper().getTypeFactory().constructCollectionType(ArrayList.class, CommandStaffAction.class));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}

	private static void save() {
		try {
			Main.getFiles().write("staffactions.json", Main.getMapper().writeValueAsString(staffActions));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		load();
		
		new Thread(() -> {
			while(true) {
				check();
				try {
					Thread.sleep(1750);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public static void check() {
		try {
			var logs = Main.getServer().getAuditLog(20).get().getEntries();
			for(var log : logs) {
				if(log.getCreationTimestamp().isBefore(Instant.now().minusSeconds(3))) {
					continue;
				}
				switch (log.getType()) {
				case MEMBER_BAN_ADD: {
					registerStaffAction(new CommandStaffAction(Instant.now().getEpochSecond()
					, log.getTarget().get().asUser().get().getIdAsString()
					, log.getUser().get().getIdAsString()
					, log.getReason().isPresent() ? log.getReason().get() : ""
					, StaffActionType.ban));
					break;
				}
				case MEMBER_BAN_REMOVE: {
					registerStaffAction(new CommandStaffAction(Instant.now().getEpochSecond()
					, log.getTarget().get().asUser().get().getIdAsString()
					, log.getUser().get().getIdAsString()
					, log.getReason().isPresent() ? log.getReason().get() : ""
					, StaffActionType.unban));
					break;
				}
				case MEMBER_KICK: {
					registerStaffAction(new CommandStaffAction(Instant.now().getEpochSecond()
					, log.getTarget().get().asUser().get().getIdAsString()
					, log.getUser().get().getIdAsString()
					, log.getReason().isPresent() ? log.getReason().get() : ""
					, StaffActionType.kick));
					break;
				}
				default:
					break;
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}
