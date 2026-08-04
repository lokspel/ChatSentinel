package dev._2lstudios.chatsentinel.bukkit.platform;

import dev._2lstudios.chatsentinel.bukkit.ChatSentinel;
import dev._2lstudios.chatsentinel.bukkit.text.BukkitMessageSink;
import dev._2lstudios.chatsentinel.bukkit.utils.BukkitLocaleUtil;
import dev._2lstudios.chatsentinel.bukkit.utils.FoliaAPI;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationAction;
import dev._2lstudios.chatsentinel.shared.platform.ChatUser;
import dev._2lstudios.chatsentinel.shared.text.WarningDeliverySettings;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BukkitChatUser implements ChatUser {
    private final ChatSentinel plugin;
    private final Player player;
    private final BukkitMessageSink messageSink;

    public BukkitChatUser(final ChatSentinel plugin, final Player player, final BukkitMessageSink messageSink) {
        this.plugin = plugin;
        this.player = player;
        this.messageSink = messageSink;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getLocale() {
        return BukkitLocaleUtil.getLocale(player);
    }

    @Override
    public String getServerName() {
        return plugin.getServer().getName();
    }

    @Override
    public boolean hasPermission(final String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(final String legacyMessage) {
        FoliaAPI.runTaskForEntity(plugin, player, new Runnable() {
            @Override
            public void run() {
                messageSink.sendMessage(player, legacyMessage);
            }
        }, new Runnable() {
            @Override
            public void run() {
                plugin.getLogger().fine("Skipped message delivery to retired player " + player.getName());
            }
        }, 0L);
    }

    @Override
    public void sendWarning(final String legacyMessage, final WarningDeliverySettings settings) {
        FoliaAPI.runTaskForEntity(plugin, player, new Runnable() {
            @Override
            public void run() {
                messageSink.sendWarning(player, legacyMessage, settings);
            }
        }, new Runnable() {
            @Override
            public void run() {
                plugin.getLogger().fine("Skipped message delivery to retired player " + player.getName());
            }
        }, 0L);
    }

    public void sendRequiredMessages(final ChatModerationAction action, final String reasonId,
            final List<String> legacyMessages) {
        if (action == null || !action.isTerminal() || reasonId == null || reasonId.trim().isEmpty()
                || legacyMessages == null || legacyMessages.isEmpty()) {
            throw new IllegalArgumentException("sendRequiredMessages requires terminal action, nonblank reasonId, and at least one message");
        }
        final List<String> snapshot = Collections.unmodifiableList(new ArrayList<>(legacyMessages));
        final String playerUuid = player.getUniqueId().toString();
        final String playerName = player.getName();
        boolean scheduled = FoliaAPI.tryRunTaskForEntity(plugin, player, new Runnable() {
            @Override
            public void run() {
                for (String msg : snapshot) {
                    messageSink.sendMessage(player, msg);
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                plugin.getLogger().warning("Required delivery retired: action=" + action + " reasonId=" + reasonId
                        + " player=" + playerName + "(" + playerUuid + ")");
            }
        }, 0L);
        if (!scheduled) {
            plugin.getLogger().warning("Required delivery rejected: action=" + action + " reasonId=" + reasonId
                    + " player=" + playerName + "(" + playerUuid + ")");
        }
    }
}
