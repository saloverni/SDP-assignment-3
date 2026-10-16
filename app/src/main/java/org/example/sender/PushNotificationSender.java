package org.example.sender;

public class PushNotificationSender extends SimulatedMessageSender {
    @Override
    public String getChannelName() { return "Push"; }

    @Override
    protected String format(String recipient, String title, String body) {
        return "[Push] Device: " + recipient + " | Alert: " + title + " | " + body;
    }
}
