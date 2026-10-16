package org.example.notification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.example.sender.MessageSender;

public class EncryptedNotification extends Notification {
    public EncryptedNotification(MessageSender sender) { super(sender); }

    @Override
    public void send(String recipient, String message) {
        validate(recipient, message);
        String encoded = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        report(sender.sendMessage(recipient, "Encoded notification", encoded), sender);
    }
}
