package pw.vodes.rimuru;

import java.util.concurrent.ExecutionException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.server.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pw.vodes.rimuru.command.CommandManager;
import pw.vodes.rimuru.file.AutoRoles;
import pw.vodes.rimuru.file.FileManager;
import pw.vodes.rimuru.verification.VerificationListener;

public class Main {
	
	public static DiscordApi api;
	private static FileManager fileManager = new FileManager();
	private static Config config;
	private static ObjectMapper mapper = new ObjectMapper();
	private static CommandManager commandManager;
	
	private static Server server;
	
	public static void main(String[] args) {
		try {
			config = mapper.readValue(fileManager.read("config.json"), Config.class);
		} catch (JsonProcessingException e) {
			try {
				fileManager.write("config.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Config()));
				System.out.println("Please setup your config at: " + fileManager.getConfigDir().getAbsolutePath());
				System.exit(0);
			} catch (JsonProcessingException e1) {}
		}
		api = new DiscordApiBuilder()
				.setToken(config.bot_token)
				.setAllIntents()
				.login().join();
		api.updateActivity(ActivityType.CUSTOM, "!help");
		server = (Server) api.getServers().toArray()[0];
		commandManager = new CommandManager().init();
		AutoRoles.load();				
		try {
			var verifyMessage = server.getChannelById(getConfig().verification_channel).get().asServerTextChannel().get().getMessageById(getConfig().verification_reaction_message).get();
			verifyMessage.addReactionAddListener(new VerificationListener());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		api.addMessageCreateListener(e -> commandManager.tryRunCommand(e));
	}
	
	public static FileManager getFiles() {
		return fileManager;
	}
	
	public static Config getConfig() {
		return config;
	}
	
	public static Server getServer() {
		return server;
	}
	
	public static ObjectMapper getMapper() {
		return mapper;
	}
	
}
