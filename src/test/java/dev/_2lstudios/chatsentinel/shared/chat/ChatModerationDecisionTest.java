package dev._2lstudios.chatsentinel.shared.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class ChatModerationDecisionTest {

    @Test(expected = IllegalArgumentException.class)
    public void block_rejectsBlankFeedback() {
        ChatModerationDecision.block("msg", "", "id", "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void block_rejectsBlankReasonId() {
        ChatModerationDecision.block("msg", "fb", "", "name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void selfOnly_rejectsBlankReasonName() {
        ChatModerationDecision.selfOnly("msg", "fb", "id", "");
    }

    @Test
    public void rewrite_hasRewriteAction() {
        assertEquals(ChatModerationAction.REWRITE, ChatModerationDecision.rewrite("msg").getAction());
    }

    @Test
    public void terminalReason_isRetained() {
        ChatModerationDecision decision = ChatModerationDecision.block("msg", "fb", "reason-id", "reason-name");
        assertEquals("reason-id", decision.getReasonId().orElse(null));
        assertEquals("reason-name", decision.getReasonName().orElse(null));
    }

    @Test
    public void pass_hasPassAction() {
        assertEquals(ChatModerationAction.PASS, ChatModerationDecision.pass("msg").getAction());
    }

    @Test
    public void pass_hasNoPlayerFeedback() {
        assertFalse(ChatModerationDecision.pass("msg").getPlayerFeedback().isPresent());
    }

    @Test
    public void block_hasBlockAction() {
        assertEquals(ChatModerationAction.BLOCK,
                ChatModerationDecision.block("msg", "fb", "id", "name").getAction());
    }

    @Test
    public void selfOnly_hasSelfOnlyAction() {
        assertEquals(ChatModerationAction.SELF_ONLY,
                ChatModerationDecision.selfOnly("msg", "fb", "id", "name").getAction());
    }

    @Test
    public void selfOnly_isTerminal() {
        assertTrue(ChatModerationDecision.selfOnly("msg", "fb", "id", "name").isTerminal());
    }

    @Test
    public void block_isTerminal() {
        assertTrue(ChatModerationDecision.block("msg", "fb", "id", "name").isTerminal());
    }

    @Test
    public void pass_isNotTerminal() {
        assertFalse(ChatModerationDecision.pass("msg").isTerminal());
    }
}
