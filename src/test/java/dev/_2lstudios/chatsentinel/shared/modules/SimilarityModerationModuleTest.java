package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatModerationAction;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SimilarityModerationModuleTest {
    @Test
    public void process_blocksSpamAfterSpam() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");
        chatPlayer.addLastMessage("spam", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "spaaam", "en");

        assertNotNull(result);
        assertEquals(ChatModerationAction.BLOCK, result.getAction());
        assertTrue(result.getPlayerMessage().orElse("").contains("%"));
    }

    @Test
    public void process_blocksSpaaaaaamAfterSpam() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");
        chatPlayer.addLastMessage("spam", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "spaaaaaam", "en");

        assertNotNull(result);
        assertEquals(ChatModerationAction.BLOCK, result.getAction());
        assertTrue(result.getPlayerMessage().orElse("").contains("%"));
    }

    @Test
    public void process_allowsShortRepeatedMessage_whenBelowMinLength() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");
        chatPlayer.addLastMessage("ok", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "ok", "en");

        assertNull(result);
    }

    @Test
    public void process_usesPlayerSpecificHistory() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer player1 = createChatPlayer("Steve");
        ChatPlayer player2 = createChatPlayer("Alex");
        player1.addLastMessage("spam", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(player2, messagesModule, "Alex", "spaaam", "en");

        assertNull(result);
    }

    @Test
    public void process_allowsFirstMessage() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "hello world", "en");

        assertNull(result);
    }

    @Test
    public void process_ignoresCommands() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");
        chatPlayer.addLastMessage("spam", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "/spam", "en");

        assertNull(result);
    }

    @Test
    public void process_blocksRepeatedMessageAtThreshold() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");
        chatPlayer.addLastMessage("message", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "messag", "en");

        assertNotNull(result);
        assertEquals(ChatModerationAction.BLOCK, result.getAction());
        assertTrue(result.getPlayerMessage().orElse("").contains("%"));
    }

    @Test
    public void process_disabledModule_returnsNull() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(false, "Similarity", 75.0D, 3, 4, true, true, true);
        MessagesModule messagesModule = createMessagesModule();
        ChatPlayer chatPlayer = createChatPlayer("Steve");
        chatPlayer.addLastMessage("spam", System.currentTimeMillis());

        ChatEventResult result = module.processEvent(chatPlayer, messagesModule, "Steve", "spaaam", "en");

        assertNull(result);
    }

    @Test
    public void getThresholdPercentage_returnsConfiguredValue() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 85.0D, 3, 4, true, true, true);
        assertEquals(85.0D, module.getThresholdPercentage(), 0.01);
    }

    @Test
    public void getCompareLastMessages_returnsConfiguredValue() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 5, 4, true, true, true);
        assertEquals(5, module.getCompareLastMessages());
    }

    @Test
    public void getMinNormalizedLength_returnsConfiguredValue() {
        SimilarityModerationModule module = new SimilarityModerationModule();
        module.loadData(true, "Similarity", 75.0D, 3, 6, true, true, true);
        assertEquals(6, module.getMinNormalizedLength());
    }

    private MessagesModule createMessagesModule() {
        MessagesModule messagesModule = new MessagesModule();
        java.util.Map<String, java.util.Map<String, String>> locales = new java.util.HashMap<String, java.util.Map<String, String>>();
        java.util.Map<String, String> en = new java.util.HashMap<String, String>();
        en.put("similarity_warn_message", "Stop writing similar messages. Similarity: %similarity%.");
        locales.put("en", en);
        messagesModule.loadData("en", locales);
        return messagesModule;
    }

    private ChatPlayer createChatPlayer(String name) {
        return new ChatPlayer(UUID.randomUUID());
    }
}