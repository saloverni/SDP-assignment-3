package org.example.sender;

public class KafkaEventSender extends SimulatedMessageSender {
    @Override
    public String getChannelName() { return "Kafka"; }

    @Override
    protected String format(String recipient, String title, String body) {
        return "[Kafka] Published event: recipient=" + recipient + " | title=" + title + " | payload=" + body;
    }
}
