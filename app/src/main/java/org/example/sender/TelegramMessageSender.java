package org.example.sender;

public class TelegramMessageSender extends SimulatedMessageSender {
    @Override
    public String getChannelName() { return "Telegram"; }

    @Override
    protected String format(String recipient, String title, String body) {
        return "[Telegram] To: " + recipient + " | <b>" + title + "</b>\n" + body;
    }
}
