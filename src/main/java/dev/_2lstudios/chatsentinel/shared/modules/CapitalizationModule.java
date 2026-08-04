package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Supplier;

public class CapitalizationModule extends ModerationModule {
    private boolean correct;
    private boolean capitalizeFirstLetter = true;
    private int maxUppercase;
    private boolean whitelistPlayerNames;
    private String bypassPermission = "chatsentinel.bypass.capitalization";
    private String[] whitelist = new String[0];
    private Supplier<Collection<String>> onlinePlayerNamesSupplier = Collections::<String>emptyList;

    public void loadData(boolean enabled, String customName, boolean correct, int max, int maxWarns,
            String warnNotification, boolean webhookEnabled, String[] commands, boolean whitelistPlayerNames,
            String[] whitelist, Supplier<Collection<String>> onlinePlayerNamesSupplier) {
        loadData(enabled, customName, correct, false, max, maxWarns, warnNotification, webhookEnabled, commands,
                whitelistPlayerNames, whitelist, onlinePlayerNamesSupplier, "");
    }

    public void loadData(boolean enabled, String customName, boolean correct, boolean capitalizeFirstLetter, int max, int maxWarns,
            String warnNotification, boolean webhookEnabled, String[] commands, boolean whitelistPlayerNames,
            String[] whitelist, Supplier<Collection<String>> onlinePlayerNamesSupplier, String bypassPermission) {
        setEnabled(enabled);
        setMaxWarns(maxWarns);
        setWarnNotification(warnNotification == null ? "" : warnNotification);
        setWebhookEnabled(webhookEnabled);
        setCommands(commands == null ? new String[0] : commands);
        setCustomName(customName);
        this.correct = correct;
        this.capitalizeFirstLetter = capitalizeFirstLetter;
        this.maxUppercase = max;
        this.whitelistPlayerNames = whitelistPlayerNames;
        this.whitelist = whitelist == null ? new String[0] : whitelist;
        this.onlinePlayerNamesSupplier = onlinePlayerNamesSupplier == null
                ? Collections::emptyList
                : onlinePlayerNamesSupplier;
        this.bypassPermission = bypassPermission == null || bypassPermission.trim().isEmpty()
                ? ""
                : bypassPermission;
    }

    public boolean isCorrect() {
        return correct;
    }

    public long uppercaseCount(String string) {
        return string == null ? 0L : string.codePoints().filter(c -> c >= 'A' && c <= 'Z').count();
    }

    @Override
    public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
            String originalMessage, String lang) {
        if (!isEnabled() || originalMessage == null || originalMessage.isEmpty()) {
            return null;
        }

        final String countedMessage = removeWhitelistEntries(originalMessage);
        final boolean excessiveUppercase = uppercaseCount(countedMessage) > maxUppercase;
        final boolean needsFirstLetter = capitalizeFirstLetter && startsWithLowercaseLetter(originalMessage);

        if (!excessiveUppercase && !needsFirstLetter) {
            return null;
        }

        String correctedMessage = originalMessage;
        if (correct) {
            if (excessiveUppercase) {
                correctedMessage = correctedMessage.toLowerCase(Locale.ROOT);
            }
            if (capitalizeFirstLetter) {
                correctedMessage = capitalizeFirstAlphabetic(correctedMessage);
            }
        }

        if (!excessiveUppercase && needsFirstLetter) {
            return ChatEventResult.rewrite(correctedMessage).withNotify(false);
        }
        return ChatEventResult.rewrite(correctedMessage);
    }

    @Override
    public String getName() {
        return "Capitalization";
    }

    @Override
    public String getCustomName() {
        String customName = super.getCustomName();
        return customName == null || customName.trim().isEmpty() ? "Capitalization" : customName;
    }

    @Override
    public String getBypassPermission() {
        return bypassPermission;
    }

    private String removeWhitelistEntries(String message) {
        String filteredMessage = message == null ? "" : message;
        if (whitelistPlayerNames) {
            filteredMessage = removeTokens(filteredMessage, onlinePlayerNamesSupplier.get());
        }
        return removeTokens(filteredMessage, Arrays.asList(whitelist));
    }

    private String removeTokens(String message, Iterable<String> tokens) {
        String filteredMessage = message;
        if (tokens == null) {
            return filteredMessage;
        }
        for (String token : tokens) {
            if (token != null && !token.isEmpty()) {
                filteredMessage = filteredMessage.replace(token, "");
            }
        }
        return filteredMessage;
    }

    private boolean startsWithLowercaseLetter(final String message) {
        for (int i = 0; i < message.length(); i++) {
            final char c = message.charAt(i);
            if (Character.isLetter(c)) {
                return Character.isLowerCase(c);
            }
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return false;
    }

    private String capitalizeFirstAlphabetic(final String message) {
        for (int i = 0; i < message.length(); i++) {
            final char c = message.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    return message;
                }
                final StringBuilder builder = new StringBuilder(message.length());
                builder.append(message, 0, i);
                builder.append(Character.toUpperCase(c));
                builder.append(message, i + 1, message.length());
                return builder.toString();
            }
            if (!Character.isWhitespace(c)) {
                return message;
            }
        }
        return message;
    }
}
