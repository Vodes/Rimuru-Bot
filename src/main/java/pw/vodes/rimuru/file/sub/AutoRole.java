package pw.vodes.rimuru.file.sub;

import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AutoRole {
	
	public String channel_id, message_id;
	public String role_id;
	
	@JsonIgnore
	public ReactionAddListener addListener;
	@JsonIgnore
	public ReactionRemoveListener removeListener;
	
	public AutoRole() {}
	
	public AutoRole(String channel_id, String message_id, String role_id) {
		this.channel_id = channel_id;
		this.message_id = message_id;
		this.role_id = role_id;
	}

}
