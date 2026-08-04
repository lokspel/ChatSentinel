package dev._2lstudios.chatsentinel.bukkit.listeners;

import dev._2lstudios.chatsentinel.bukkit.platform.BukkitChatUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import dev._2lstudios.chatsentinel.bukkit.ChatSentinel;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationAction;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationDecision;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.socialspy.SocialSpyService;

public class ServerCommandListener implements Listener {
	private ChatPlayerManager chatPlayerManager;
	private final SocialSpyService socialSpyService;

	public ServerCommandListener(final ChatPlayerManager chatPlayerManager,
			final dev._2lstudios.chatsentinel.shared.chat.ChatNotificationManager chatNotificationManager,
			final SocialSpyService socialSpyService) {
		this.chatPlayerManager = chatPlayerManager;
		this.socialSpyService = socialSpyService;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onServerCommand(PlayerCommandPreprocessEvent event) {
		// Get player
		Player player = event.getPlayer();

		// Get event variables
		String message = event.getMessage();
		// Get chat player
		BukkitChatUser chatUser = new BukkitChatUser(ChatSentinel.getInstance(), player, ChatSentinel.getInstance().getMessageSink());
		ChatPlayer chatPlayer = chatPlayerManager.getPlayer(chatUser);
		socialSpyService.publishCommand(chatUser, message);

		// Process the event
		ChatModerationDecision decision = ChatSentinel.getInstance().getChatEventProcessor().process(chatUser, message, true);

		// Apply modifiers to event
		final ChatModerationAction action = decision.getAction();
		if (action.isTerminal()) {
			event.setCancelled(true);
			final java.util.List<String> messages = java.util.Collections.singletonList(
					decision.getPlayerFeedback().orElse(""));
			final String reasonId = decision.getReasonId().orElse("");
			chatUser.sendRequiredMessages(action, reasonId, messages);
		} else {
			if (action.rewritesMessage()) {
				event.setMessage(decision.getMessage());
			}
			chatPlayer.addLastCommand(System.currentTimeMillis());
		}
	}
}
