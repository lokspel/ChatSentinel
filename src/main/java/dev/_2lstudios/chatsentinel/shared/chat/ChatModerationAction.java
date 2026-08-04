package dev._2lstudios.chatsentinel.shared.chat;

public enum ChatModerationAction {
    PASS(false, false, false, false),
    REWRITE(false, false, true, false),
    BLOCK(true, true, false, false),
    SELF_ONLY(true, true, false, true);

    private final boolean terminal;
    private final boolean cancelsOriginal;
    private final boolean rewritesMessage;
    private final boolean echoesToSender;

    private ChatModerationAction(boolean terminal, boolean cancelsOriginal,
            boolean rewritesMessage, boolean echoesToSender) {
        this.terminal = terminal;
        this.cancelsOriginal = cancelsOriginal;
        this.rewritesMessage = rewritesMessage;
        this.echoesToSender = echoesToSender;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean cancelsOriginal() {
        return cancelsOriginal;
    }

    public boolean rewritesMessage() {
        return rewritesMessage;
    }

    public boolean echoesToSender() {
        return echoesToSender;
    }
}
