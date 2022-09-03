package pw.vodes.rimuru;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.javacord.api.entity.message.Message;

public class Util {

	public static SecureRandom random = new SecureRandom();
	public static final Pattern messageSplitPattern = Pattern.compile("(?=\\S)[^\\\"\\s]*(?:\\\"[^\\\\\\\"]*(?:\\\\[\\s\\S][^\\\\\\\"]*)*\\\"[^\\\"\\s]*)*");
	public static final Pattern imageUrlPattern = Pattern.compile("[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/=]*)(.jpg|.png|.jpeg|.gif)", Pattern.CASE_INSENSITIVE);
	public static final Pattern u2PasskeyURLPattern = Pattern.compile("https?:\\/\\/u2\\.dmhy\\.org\\/torrentrss\\.php.*(passkey=[^& ]*).*", Pattern.CASE_INSENSITIVE);
	public static final Pattern nyaaAutismPattern = Pattern.compile("(<.*>)(#\\d*) \\| (.+)(?:<\\/a>) \\| (\\d*\\.\\d* (?:GiB|MiB|TiB)) \\| ([^|]*) \\| (.*)", Pattern.CASE_INSENSITIVE);

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
