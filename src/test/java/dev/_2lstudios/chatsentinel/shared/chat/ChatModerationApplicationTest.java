package dev._2lstudios.chatsentinel.shared.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ChatModerationApplicationTest {

    @Test
    public void blockChat_cancelsAndSendsFeedback() {
        ChatModerationDecision decision = ChatModerationDecision.block("msg", "feedback", "id", "name");
        ChatModerationApplication app = ChatModerationApplication.forChat(decision, "ignored");

        assertTrue(app.getCancelOriginal());
        assertEquals(1, app.getSenderMessages().size());
        assertFalse(app.getRecordHistory());
    }

    @Test
    public void selfOnlyChat_sendsFeedbackBeforeEcho() {
        ChatModerationDecision selfOnly = ChatModerationDecision.selfOnly("msg", "feedback", "id", "name");
        ChatModerationApplication app = ChatModerationApplication.forChat(selfOnly, "PlayerName: msg");

        assertEquals(2, app.getSenderMessages().size());
        assertEquals("feedback", app.getSenderMessages().get(0));
        assertEquals("PlayerName: msg", app.getSenderMessages().get(1));
    }

    @Test
    public void selfOnlyCommand_doesNotEchoCommand() {
        ChatModerationDecision selfOnly = ChatModerationDecision.selfOnly("msg", "feedback", "id", "name");
        ChatModerationApplication app = ChatModerationApplication.forCommand(selfOnly);

        assertEquals(1, app.getSenderMessages().size());
        assertEquals("feedback", app.getSenderMessages().get(0));
    }

    @Test
    public void pass_doesNotCreateReplacement() {
        ChatModerationDecision decision = ChatModerationDecision.pass("msg");
        ChatModerationApplication app = ChatModerationApplication.forChat(decision, "ignored");

        assertFalse(app.getCancelOriginal());
        assertFalse(app.getReplacementMessage().isPresent());
        assertTrue(app.getRecordHistory());
    }

    @Test
    public void rewrite_exposesReplacement() {
        ChatModerationDecision decision = ChatModerationDecision.rewrite("rewritten message");
        ChatModerationApplication app = ChatModerationApplication.forChat(decision, "ignored");

        assertFalse(app.getCancelOriginal());
        assertTrue(app.getReplacementMessage().isPresent());
        assertEquals("rewritten message", app.getReplacementMessage().get());
    }

    @Test
    public void terminalApplication_doesNotRecordHistory() {
        ChatModerationDecision block = ChatModerationDecision.block("msg", "fb", "id", "name");
        ChatModerationApplication blockApp = ChatModerationApplication.forChat(block, "ignored");
        assertFalse(blockApp.getRecordHistory());

        ChatModerationDecision selfOnly = ChatModerationDecision.selfOnly("msg", "fb", "id", "name");
        ChatModerationApplication selfOnlyApp = ChatModerationApplication.forChat(selfOnly, "PlayerName: msg");
        assertFalse(selfOnlyApp.getRecordHistory());
    }

    @Test
    public void blockCommand_cancelsAndSendsFeedback() {
        ChatModerationDecision decision = ChatModerationDecision.block("msg", "feedback", "id", "name");
        ChatModerationApplication app = ChatModerationApplication.forCommand(decision);

        assertTrue(app.getCancelOriginal());
        assertEquals(1, app.getSenderMessages().size());
        assertEquals("feedback", app.getSenderMessages().get(0));
    }

    @Test
    public void passCommand_recordsHistory() {
        ChatModerationDecision decision = ChatModerationDecision.pass("msg");
        ChatModerationApplication app = ChatModerationApplication.forCommand(decision);

        assertTrue(app.getRecordHistory());
    }
}
