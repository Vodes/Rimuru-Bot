package pw.vodes.rimuru;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.javacord.api.entity.message.Message;

public class Util {

	public static SecureRandom random = new SecureRandom();
	public static final Pattern messageSplitPattern = Pattern.compile("(?=\\S)[^\\\"\\s]*(?:\\\"[^\\\\\\\"]*(?:\\\\[\\s\\S][^\\\\\\\"]*)*\\\"[^\\\"\\s]*)*");

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
