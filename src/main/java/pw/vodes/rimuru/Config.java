package pw.vodes.rimuru;

import java.util.Arrays;
import java.util.List;

public class Config {
	
	public String bot_token = "Your bot token";
	public String command_prefix = "!";
	public String general_chat = "";
	public String verification_role = "", verification_channel = "", verification_reaction_message = "";
	public List<String> mod_roles = Arrays.asList("Role ID 1", "Role ID 2");
	public List<String> automod_excluded_categories = Arrays.asList("unhinged", "category 2");
	
}