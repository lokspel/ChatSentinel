package dev._2lstudios.chatsentinel.shared.chat;

import java.util.Objects;
import java.util.Optional;

import dev._2lstudios.chatsentinel.shared.moderation.ModerationViolation;

public final class ChatEventResult {
    private final String message;
    private final ChatModerationAction action;
    private final boolean notify;
    private final Optional<ModerationViolation> violation;
    private final Optional<String> playerMessage;

    private ChatEventResult(String message, ChatModerationAction action, boolean notify,
            Optional<ModerationViolation> violation, Optional<String> playerMessage) {
        this.message = message == null ? "" : message;
        this.action = action;
        this.notify = notify;
        this.violation = violation;
        this.playerMessage = playerMessage;
    }

    public static ChatEventResult pass(String message) {
        return new ChatEventResult(
                message == null ? "" : message,
                ChatModerationAction.PASS,
                true,
                Optional.empty(),
                Optional.empty()
        );
    }

    public static ChatEventResult rewrite(String message) {
        return new ChatEventResult(
                message == null ? "" : message,
                ChatModerationAction.REWRITE,
                true,
                Optional.empty(),
                Optional.empty()
        );
    }

    public static ChatEventResult block(String message) {
        return new ChatEventResult(
                message == null ? "" : message,
                ChatModerationAction.BLOCK,
                true,
                Optional.empty(),
                Optional.empty()
        );
    }

    public static ChatEventResult selfOnly(String message) {
        return new ChatEventResult(
                message == null ? "" : message,
                ChatModerationAction.SELF_ONLY,
                true,
                Optional.empty(),
                Optional.empty()
        );
    }

    public ChatEventResult withNotify(boolean notify) {
        return new ChatEventResult(message, action, notify, violation, playerMessage);
    }

    public ChatEventResult withViolation(ModerationViolation v) {
        return new ChatEventResult(message, action, notify,
                Optional.of(Objects.requireNonNull(v, "violation")), playerMessage);
    }

    public ChatEventResult withPlayerMessage(String pm) {
        if (pm == null || pm.trim().isEmpty()) {
            return new ChatEventResult(message, action, notify, violation, Optional.empty());
        }
        return new ChatEventResult(message, action, notify, violation, Optional.of(pm));
    }

    public String getMessage() {
        return message;
    }

    public ChatModerationAction getAction() {
        return action;
    }

    public boolean isNotify() {
        return notify;
    }

    public Optional<ModerationViolation> getViolation() {
        return violation;
    }

    public Optional<String> getPlayerMessage() {
        return playerMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatEventResult that = (ChatEventResult) o;
        return notify == that.notify &&
                action == that.action &&
                Objects.equals(message, that.message) &&
                Objects.equals(violation, that.violation) &&
                Objects.equals(playerMessage, that.playerMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, action, notify, violation, playerMessage);
    }

    @Override
    public String toString() {
        return "ChatEventResult{" +
                "message='" + message + '\'' +
                ", action=" + action +
                ", notify=" + notify +
                ", violation=" + violation +
                ", playerMessage=" + playerMessage +
                '}';
    }
}
