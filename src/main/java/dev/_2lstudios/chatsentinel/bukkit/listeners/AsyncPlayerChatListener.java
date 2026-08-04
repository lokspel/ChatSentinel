package dev._2lstudios.chatsentinel.bukkit.listeners;

import dev._2lstudios.chatsentinel.bukkit.ChatSentinel;
import dev._2lstudios.chatsentinel.bukkit.platform.BukkitChatUser;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationAction;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationApplication;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationDecision;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayerManager;
import dev._2lstudios.chatsentinel.shared.chat.LegacyChatFormatRenderer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AsyncPlayerChatListener implements Listener {
    private final ChatSentinel plugin;
    private final ChatPlayerManager chatPlayerManager;
    private final LegacyChatFormatRenderer chatFormatRenderer;

    public AsyncPlayerChatListener(final ChatSentinel plugin, final ChatPlayerManager chatPlayerManager) {
        this.plugin = plugin;
        this.chatPlayerManager = chatPlayerManager;
        this.chatFormatRenderer = new LegacyChatFormatRenderer();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAsyncPlayerChatModeration(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final String originalMessage = event.getMessage();

        if (isIgnoredCommand(originalMessage)) {
            return;
        }

        final BukkitChatUser chatUser = new BukkitChatUser(plugin, player, plugin.getMessageSink());
        final ChatModerationDecision decision = plugin.getChatEventProcessor().process(chatUser, originalMessage, true);

        final ChatModerationAction action = decision.getAction();

        if (action.isTerminal()) {
            String renderedSelfOnly = null;
            if (action == ChatModerationAction.SELF_ONLY) {
                renderedSelfOnly = chatFormatRenderer.render(event.getFormat(), player.getDisplayName(), player.getName(), originalMessage);
            }
            final ChatModerationApplication application = ChatModerationApplication.forChat(decision, renderedSelfOnly);

            if (application.getCancelOriginal()) {
                event.setCancelled(true);
            }

            final java.util.List<String> messages = application.getSenderMessages();
            if (!messages.isEmpty()) {
                final String reasonId = decision.getReasonId().orElse("");
                chatUser.sendRequiredMessages(action, reasonId, messages);
            }
            return;
        }

        if (action.rewritesMessage()) {
            event.setMessage(decision.getMessage());
        }

        final ChatPlayer chatPlayer = chatPlayerManager.getPlayer(chatUser);
        chatPlayer.addLastMessage(decision.getMessage(), System.currentTimeMillis());
    }

    private boolean isIgnoredCommand(final String message) {
        return message != null && message.startsWith("/")
                && !plugin.getModuleManager().getGeneralModule().isCommand(message);
    }

    private String renderLine(final AsyncPlayerChatEvent event, final Player player, final String message) {
        return chatFormatRenderer.render(event.getFormat(), player.getDisplayName(), player.getName(), message);
    }
}
