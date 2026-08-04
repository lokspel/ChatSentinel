package dev._2lstudios.chatsentinel.bungee.listeners;

import dev._2lstudios.chatsentinel.bungee.ChatSentinel;
import dev._2lstudios.chatsentinel.bungee.platform.BungeeChatUser;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationAction;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationApplication;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationDecision;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.modules.WhitelistModule;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ChatListener implements Listener {
	private WhitelistModule whitelistModule;
	private ChatPlayerManager chatPlayerManager;
	private final ChatSentinel plugin;

	public ChatListener(ChatSentinel plugin, WhitelistModule whitelistModule, ChatPlayerManager chatPlayerManager) {
		this.plugin = plugin;
		this.whitelistModule = whitelistModule;
		this.chatPlayerManager = chatPlayerManager;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onChatEvent(ChatEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Connection sender = event.getSender();
		if (!(sender instanceof ProxiedPlayer)) {
			return;
		}

		ProxiedPlayer player = (ProxiedPlayer) sender;

		if (player.getServer() != null) {
			String playerCurrentServer = player.getServer().getInfo().getName();
			if (whitelistModule.getWhitelistedServers().contains(playerCurrentServer)) {
				return;
			}
		}

		String message = event.getMessage();
		boolean isCommand = event.isCommand();

		BungeeChatUser chatUser = new BungeeChatUser(player, plugin.getMessageSink());
		ChatPlayer chatPlayer = chatPlayerManager.getPlayer(chatUser);

		ChatModerationDecision decision = plugin.getChatEventProcessor().process(chatUser, message, true);

		ChatModerationAction action = decision.getAction();

		if (action.isTerminal()) {
			ChatModerationApplication application = isCommand
					? ChatModerationApplication.forCommand(decision)
					: ChatModerationApplication.forChat(decision, message);

			if (application.getCancelOriginal()) {
				event.setCancelled(true);
			}

			for (String msg : application.getSenderMessages()) {
				chatUser.sendMessage(msg);
			}
			return;
		}

		if (action.rewritesMessage()) {
			event.setMessage(decision.getMessage());
		}

		if (action == ChatModerationAction.PASS || action == ChatModerationAction.REWRITE) {
			if (isCommand) {
				chatPlayer.addLastCommand(System.currentTimeMillis());
			} else {
				chatPlayer.addLastMessage(decision.getMessage(), System.currentTimeMillis());
			}
		}
	}
}
