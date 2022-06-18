package pw.vodes.rimuru.command.commands;

import java.util.stream.Collectors;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;
import pw.vodes.rimuru.command.util.MultiPageEmbed;

public class CommandHelp extends Command {

	public CommandHelp() {
		super("Help", new String[] {"help", "h"}, CommandType.everyone);
	}

	@Override
	public void run(MessageCreateEvent event) {
		var embed = new EmbedBuilder().setTitle("Rimuru-Bot")
				.setAuthor(event.getMessageAuthor())
				.setThumbnail(Main.api.getYourself().getAvatar(4096))
				.addField("Version", Main.getVersion(), true)
				.addField("Written by", "<@129871096299126784>", true)
				.addField("Source code", "https://github.com/Vodes/Rimuru-Bot", false);
		
		var commands = new EmbedBuilder().setTitle("Commands")
				.setAuthor(event.getMessageAuthor());
		
		int current = 0;
		for(var cmd : Main.getCommandManager().getCommands().stream()
				.filter(c -> !c.getName().equalsIgnoreCase("help"))
				.sorted((c1, c2) -> c2.getName().compareToIgnoreCase(c1.getName()))
				.collect(Collectors.toList())) {
			commands.addField(cmd.getName() + (cmd.isEnabled() ? "" : " (disabled)"), cmd.getType().toString(), current == 2 ? false : true);
			current++;
			if(current > 2) 
				current = 0;
		}
		
		var multiEmbed = new MultiPageEmbed(embed, event.getMessageAuthor().asUser().get(), false, true);
		multiEmbed.addPage(commands);
		multiEmbed.sendMessage(event.getChannel());
	}

}
