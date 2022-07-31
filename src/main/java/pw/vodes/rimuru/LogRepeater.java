package pw.vodes.rimuru;

import java.util.ArrayList;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class LogRepeater {
	
	public static ArrayList<EmbedBuilder> embedsToSend = new ArrayList<>();
	
	public static void start() {
		new Thread(() -> {
			while(true) {
				var toSend = new ArrayList<EmbedBuilder>();
				toSend.addAll(embedsToSend);
				embedsToSend.clear();
				for(var embed : toSend) {
					try {
							Main.getConfig().getOtherLogChannel().sendMessage(embed);
					} catch (Exception e) { embedsToSend.add(embed); }
				}
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}
