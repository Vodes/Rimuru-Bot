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
	private TextChannel lobbyChannel;

	// Verification Stuff
	private Role verificationRole;
	private Message verificationMessage;
	private TextChannel verificationChannel, hallOfShameChannel;
	
	//Log
	private TextChannel staffActionChannel, otherLogChannel;

	
	public ConfigAdapter() {
		if(Main.getServer() == null)
			return;
		
		automodExcludedCategories.addAll(Main.getConfigFile().automod_excluded_categories);
		commandPrefix = Main.getConfigFile().command_prefix;
		projectPrefix = Main.getConfigFile().project_prefix;
		
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
		
		if(!Main.getConfigFile().staff_action_log_channel.isBlank())
			staffActionChannel = Main.getServer().getTextChannelById(Main.getConfigFile().staff_action_log_channel).orElseGet(null);
		
		if(!Main.getConfigFile().auditlog_replacement_channel.isBlank())
			otherLogChannel = Main.getServer().getTextChannelById(Main.getConfigFile().auditlog_replacement_channel).orElseGet(null);
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


	public TextChannel getStaffActionChannel() {
		return staffActionChannel;
	}


	public TextChannel getOtherLogChannel() {
		return otherLogChannel;
	}

}
