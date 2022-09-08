package pw.vodes.rimuru;

import java.util.ArrayList;
import java.util.Optional;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;

import pw.vodes.rimuru.verification.VerificationListener;

public class ConfigAdapter {
	
	private ArrayList<Role> modRoles = new ArrayList<>();
	private ArrayList<String> automodExcludedCategories = new ArrayList<>();
	
	private String commandPrefix, projectPrefix;
	private String u2PassKey;
	private TextChannel lobbyChannel;

	// Verification Stuff
	private Role verificationRole;
	private Message verificationMessage;
	private TextChannel verificationChannel, hallOfShameChannel;
	private boolean purgeUnverified;
	
	//Log
	private TextChannel userLogChannel, otherLogChannel;

	
	public ConfigAdapter() {
		if(Main.getServer() == null)
			return;
		
		automodExcludedCategories.addAll(Main.getConfigFile().automod_excluded_categories);
		commandPrefix = Main.getConfigFile().command_prefix;
		projectPrefix = Main.getConfigFile().project_prefix;
		purgeUnverified = Main.getConfigFile().purge_unverified_3days;
		u2PassKey = Main.getConfigFile().u2_passkey;
		
		for(var roleID : Main.getConfigFile().mod_roles) {
			Optional<Role> role;
			if((role = Main.getServer().getRoleById(roleID)).isPresent())
				modRoles.add(role.get());
		}
		
		if(!Main.getConfigFile().general_chat.isBlank())
			lobbyChannel = Main.getServer().getTextChannelById(Main.getConfigFile().general_chat).orElseGet(null);
		
		if(!Main.getConfigFile().verification_channel.isBlank()) {
			verificationChannel = Main.getServer().getTextChannelById(Main.getConfigFile().verification_channel).orElseGet(null);
			if(verificationChannel != null) {
				try {
					verificationRole = Main.getServer().getRoleById(Main.getConfigFile().verification_role).get();
					hallOfShameChannel = Main.getServer().getTextChannelById(Main.getConfigFile().verify_hall_of_shame).orElse(null);
					verificationMessage = verificationChannel.getMessageById(Main.getConfigFile().verification_reaction_message).get();
					verificationMessage.addReactionAddListener(new VerificationListener());
				} catch (Exception e) {}
			}
		}
		
		if(!Main.getConfigFile().user_log_channel.isBlank())
			userLogChannel = Main.getServer().getTextChannelById(Main.getConfigFile().user_log_channel).orElseGet(null);
		
		if(!Main.getConfigFile().other_log_channel.isBlank())
			otherLogChannel = Main.getServer().getTextChannelById(Main.getConfigFile().other_log_channel).orElseGet(null);
	}


	public ArrayList<Role> getModRoles() {
		return modRoles;
	}


	public ArrayList<String> getAutomodExcludedCategories() {
		return automodExcludedCategories;
	}


	public String getCommandPrefix() {
		return commandPrefix;
	}


	public String getProjectPrefix() {
		return projectPrefix;
	}


	public TextChannel getLobbyChannel() {
		return lobbyChannel;
	}


	public Role getVerificationRole() {
		return verificationRole;
	}

	public TextChannel getVerificationChannel() {
		return verificationChannel;
	}


	public TextChannel getHallOfShameChannel() {
		return hallOfShameChannel;
	}
	
	public boolean shouldPurgeUnverified() {
		return purgeUnverified;
	}
	
	public String getU2PassKey() {
		return u2PassKey;
	}

	public TextChannel getUserLogChannel() {
		return userLogChannel;
	}


	public TextChannel getOtherLogChannel() {
		return otherLogChannel;
	}

}
