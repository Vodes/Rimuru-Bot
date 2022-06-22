package pw.vodes.rimuru.verification;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

import pw.vodes.rimuru.Main;

public class VerificationListener implements ReactionAddListener {

	// This could and should get a rewrite
	
	@Override
	public void onReactionAdd(ReactionAddEvent event) {
		var role = Main.getServer().getRoleById(Main.getConfig().verification_role).get();
		User user;
		try {
			user = Main.api.getUserById(event.getUserId()).get();
			if(user != null && !role.hasUser(user)) {
				var math = new VerificationMath();
				try {
					var thread = event.getChannel().asServerTextChannel().get().createThread(ChannelType.SERVER_PUBLIC_THREAD, user.getDiscriminatedName(), 60, true).get();
					var msg = thread.sendMessage(user.getMentionTag() + " " + math.getMessage()).get();
					deleteMessages(event);

					AtomicBoolean threadIsGone = new AtomicBoolean(false);
					thread.addMessageCreateListener(e -> {
						var disable = false;
						if(e.getMessageAuthor().getId() == user.getId() && !disable) {
							try {
								var answer = 0D;
								try {
									answer = Double.parseDouble(e.getMessageContent());
								} catch (Exception e2) {
									try {
										var embed = new EmbedBuilder().setTitle("Thinks he is funny")
												.setAuthor(user)
												.addField("Question", math.getMessage())
												.addField("Correct Result", "" + math.getResult(), true)
												.addField("Answer", "" + e.getMessageContent(), true)
												.setFooter("UserID: " + user.getIdAsString());
										if(Main.getHallOfShameChannel() != null)
											Main.getHallOfShameChannel().sendMessage(embed);
										else
											Main.getServer().getChannelById(Main.getConfig().general_chat).get().asServerTextChannel().get().sendMessage(embed);
										return;
									} catch(Exception ex) {}
								}
								if(answer == math.getResult()) {
									role.addUser(e.getMessageAuthor().asUser().get());
								} else {
									if(StringUtils.isNotBlank(Main.getConfig().general_chat)) {
										try {
											var embed = new EmbedBuilder().setTitle("Failed verification").setDescription("Laugh at this user")
													.setAuthor(user)
													.addField("Question", math.getMessage())
													.addField("Correct Result", "" + math.getResult(), true)
													.addField("Answer", "" + answer, true)
													.setFooter("UserID: " + user.getIdAsString());
											if(Main.getHallOfShameChannel() != null)
												Main.getHallOfShameChannel().sendMessage(embed);
											else
												Main.getServer().getChannelById(Main.getConfig().general_chat).get().asServerTextChannel().get().sendMessage(embed);
										} catch(Exception ex) {}
									}
									Main.getServer().timeoutUser(user, Duration.ofMinutes(5), "Incorrect Verification");
								}
								disable = true;
								e.getMessage().delete();
								Main.getServer().getThreadChannelById(thread.getId()).get().delete();
								threadIsGone.set(true);
							} catch (Exception e2) {}
						}
					}).removeAfter(17, TimeUnit.SECONDS);
					new Thread(() -> {
						try {
							Thread.sleep(17000L);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						deleteMessages(event);
						try {
							if(!threadIsGone.get())
								Main.getServer().getThreadChannelById(thread.getId()).get().delete();
						} catch (Exception e2) {}
					}).start();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException | ExecutionException e1) {
			e1.printStackTrace();
		}
	}
	
	private void deleteMessages(ReactionAddEvent event) {
		List<Message> msgs;
		try {
			msgs = event.getChannel().getMessages(10).get().stream().filter(o -> o.getAuthor().isYourself()).collect(Collectors.toList());
			if(!msgs.isEmpty()) {
				if(msgs.size() < 2)
					msgs.get(0).delete();
				else
					event.getServerTextChannel().get().bulkDelete(msgs);
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

}
