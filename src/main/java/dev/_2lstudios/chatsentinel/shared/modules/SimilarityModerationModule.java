package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SimilarityModerationModule extends ModerationModule {
    private String customName = "Similarity";
    private double thresholdPercentage = 75.0D;
    private int compareLastMessages = 3;
    private int minNormalizedLength = 4;
    private boolean stripSpecialCharacters = true;
    private boolean stripAccents = true;
    private boolean collapseRepeatedCharacters = true;

    public void loadData(final Object... args) {
        if (args.length < 8) {
            return;
        }
        setEnabled(convertToBoolean(args[0]));
        this.customName = convertToString(args[1], "Similarity");
        this.thresholdPercentage = clampDouble(convertToDouble(args[2], 75.0D), 1.0D, 100.0D);
        this.compareLastMessages = clampInt(convertToInt(args[3], 3), 1, 10);
        this.minNormalizedLength = clampInt(convertToInt(args[4], 4), 1, 64);
        this.stripSpecialCharacters = convertToBoolean(args[5]);
        this.stripAccents = convertToBoolean(args[6]);
        this.collapseRepeatedCharacters = convertToBoolean(args[7]);
    }

    @Override
    public ChatEventResult processEvent(final ChatPlayer chatPlayer, final MessagesModule messagesModule,
            final String playerName, final String message, final String lang) {
        if (!isEnabled() || message == null || message.startsWith("/")) {
            return null;
        }

        final String normalized = normalize(message);
        if (normalized.length() < minNormalizedLength) {
            return null;
        }

        double highest = 0.0D;
        String previousMatch = "";
        int compared = 0;
        final List<String> history = chatPlayer.getRecentMessagesSnapshot();
        for (final String previous : history) {
            if (compared >= compareLastMessages) {
                break;
            }
            final String normalizedPrevious = normalize(previous);
            if (normalizedPrevious.length() < minNormalizedLength) {
                continue;
            }
            compared++;
            final double similarity = similarityPercent(normalized, normalizedPrevious);
            if (similarity > highest) {
                highest = similarity;
                previousMatch = previous;
            }
        }

        if (highest < thresholdPercentage) {
            return null;
        }

        return ChatEventResult.block(message).withPlayerMessage(messagesModule.getSimilarityWarnMessage(new String[][] {
                { "%similarity%", "%threshold%", "%previous_message%" },
                { formatPercent(highest), formatPercent(thresholdPercentage), previousMatch }
        }, lang));
    }

    @Override
    public String getName() {
        return "Similarity";
    }

    @Override
    public String getCustomName() {
        return customName;
    }

    @Override
    public String getWarnNotification(final String[][] placeholders) {
        return null;
    }

    String normalize(final String input) {
        if (input == null) {
            return "";
        }
        String value = input;
        value = value.replaceAll("(?i)[&§][0-9A-FK-ORX]", "");
        value = value.toLowerCase(Locale.ROOT);
        if (stripAccents) {
            value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        }
        if (stripSpecialCharacters) {
            value = value.replaceAll("[^\\p{L}\\p{N}]+", " ");
        }
        value = value.trim().replaceAll("\\s+", " ");
        if (collapseRepeatedCharacters) {
            value = collapseRepeatedCharacters(value);
        }
        return value;
    }

    private String collapseRepeatedCharacters(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        final StringBuilder result = new StringBuilder();
        char lastChar = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (i == 0 || c != lastChar) {
                result.append(c);
                lastChar = c;
            }
        }
        return result.toString();
    }

    private double similarityPercent(final String first, final String second) {
        final int maxLength = Math.max(first.length(), second.length());
        if (maxLength == 0) {
            return 100.0D;
        }
        final int distance = levenshteinDistance(first, second);
        return 100.0D * (1.0D - ((double) distance / (double) maxLength));
    }

    private int levenshteinDistance(String first, String second) {
        if (first == null) {
            first = "";
        }
        if (second == null) {
            second = "";
        }
        final int n = first.length();
        final int m = second.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                final int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost);
            }
            final int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }

    private String formatPercent(final double value) {
        return String.format(Locale.ROOT, "%.0f%%", value);
    }

    private double clampDouble(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean convertToBoolean(final Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private String convertToString(final Object value, final String defaultValue) {
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    private double convertToDouble(final Object value, final double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private int convertToInt(final Object value, final int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public double getThresholdPercentage() {
        return thresholdPercentage;
    }

    public int getCompareLastMessages() {
        return compareLastMessages;
    }

    public int getMinNormalizedLength() {
        return minNormalizedLength;
    }

    public boolean isStripSpecialCharacters() {
        return stripSpecialCharacters;
    }

    public boolean isStripAccents() {
        return stripAccents;
    }

    public boolean isCollapseRepeatedCharacters() {
        return collapseRepeatedCharacters;
    }
}