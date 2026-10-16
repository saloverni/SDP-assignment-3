package org.example.notification;

import java.util.List;
import org.example.sender.MessageSender;

public class UrgentNotification extends Notification {
    private final List<MessageSender> fallbacks;

    public UrgentNotification(MessageSender sender, MessageSender... fallbacks) {
        super(sender);
        this.fallbacks = List.of(fallbacks);
    }

    @Override
    public void send(String recipient, String message) {
        validate(recipient, message);
        String body = "[URGENT] " + message;
        if (trySend(sender, recipient, body)) { return; }
        for (MessageSender fallback : fallbacks) {
            System.out.println("Trying fallback: " + fallback.getChannelName());
            if (trySend(fallback, recipient, body)) { return; }
        }
        System.out.println("Failed: all urgent delivery channels exhausted");
    }

    private boolean trySend(MessageSender channel, String recipient, String body) {
        boolean success = channel.isAvailable() && channel.sendMessage(recipient, "Urgent notification", body);
        report(success, channel);
        return success;
    }
}
