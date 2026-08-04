package dev._2lstudios.chatsentinel.shared.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ChatModerationApplication {
    private final ChatModerationAction action;
    private final boolean cancelOriginal;
    private final Optional<String> replacementMessage;
    private final List<String> senderMessages;
    private final boolean recordHistory;

    private ChatModerationApplication(ChatModerationAction action, boolean cancelOriginal,
            Optional<String> replacementMessage, List<String> senderMessages, boolean recordHistory) {
        this.action = action;
        this.cancelOriginal = cancelOriginal;
        this.replacementMessage = replacementMessage;
        this.senderMessages = Collections.unmodifiableList(new ArrayList<>(senderMessages));
        this.recordHistory = recordHistory;
    }

    public static ChatModerationApplication forChat(ChatModerationDecision decision,
            String renderedSelfOnlyMessage) {
        ChatModerationAction action = decision.getAction();
        List<String> messages = new ArrayList<>();

        switch (action) {
            case PASS:
                return new ChatModerationApplication(
                        action, false, Optional.empty(), Collections.emptyList(), true);
            case REWRITE:
                return new ChatModerationApplication(
                        action, false, Optional.of(decision.getMessage()), Collections.emptyList(), true);
            case BLOCK:
                messages.add(decision.getPlayerFeedback().orElseThrow(() ->
                        new IllegalStateException("BLOCK requires playerFeedback")));
                return new ChatModerationApplication(
                        action, true, Optional.empty(), messages, false);
            case SELF_ONLY:
                if (renderedSelfOnlyMessage == null || renderedSelfOnlyMessage.trim().isEmpty()) {
                    throw new IllegalArgumentException("SELF_ONLY forChat requires nonblank renderedSelfOnlyMessage");
                }
                messages.add(decision.getPlayerFeedback().orElseThrow(() ->
                        new IllegalStateException("SELF_ONLY requires playerFeedback")));
                messages.add(renderedSelfOnlyMessage);
                return new ChatModerationApplication(
                        action, true, Optional.empty(), messages, false);
            default:
                throw new IllegalStateException("Unknown action: " + action);
        }
    }

    public static ChatModerationApplication forCommand(ChatModerationDecision decision) {
        ChatModerationAction action = decision.getAction();
        List<String> messages = new ArrayList<>();

        switch (action) {
            case PASS:
                return new ChatModerationApplication(
                        action, false, Optional.empty(), Collections.emptyList(), true);
            case REWRITE:
                return new ChatModerationApplication(
                        action, false, Optional.of(decision.getMessage()), Collections.emptyList(), true);
            case BLOCK:
            case SELF_ONLY:
                messages.add(decision.getPlayerFeedback().orElseThrow(() ->
                        new IllegalStateException("terminal action requires playerFeedback")));
                return new ChatModerationApplication(
                        action, true, Optional.empty(), messages, false);
            default:
                throw new IllegalStateException("Unknown action: " + action);
        }
    }

    public ChatModerationAction getAction() {
        return action;
    }

    public boolean getCancelOriginal() {
        return cancelOriginal;
    }

    public Optional<String> getReplacementMessage() {
        return replacementMessage;
    }

    public List<String> getSenderMessages() {
        return senderMessages;
    }

    public boolean getRecordHistory() {
        return recordHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatModerationApplication that = (ChatModerationApplication) o;
        return cancelOriginal == that.cancelOriginal &&
                recordHistory == that.recordHistory &&
                action == that.action &&
                Objects.equals(replacementMessage, that.replacementMessage) &&
                Objects.equals(senderMessages, that.senderMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, cancelOriginal, replacementMessage, senderMessages, recordHistory);
    }

    @Override
    public String toString() {
        return "ChatModerationApplication{" +
                "action=" + action +
                ", cancelOriginal=" + cancelOriginal +
                ", replacementMessage=" + replacementMessage +
                ", senderMessages=" + senderMessages +
                ", recordHistory=" + recordHistory +
                '}';
    }
}
