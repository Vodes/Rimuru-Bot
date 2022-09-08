package pw.vodes.rimuru;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class Util {

	public static SecureRandom random = new SecureRandom();
	public static final Pattern messageSplitPattern = Pattern.compile("(?=\\S)[^\\\"\\s]*(?:\\\"[^\\\\\\\"]*(?:\\\\[\\s\\S][^\\\\\\\"]*)*\\\"[^\\\"\\s]*)*");
	public static final Pattern imageUrlPattern = Pattern.compile("[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\-\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/=]*)(.jpg|.png|.jpeg|.gif)", Pattern.CASE_INSENSITIVE);
	public static final Pattern u2PasskeyURLPattern = Pattern.compile("https?:\\/\\/u2\\.dmhy\\.org\\/torrentrss\\.php.*(passkey=[^& ]*).*", Pattern.CASE_INSENSITIVE);
	public static final Pattern nyaaAutismPattern = Pattern.compile("(<.*>)(#\\d*) \\| (.+)(?:<\\/a>) \\| (\\d*\\.\\d* (?:GiB|MiB|TiB)) \\| ([^|]*) \\| (.*)", Pattern.CASE_INSENSITIVE);

	
	public static void reportException(Throwable ex) {
		reportException(ex, null);
	}
	
	public static void reportException(Throwable ex, String source) {
		try {
			var stackTrace = ExceptionUtils.getStackTrace(ex);
			var message = ExceptionUtils.getMessage(ex);
			System.out.println(stackTrace);
			// Trim to avoid message that's too long for discord
			var trimmed = stackTrace.substring(stackTrace.indexOf('\n') + 1, Math.min(stackTrace.length(), 1200));
			var embed = new EmbedBuilder()
					.setTitle("Exception thrown!")
					.setDescription(trimmed.toLowerCase().contains("pw.vodes.rimuru") ? 
							String.format("%s\n```\n%s\n```", message, trimmed) 
							: message);
			if(source != null && !source.isBlank())
				embed.setFooter("Source: " + source);
			Main.getConfig().getOtherLogChannel().sendMessage(embed);
		} catch (Exception e) {
			// This shouldn't happen...
		}
	}
	
	public static ProcessBuilder getSystemProcessBuilder(List<String> commands) {
		ProcessBuilder process = null;
		ArrayList<String> commandList = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			commandList.add("cmd");
			commandList.add("/c");
//			commandList.add("start");
		} else if (SystemUtils.IS_OS_LINUX) {
			commandList.add("bash");
			commandList.add("-c");
		} else if (SystemUtils.IS_OS_MAC) {
			commandList.add("/bin/bash");
			commandList.add("-c");
		}
		commandList.addAll(commands);
		process = new ProcessBuilder(commandList);
		return process;
	}
	
	public static class MessageDeleteThread extends Thread {

		public Message msg;
		public long duration;
		public long start;

		public MessageDeleteThread(Message msg, long duration) {
			this.msg = msg;
			this.duration = duration;
			start = System.currentTimeMillis();
		}

		@Override
		public void run() {
			boolean hasbeendeleted = false;

			while (!hasbeendeleted) {
				if ((start + duration) < System.currentTimeMillis()) {
					try {
						msg.delete();
					} catch (Exception e) {}
					hasbeendeleted = true;
				}
				try {
					Thread.sleep(150L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (hasbeendeleted) {
				this.interrupt();
			}
		}
	}

}
