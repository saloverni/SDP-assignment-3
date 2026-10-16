package org.example;

import org.example.notification.*;
import org.example.sender.*;

public class Main {
    public static void main(String[] args) {
        TelegramMessageSender telegram = new TelegramMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        SmsMessageSender sms = new SmsMessageSender();

        System.out.println("\n1. Urgent + Telegram");
        Notification urgent = new UrgentNotification(telegram, sms);
        urgent.send("alice", "Server is down");

        System.out.println("\n2. Encrypted + Email");
        new EncryptedNotification(email).send("alice@example.com", "Your code: 1234");

        System.out.println("\n3. Scheduled batch + SMS (one rejected long message)");
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms);
        batch.send("+77000000001", "Meeting at 10:00");
        batch.send("+77000000002", "Meeting at 10:00");
        batch.send("+77000000003", "x".repeat(161));
        batch.sendBatch();

        System.out.println("\n4. Same urgent object: Telegram -> Email");
        urgent.setSender(email);
        urgent.send("alice@example.com", "Server is restored");

        System.out.println("\n5. Telegram unavailable -> fallback SMS");
        telegram.setAvailable(false);
        urgent.setSender(telegram);
        urgent.send("+77000000001", "Please check the server");

        System.out.println("\n6. Marketing + Push (independent extensions)");
        new MarketingNotification(new PushNotificationSender()).send("device-42", "20% off today!");

        System.out.println("\n7. Encrypted + Kafka");
        new EncryptedNotification(new KafkaEventSender()).send("audit-topic", "Demo event");
    }
}
