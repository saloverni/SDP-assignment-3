package org.example.notification;

import java.util.Objects;
import org.example.sender.MessageSender;

public abstract class Notification {
    protected MessageSender sender;

    protected Notification(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public void setSender(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    protected static void validate(String recipient, String message) {
        if (recipient == null || recipient.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Recipient and message must not be blank");
        }
    }

    protected void report(boolean success, MessageSender channel) {
        System.out.println((success ? "Successful: " : "Failed: ") + channel.getChannelName());
    }

    public abstract void send(String recipient, String message);
}
