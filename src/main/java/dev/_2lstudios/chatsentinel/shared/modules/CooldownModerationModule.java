package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

import java.util.Locale;

public class CooldownModerationModule extends ModerationModule {
	private int repeatTimeGlobal;
	private int repeatTime;
	private int normalTime;
	private int commandTime;

	private long lastMessageTime = 0L;
	private String lastMessage = "";

	public void loadData(boolean enabled, int repeatTimeGlobal, int repeatTime,
			int normalTime,
			int commandTime) {
		setEnabled(enabled);
		this.repeatTimeGlobal = repeatTimeGlobal;
		this.repeatTime = repeatTime;
		this.normalTime = normalTime;
		this.commandTime = commandTime;
	}

	public long getRemainingMillis(ChatPlayer chatPlayer, String message) {
		if (!isEnabled() || message == null) {
			return 0L;
		}

		final long currentTime = System.currentTimeMillis();
		final boolean isCommand = message.startsWith("/");
		final long lastMessageTimePassed = currentTime - (isCommand ? chatPlayer.getLastCommandTime() : chatPlayer.getLastMessageTime());
		final long lastMessageTimePassedGlobal = currentTime - this.lastMessageTime;
		final long remainingTime;

		if (isCommand) {
			remainingTime = this.commandTime - lastMessageTimePassed;
		} else if (chatPlayer.isLastMessage(message) && lastMessageTimePassed < this.repeatTime) {
			remainingTime = this.repeatTime - lastMessageTimePassed;
		} else if (this.lastMessage.equals(message) && lastMessageTimePassedGlobal < this.repeatTimeGlobal) {
			remainingTime = this.repeatTimeGlobal - lastMessageTimePassedGlobal;
		} else {
			remainingTime = this.normalTime - lastMessageTimePassed;
		}

		return Math.max(0L, remainingTime);
	}

	public float getRemainingTime(ChatPlayer chatPlayer, String message) {
		final long remainingMillis = getRemainingMillis(chatPlayer, message);
		if (remainingMillis <= 0L) {
			return 0.0F;
		}
		return ((float) Math.ceil(remainingMillis / 100.0D)) / 10.0F;
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String originalMessage, String lang) {
		final float remainingTime = getRemainingTime(chatPlayer, originalMessage);
		if (isEnabled() && remainingTime > 0.0F) {
			return ChatEventResult.block(originalMessage).withNotify(false).withPlayerMessage(messagesModule.getCooldownWarnMessage(new String[][] {
					{ "%cooldown%" },
					{ formatSeconds(remainingTime) }
			}, lang));
		}

		return null;
	}

	private String formatSeconds(final float seconds) {
		return String.format(Locale.ROOT, "%.1f", seconds);
	}

	@Override
	public String getName() {
		return "Cooldown";
	}

	@Override
	public String getCustomName() {
		return getName();
	}

	@Override
	public String getWarnNotification(String[][] placeholders) {
		return null;
	}

	public void setLastMessage(String lastMessage, long lastMessageTime) {
		this.lastMessage = lastMessage;
		this.lastMessageTime = lastMessageTime;
	}
}
