package dev._2lstudios.chatsentinel.shared.chat;

import java.util.Objects;
import java.util.Optional;

public final class ChatModerationDecision {
    private final ChatModerationAction action;
    private final String message;
    private final Optional<String> playerFeedback;
    private final Optional<String> reasonId;
    private final Optional<String> reasonName;

    private ChatModerationDecision(ChatModerationAction action, String message,
            Optional<String> playerFeedback, Optional<String> reasonId, Optional<String> reasonName) {
        this.action = action;
        this.message = message;
        this.playerFeedback = playerFeedback;
        this.reasonId = reasonId;
        this.reasonName = reasonName;
    }

    public static ChatModerationDecision pass(String message) {
        return new ChatModerationDecision(
                ChatModerationAction.PASS,
                message == null ? "" : message,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public static ChatModerationDecision rewrite(String message) {
        return new ChatModerationDecision(
                ChatModerationAction.REWRITE,
                message == null ? "" : message,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public static ChatModerationDecision block(String message, String playerFeedback,
            String reasonId, String reasonName) {
        String msg = message == null ? "" : message;
        String fb = playerFeedback == null ? "" : playerFeedback.trim();
        String rid = reasonId == null ? "" : reasonId.trim();
        String rname = reasonName == null ? "" : reasonName.trim();
        if (fb.isEmpty()) {
            throw new IllegalArgumentException("playerFeedback must be nonblank");
        }
        if (rid.isEmpty()) {
            throw new IllegalArgumentException("reasonId must be nonblank");
        }
        if (rname.isEmpty()) {
            throw new IllegalArgumentException("reasonName must be nonblank");
        }
        return new ChatModerationDecision(
                ChatModerationAction.BLOCK,
                msg,
                Optional.of(fb),
                Optional.of(rid),
                Optional.of(rname)
        );
    }

    public static ChatModerationDecision selfOnly(String message, String playerFeedback,
            String reasonId, String reasonName) {
        String msg = message == null ? "" : message;
        String fb = playerFeedback == null ? "" : playerFeedback.trim();
        String rid = reasonId == null ? "" : reasonId.trim();
        String rname = reasonName == null ? "" : reasonName.trim();
        if (fb.isEmpty()) {
            throw new IllegalArgumentException("playerFeedback must be nonblank");
        }
        if (rid.isEmpty()) {
            throw new IllegalArgumentException("reasonId must be nonblank");
        }
        if (rname.isEmpty()) {
            throw new IllegalArgumentException("reasonName must be nonblank");
        }
        return new ChatModerationDecision(
                ChatModerationAction.SELF_ONLY,
                msg,
                Optional.of(fb),
                Optional.of(rid),
                Optional.of(rname)
        );
    }

    public ChatModerationAction getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public Optional<String> getPlayerFeedback() {
        return playerFeedback;
    }

    public Optional<String> getReasonId() {
        return reasonId;
    }

    public Optional<String> getReasonName() {
        return reasonName;
    }

    public boolean isTerminal() {
        return action.isTerminal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatModerationDecision that = (ChatModerationDecision) o;
        return action == that.action &&
                Objects.equals(message, that.message) &&
                Objects.equals(playerFeedback, that.playerFeedback) &&
                Objects.equals(reasonId, that.reasonId) &&
                Objects.equals(reasonName, that.reasonName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, message, playerFeedback, reasonId, reasonName);
    }

    @Override
    public String toString() {
        return "ChatModerationDecision{" +
                "action=" + action +
                ", message='" + message + '\'' +
                ", playerFeedback=" + playerFeedback +
                ", reasonId=" + reasonId +
                ", reasonName=" + reasonName +
                '}';
    }
}
