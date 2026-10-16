package org.example.sender;

public class SmsMessageSender extends SimulatedMessageSender {
    public static final int MAX_LENGTH = 160;

    @Override
    protected boolean accepts(String body) {
        return body.codePointCount(0, body.length()) <= MAX_LENGTH;
    }

    @Override
    public String getChannelName() { return "SMS"; }

    @Override
    protected String format(String recipient, String title, String body) {
        return "[SMS] To: " + recipient + " | " + body;
    }
}
