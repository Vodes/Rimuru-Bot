package pw.vodes.rimuru.command.commands;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;
import pw.vodes.rimuru.file.AutoMod;
import pw.vodes.rimuru.file.sub.AutoModFile;
import pw.vodes.rimuru.file.sub.AutoModFile.Punishment;

public class CommandAutomod extends Command {

	public CommandAutomod() {
		super("Automod", new String[] {"automod", "filter"}, CommandType.mod);
		setUsage("`filter \"string to filter\" <kick/ban/timeout> <timeout_minutes>`\n"
				+ "Quotes for Strings with spaces. Quotes can also be escaped.\n"
				+ "Regex fully supported.\n"
				+ "`filter list`\n"
				+ "Lists current filters\n"
				+ "`filter remove <index>`\n"
				+ "Removes filter by index from `list`");
	}

	@Override
	public void run(MessageCreateEvent event) {
		var args = getSplitMessage(event);
		
		if(StringUtils.equalsAnyIgnoreCase(args.get(1), "delete", "remove", "rem", "del") && StringUtils.isNumeric(args.get(2))) {
			try {
				var am = AutoMod.getAutomods().get(Integer.parseInt(args.get(2)));
				AutoMod.remove(am);
				event.getChannel().sendMessage("Removed filter!");
			} catch (Exception e) {
				event.getChannel().sendMessage("No filter with that index found!");
			}
		} else if(StringUtils.equalsAnyIgnoreCase(args.get(1), "list", "l")) {
			if(AutoMod.getAutomods().isEmpty()) {
				event.getChannel().sendMessage("No filters.");
				return;
			}
			event.getChannel().sendMessage(getListEmbed());
			return;
		} else if(!args.get(1).isBlank()) {
			AutoModFile am = new AutoModFile();
			am.filteredSequence = args.get(1);
			am.punishment = Punishment.get(args.get(2));
			am.punishmentVal = StringUtils.isNumeric(args.get(3)) ? args.get(3) : "0";
			AutoMod.add(am);
			event.getChannel().sendMessage("Filter added!");
		} else {
			event.getChannel().sendMessage(getUsage());
			return;
		}
	}
	
	private EmbedBuilder getListEmbed() {
		var embed = new EmbedBuilder().setTitle("Filters");
		for(var am : AutoMod.getAutomods()) {
			var val = am.punishment == null ? "Remove only" : (am.punishment == Punishment.TIMEOUT ? (am.punishment.toString() + " " + am.punishmentVal) : am.punishment.toString());
			embed.addField(am.filteredSequence, val);
		}
		return embed;
	}
}
