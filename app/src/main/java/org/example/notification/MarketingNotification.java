package org.example.notification;

import org.example.sender.MessageSender;

public class MarketingNotification extends Notification {
    public MarketingNotification(MessageSender sender) { super(sender); }

    @Override
    public void send(String recipient, String message) {
        validate(recipient, message);
        report(sender.sendMessage(recipient, "Special offer", "[PROMO] " + message
                + "\nReply STOP to unsubscribe."), sender);
    }
}
