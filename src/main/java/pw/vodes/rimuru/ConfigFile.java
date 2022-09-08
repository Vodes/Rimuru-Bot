package pw.vodes.rimuru;

import java.util.Arrays;
import java.util.List;

import pw.vodes.rimuru.update.UpdateConfig;

public class ConfigFile {
	
	public String bot_token = "Your bot token";
	public String command_prefix = "!";
	public String project_prefix = ".";
	public String general_chat = "";
	public String user_log_channel = "", other_log_channel = "";
	public String verification_role = "", verification_channel = "", verification_reaction_message = "", verify_hall_of_shame = "";
	public String u2_passkey = "";
	public boolean purge_unverified_3days = true;
	public List<String> mod_roles = Arrays.asList("Role ID 1", "Role ID 2");
	public List<String> automod_excluded_categories = Arrays.asList("unhinged", "category 2");
	
	public UpdateConfig updateConfig = new UpdateConfig();
	
}