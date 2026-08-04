package dev._2lstudios.chatsentinel.shared.modules;

import dev._2lstudios.chatsentinel.shared.chat.ChatEventResult;
import dev._2lstudios.chatsentinel.shared.chat.ChatPlayer;

public class ServerMuteModule extends ModerationModule {
	private boolean muted;
	private String bypassPermission = "chatsentinel.mute.bypass";

	public void loadData(boolean enabled, boolean muted, String bypassPermission) {
		setEnabled(enabled);
		this.bypassPermission = bypassPermission == null || bypassPermission.trim().isEmpty()
				? "chatsentinel.mute.bypass"
				: bypassPermission;
		this.muted = muted;
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	@Override
	public ChatEventResult processEvent(ChatPlayer chatPlayer, MessagesModule messagesModule, String playerName,
			String originalMessage, String lang) {
		if (isEnabled() && muted) {
			return ChatEventResult.block(messagesModule.getServerMuted(lang));
		}

		return null;
	}

	@Override
	public String getName() {
		return "ServerMute";
	}

	@Override
	public String getCustomName() {
		return "Server Mute";
	}

	@Override
	public String getBypassPermission() {
		return bypassPermission;
	}
}
