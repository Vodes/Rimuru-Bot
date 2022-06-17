package pw.vodes.rimuru.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.command.Command;
import pw.vodes.rimuru.command.CommandType;

public class CommandClear extends Command {

	public CommandClear() {
		super("Clear", new String[] { "clear", "wipe", "cleanse" }, CommandType.mod);
		setUsage("`!clear 50 <mentioned users>`\nJust deletes a number of messages, You can also mention specific users and delete the messages from those only\n"
				+ "`!clear after <link/ID>`\nDeletes message after a certain messages, Replies also work");
	}

	@Override
	public void run(MessageCreateEvent event) {
		var args = getSplitMessage(event);
		List<Message> messages = new ArrayList<>();

		if (args.get(1).equalsIgnoreCase("after")) {
			Message start = null;
			if (args.get(2).isBlank() && event.getMessage().getReferencedMessage().isPresent()) {
				start = event.getMessage().getReferencedMessage().get();
			} else if (!args.get(2).isBlank() && args.get(2).startsWith("http")) {
				try {
					start = Main.api.getMessageByLink(args.get(2)).get().get();
					if(start.getChannel().getId() != event.getChannel().getId()) {
						throw new Exception();
					}
				} catch (Exception ex) {
					event.getChannel().sendMessage("Not a valid message link!\n" + getUsage());
					return;
				}
			} else if (!args.get(2).isBlank() && StringUtils.isNumeric(args.get(2))) {
				try {
					start = event.getChannel().getMessageById(Long.parseLong(args.get(2))).get();
				} catch (Exception ex) {
					event.getChannel().sendMessage("Not a valid message id!\n" + getUsage());
					return;
				}
			}
			if(start == null) {
				event.getChannel().sendMessage(getUsage());
				return;
			}
			messages = event.getChannel().getMessagesAfterAsStream(start).collect(Collectors.toList());
		} else if(StringUtils.isNumeric(args.get(1))) {
			if(event.getMessage().getMentionedUsers().isEmpty()) {
				try {
					messages = event.getChannel().getMessages(Integer.parseInt(args.get(1)) + 1).get().stream().collect(Collectors.toList());
				} catch (NumberFormatException | InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			} else {
				var count = Integer.parseInt(args.get(1));
				for(var user : event.getMessage().getMentionedUsers()) {
					messages.addAll(event.getChannel().getMessagesAsStream().filter(m -> m.getAuthor().getId() == user.getId()).limit(count).collect(Collectors.toList()));
				}
			}
		} else {
			event.getChannel().sendMessage(getUsage());
			return;
		}
		
		if(!messages.isEmpty()) {
			event.getChannel().deleteMessages(messages);
		}
	}

}
