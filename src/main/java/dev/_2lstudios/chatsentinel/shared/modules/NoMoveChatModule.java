package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class NoMoveChatModule {
    private boolean enabled;
    private String bypassPermission;
    private double minDistanceBlocks = 5.0D;
    private boolean allowTeleport = true;

    public void loadData(boolean enabled, String bypassPermission) {
        loadData(enabled, bypassPermission, 5.0D, true);
    }

    public void loadData(boolean enabled, String bypassPermission, double minDistanceBlocks, boolean allowTeleport) {
        this.enabled = enabled;
        this.bypassPermission = bypassPermission;
        this.minDistanceBlocks = minDistanceBlocks <= 0.0D ? 0.0D : minDistanceBlocks;
        this.allowTeleport = allowTeleport;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public double getMinDistanceBlocks() {
        return minDistanceBlocks;
    }

    public double getMinDistanceSquared() {
        return minDistanceBlocks * minDistanceBlocks;
    }

    public boolean isAllowTeleport() {
        return allowTeleport;
    }

    public ChatEventResult processEvent(ChatPlayer chatPlayer, String originalMessage) {
        if (enabled && !chatPlayer.hasMovementGatePassed()) {
			return ChatEventResult.block(originalMessage);
        }

        return null;
    }
}
