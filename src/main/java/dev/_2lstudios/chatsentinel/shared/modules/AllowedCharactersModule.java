package dev._2lstudios.chatsentinel.shared.modules;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class AllowedCharactersModule extends ModerationModule {
	public static final String DEFAULT_MODE = "replace";
	public static final String DEFAULT_ALLOWED_REGEX = "[A-Za-z0-9 _.,!?@#:/;\\-()\\[\\]{}'\"]";
	public static final String DEFAULT_REPLACEMENT = "";

	private static final String BLOCK_MODE = "block";
	private static final Pattern DEFAULT_PATTERN = Pattern.compile(DEFAULT_ALLOWED_REGEX);

	private String mode = DEFAULT_MODE;
	private Pattern allowedPattern = DEFAULT_PATTERN;
	private String replacement = DEFAULT_REPLACEMENT;

	public AllowedCharactersModule() {
		setEnabled(false);
		setMaxWarns(-1);
		setWarnNotification("");
		setWebhookEnabled(false);
	}

	public void loadData(boolean enabled, String mode, String allowedRegex, String replacement) {
		setEnabled(enabled);
		setMaxWarns(-1);
		setWarnNotification("");
		setWebhookEnabled(false);
		this.mode = BLOCK_MODE.equalsIgnoreCase(mode) ? BLOCK_MODE : DEFAULT_MODE;
		this.allowedPattern = compileAllowedPattern(allowedRegex);
		this.replacement = replacement == null ? DEFAULT_REPLACEMENT : replacement;
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String originalMessage, String lang) {
		if (!isEnabled() || originalMessage == null || originalMessage.isEmpty()) {
			return null;
		}

		if (!containsDisallowedCharacter(originalMessage)) {
			return null;
		}

		if (BLOCK_MODE.equals(mode)) {
			return ChatEventResult.block(originalMessage).withPlayerMessage(messagesModule.getFiltered(lang));
		}

		String replacedMessage = replaceDisallowedCharacters(originalMessage);
		if (replacedMessage.isEmpty()) {
			return ChatEventResult.block(originalMessage).withPlayerMessage(messagesModule.getFiltered(lang));
		}
		return ChatEventResult.rewrite(replacedMessage);
	}

	@Override
	public String getName() {
		return "AllowedCharacters";
	}

	private boolean containsDisallowedCharacter(String message) {
		for (int i = 0; i < message.length(); i++) {
			if (!isAllowed(message.charAt(i))) {
				return true;
			}
		}

		return false;
	}

	private String replaceDisallowedCharacters(String message) {
		StringBuilder builder = new StringBuilder(message.length());
		for (int i = 0; i < message.length(); i++) {
			char character = message.charAt(i);
			if (isAllowed(character)) {
				builder.append(character);
			} else {
				builder.append(replacement);
			}
		}

		return builder.toString();
	}

	private boolean isAllowed(char character) {
		return allowedPattern.matcher(String.valueOf(character)).matches();
	}

	private Pattern compileAllowedPattern(String allowedRegex) {
		if (allowedRegex == null || allowedRegex.isEmpty()) {
			return DEFAULT_PATTERN;
		}

		try {
			return Pattern.compile(allowedRegex);
		} catch (PatternSyntaxException ignored) {
			return DEFAULT_PATTERN;
		}
	}
}
