package pw.vodes.rimuru;

import java.util.concurrent.ExecutionException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pw.vodes.rimuru.audit.AuditLogs;
import pw.vodes.rimuru.command.CommandManager;
import pw.vodes.rimuru.file.AutoMod;
import pw.vodes.rimuru.file.AutoRoles;
import pw.vodes.rimuru.file.FileManager;
import pw.vodes.rimuru.listeners.MemberJoinListener;
import pw.vodes.rimuru.listeners.MemberLeaveListener;
import pw.vodes.rimuru.verification.VerificationListener;

public class Main {
	
	public static DiscordApi api;
	private static FileManager fileManager;
	private static ConfigFile configFile;
	private static ConfigAdapter config;
	private static ObjectMapper mapper = new ObjectMapper();
	private static CommandManager commandManager;
	
	private static Server server;
	
	private static TextChannel logChannel, staffActionChannel, hallOfShameChannel;
	
	public static void main(String[] args) {
		fileManager = new FileManager(args.length > 0 ? args[0] : null);
		try {
			configFile = mapper.readValue(fileManager.read("config.json"), ConfigFile.class);
		} catch (JsonProcessingException e) {
			try {
				fileManager.write("config.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ConfigFile()));
				System.out.println("Please setup your config at: " + fileManager.getConfigDir().getAbsolutePath());
				return;
			} catch (JsonProcessingException e1) {}
		}
		api = new DiscordApiBuilder()
				.setToken(configFile.bot_token)
				.setAllIntents()
				.login().join();
		if(api.getServers().isEmpty()) {
			System.out.println("Invite Link: " + api.createBotInvite());
			return;
		}
		api.updateActivity(ActivityType.PLAYING, "!help");
		server = (Server) api.getServers().toArray()[0];
		config = new ConfigAdapter();
		commandManager = new CommandManager().init();
		AutoRoles.load();
		AutoMod.load();
		AuditLogs.init();
		
		
		api.addMessageCreateListener(e -> commandManager.tryRunCommand(e));
		api.addMessageCreateListener(AutoMod.getAutomodListener());
		api.addServerMemberJoinListener(new MemberJoinListener());
		api.addServerMemberLeaveListener(new MemberLeaveListener());
	}
	
	public static String getVersion() {
		return "0.0.3";
	}
	
	public static FileManager getFiles() {
		return fileManager;
	}
	
	public static CommandManager getCommandManager() {
		return commandManager;
	}
	
	public static ConfigFile getConfigFile() {
		return configFile;
	}
	
	public static ConfigAdapter getConfig() {
		return config;
	}
	
	public static Server getServer() {
		return server;
	}
	
	public static ObjectMapper getMapper() {
		return mapper;
	}
	
	public static TextChannel getStaffActionChannel() {
		return staffActionChannel;
	}
	
	public static TextChannel getLogChannel() {
		return logChannel;
	}
	
	public static TextChannel getHallOfShameChannel() {
		return hallOfShameChannel;
	}
	
}
