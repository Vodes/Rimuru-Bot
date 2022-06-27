package pw.vodes.rimuru.projects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandProjects extends Command {
	
	public CommandProjects() {
		super("Projects", new String[] {"project", "projects", "pro"}, CommandType.admin);
	}
	
	private void sendUsageEmbed(MessageCreateEvent event) {
		
	}

	@Override
	public void run(MessageCreateEvent event) {
		var args = getSplitMessage(event);
		
		if(args.get(1).isBlank()) {
			sendUsageEmbed(event);
			return;
		}
		
		var p = Main.getConfig().getCommandPrefix();
		
		if(StringUtils.equalsAnyIgnoreCase(args.get(1), "create", "new")) {
			createProjectEvent(args, event);
		} else if (StringUtils.equalsAnyIgnoreCase(args.get(1), "list", "l")){
			
		} else if (StringUtils.equalsAnyIgnoreCase(args.get(1), "remove", "delete", "rem")){
			if(args.get(2).isBlank() || !StringUtils.isNumeric(args.get(2))) {
				event.getChannel().sendMessage(p + "projects remove <list-index>");
				return;
			}
			
		} else {
			sendUsageEmbed(event);
		}
	}
	
	private void createProjectEvent(List<String> args, MessageCreateEvent event) {
		
	}
}
