package dev._2lstudios.chatsentinel.shared.modules;

import java.util.regex.Pattern;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class FloodModerationModule extends ModerationModule {
	private boolean replace;
	private Pattern pattern;

	public void loadData(boolean enabled, String customName, boolean replace, int maxWarns, String pattern,
			String warnNotification, boolean webhookEnabled, String[] commands) {
		setEnabled(enabled);
		setMaxWarns(maxWarns);
		setWarnNotification(warnNotification);
		setWebhookEnabled(webhookEnabled);
		setCommands(commands);
		setCustomName(customName);
		this.replace = replace;
		this.pattern = Pattern.compile(pattern);
	}

	public boolean isReplace() {
		return this.replace;
	}

	public String replace(String string) {
		return pattern.matcher(string).replaceAll("");
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String message, String lang) {
		if (isEnabled() && pattern.matcher(message).find()) {
			if (isReplace()) {
				String replacedString = replace(message);

				if (!replacedString.isEmpty()) {
					return ChatEventResult.rewrite(replacedString);
				}
			}

			return ChatEventResult.block(message);
		}

		return null;
	}

	@Override
	public String getName() {
		return "Flood";
	}
}
