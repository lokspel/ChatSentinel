package dev._2lstudios.chatsentinel.velocity.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationAction;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationApplication;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationDecision;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.LegacyChatFormatRenderer;
import dev._2lstudios.chatsentinel.shared.modules.WhitelistModule;
import dev._2lstudios.chatsentinel.velocity.ChatSentinel;
import dev._2lstudios.chatsentinel.velocity.platform.VelocityChatUser;

public class ChatListener {
	private final ChatSentinel plugin;
	private final WhitelistModule whitelistModule;
	private final LegacyChatFormatRenderer chatFormatRenderer;

	public ChatListener(ChatSentinel plugin, WhitelistModule whitelistModule) {
		this.plugin = plugin;
		this.whitelistModule = whitelistModule;
		this.chatFormatRenderer = new LegacyChatFormatRenderer();
	}

	@Subscribe(order = PostOrder.LAST)
	public void onChatEvent(PlayerChatEvent event) {
		if (!event.getResult().isAllowed()) {
			return;
		}

		// Sender
		Player player = event.getPlayer();
		
		if (player == null) {
			return;
		}

		// Check if the player's current server is on the whitelist
		if (player.getCurrentServer().isPresent()) {
			String playerCurrentServer = player.getCurrentServer().get().getServerInfo().getName();
			if (whitelistModule.getWhitelistedServers().contains(playerCurrentServer)) {
				return;
			}
		}

		// Get event variables
		String message = event.getMessage();

		// Get chat player
		VelocityChatUser chatUser = new VelocityChatUser(player, plugin.getMessageSink());
		ChatPlayer chatPlayer = plugin.getChatPlayerManager().getPlayer(chatUser);

		// Process the event
		if (plugin.getModuleManager().isSignedChatWarnOnly()) {
			plugin.getChatEventProcessor().process(chatUser, message, false);
			trackAllowedMessage(event, chatPlayer, player, message);
			return;
		}

		final boolean isCommand = message.startsWith("/");
		final ChatModerationDecision decision = plugin.getChatEventProcessor().process(chatUser, message, true);
		final ChatModerationAction action = decision.getAction();

		if (action.isTerminal()) {
			if (isCommand) {
				final ChatModerationApplication application = ChatModerationApplication.forCommand(decision);
				event.setResult(PlayerChatEvent.ChatResult.denied());
				for (String msg : application.getSenderMessages()) {
					chatUser.sendMessage(msg);
				}
			} else {
				final String renderedSelfOnly = chatFormatRenderer.render("<%s> %s", player.getUsername(), player.getUsername(), message);
				final ChatModerationApplication application = ChatModerationApplication.forChat(decision, renderedSelfOnly);
				event.setResult(PlayerChatEvent.ChatResult.denied());
				for (String msg : application.getSenderMessages()) {
					chatUser.sendMessage(msg);
				}
			}
		} else {
			if (action.rewritesMessage()) {
				event.setResult(PlayerChatEvent.ChatResult.message(decision.getMessage()));
			}
			trackAllowedMessage(event, chatPlayer, player, decision.getMessage());
		}
	}

	private void trackAllowedMessage(PlayerChatEvent event, ChatPlayer chatPlayer, Player player, String message) {
		if (!event.getResult().isAllowed()) {
			return;
		}

		if (message.startsWith("/")) {
			chatPlayer.addLastCommand(System.currentTimeMillis());
		} else {
			chatPlayer.addLastMessage(message, System.currentTimeMillis());
		}
	}
}
