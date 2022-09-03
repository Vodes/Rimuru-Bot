package pw.vodes.rimuru.file;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.file.sub.AutoRole;

public class AutoRoles {
	
	private static List<AutoRole> autoroles = new ArrayList<AutoRole>();
	
	@SuppressWarnings("unchecked")
	public static void load() {
		if(Main.getFiles().exists("autoroles.json")) {
			try {
				autoroles = (ArrayList<AutoRole>)Main.getMapper().readValue(Main.getFiles().read("autoroles.json"), Main.getMapper().getTypeFactory().constructCollectionType(ArrayList.class, AutoRole.class));
				for(var ar : autoroles) {
					try {
						var msg = Main.getServer().getChannelById(ar.channel_id).get().asServerTextChannel().get().getMessageById(ar.message_id).get();
						msg.canYouDelete();
						addListeners(ar);
					} catch (Exception e) {
						// Message doesnt exit
					}
				}
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static List<AutoRole> getAutoroles() {
		return autoroles;
	}
	
	public static void removeAutoRole(AutoRole ar) {
		try {
			var msg = Main.getServer().getTextChannelById(ar.channel_id).get().getMessageById(ar.message_id).get();

			if(ar.addListener != null) {
				msg.removeListener(ReactionAddListener.class, ar.addListener);
			}
			
			if(ar.removeListener != null) {
				msg.removeListener(ReactionRemoveListener.class, ar.removeListener);
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		autoroles.remove(ar);
		save();
	}
	
	private static void save() {
		try {
			Main.getFiles().write("autoroles.json", Main.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(autoroles));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	public static void addAutorole(String channel_id, String message_id, String role_id) {
		var ar = new AutoRole(channel_id, message_id, role_id);
		addListeners(ar);
		autoroles.add(ar);
		save();
	}
	
	private static void addListeners(AutoRole ar) {
		var role = Main.getServer().getRoleById(ar.role_id).get();
		
		try {
			var msg = Main.getServer().getTextChannelById(ar.channel_id).get().getMessageById(ar.message_id).get();

			var addListener = new ReactionAddListener() {
				@Override
				public void onReactionAdd(ReactionAddEvent event) {
					if (!role.hasUser(event.getUser().get())) {
						role.addUser(event.getUser().get());
					}
				}
			};

			var removeListener = new ReactionRemoveListener() {
				@Override
				public void onReactionRemove(ReactionRemoveEvent event) {
					if (role.hasUser(event.getUser().get())) {
						role.removeUser(event.getUser().get());
					}
				}
			};
			
			msg.addReactionAddListener(ar.addListener = addListener);
			msg.addReactionRemoveListener(ar.removeListener = removeListener);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

}
