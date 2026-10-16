package org.example.sender;

public class EmailMessageSender extends SimulatedMessageSender {
    @Override
    public String getChannelName() { return "Email"; }

    @Override
    protected String format(String recipient, String title, String body) {
        return "[Email] To: " + recipient + "\nSubject: " + title + "\n" + body + "\n-- Notification Gateway";
    }
}
