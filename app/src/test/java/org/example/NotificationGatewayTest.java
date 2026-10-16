package org.example;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.example.notification.*;
import org.example.sender.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationGatewayTest {
    private static class RecordingSender implements MessageSender {
        boolean available = true;
        boolean succeeds = true;
        int calls;
        String recipient;
        String title;
        String body;
        public boolean sendMessage(String recipient, String title, String body) {
            calls++;
            this.recipient = recipient;
            this.title = title;
            this.body = body;
            return succeeds;
        }
        public boolean isAvailable() { return available; }
        public String getChannelName() { return "Recording"; }
    }

    private String outputOf(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test void everyProviderDeliversValidMessages() {
        for (MessageSender sender : List.of(new TelegramMessageSender(), new EmailMessageSender(),
                new SmsMessageSender(), new KafkaEventSender(), new PushNotificationSender())) {
            assertTrue(sender.sendMessage("user", "Title", "Hello"), sender.getChannelName());
        }
    }

    @Test void unavailableProvidersRejectWithoutPublishing() {
        for (SimulatedMessageSender sender : List.of(new TelegramMessageSender(), new EmailMessageSender(),
                new SmsMessageSender(), new KafkaEventSender(), new PushNotificationSender())) {
            sender.setAvailable(false);
            assertFalse(sender.isAvailable());
            assertEquals("", outputOf(() -> assertFalse(sender.sendMessage("user", "Title", "Hello"))));
        }
    }

    @Test void urgentAddsPrefixAndPreservesRecipient() {
        RecordingSender sender = new RecordingSender();
        new UrgentNotification(sender).send("alice", "Alert");
        assertEquals("[URGENT] Alert", sender.body);
        assertEquals("alice", sender.recipient);
        assertEquals(1, sender.calls);
    }

    @Test void unavailablePrimaryIsSkippedAndFallbackDelivers() {
        RecordingSender primary = new RecordingSender();
        primary.available = false;
        RecordingSender fallback = new RecordingSender();
        new UrgentNotification(primary, fallback).send("alice", "Alert");
        assertEquals(0, primary.calls);
        assertEquals(1, fallback.calls);
        assertEquals("[URGENT] Alert", fallback.body);
    }

    @Test void successfulPrimaryDoesNotUseFallback() {
        RecordingSender fallback = new RecordingSender();
        new UrgentNotification(new RecordingSender(), fallback).send("alice", "Alert");
        assertEquals(0, fallback.calls);
    }

    @Test void failedDeliveryAlsoUsesFallback() {
        RecordingSender primary = new RecordingSender();
        primary.succeeds = false;
        RecordingSender fallback = new RecordingSender();
        new UrgentNotification(primary, fallback).send("alice", "Alert");
        assertEquals(1, primary.calls);
        assertEquals(1, fallback.calls);
    }

    @Test void multipleFallbacksAreTriedInOrder() {
        RecordingSender primary = new RecordingSender();
        primary.available = false;
        RecordingSender first = new RecordingSender();
        first.succeeds = false;
        RecordingSender second = new RecordingSender();
        new UrgentNotification(primary, first, second).send("alice", "Alert");
        assertEquals(1, first.calls);
        assertEquals(1, second.calls);
    }

    @Test void exhaustedFallbacksReportFailure() {
        TelegramMessageSender primary = new TelegramMessageSender();
        primary.setAvailable(false);
        SmsMessageSender fallback = new SmsMessageSender();
        fallback.setAvailable(false);
        String output = outputOf(() -> new UrgentNotification(primary, fallback).send("alice", "Alert"));
        assertTrue(output.contains("all urgent delivery channels exhausted"));
    }

    @Test void smsAcceptsExactly160AndRejects161Characters() {
        SmsMessageSender sms = new SmsMessageSender();
        assertTrue(sms.sendMessage("user", "Ignored title", "x".repeat(160)));
        assertFalse(sms.sendMessage("user", "Ignored title", "x".repeat(161)));
    }

    @Test void smsCountsUnicodeCodePoints() {
        SmsMessageSender sms = new SmsMessageSender();
        assertTrue(sms.sendMessage("user", "", "😀".repeat(160)));
        assertFalse(sms.sendMessage("user", "", "😀".repeat(161)));
    }

    @Test void encryptedNotificationTransformsUtf8MessageBeforeDelivery() {
        RecordingSender sender = new RecordingSender();
        String message = "Секрет 🔑";
        new EncryptedNotification(sender).send("alice", message);
        assertNotEquals(message, sender.body);
        assertEquals(message, new String(Base64.getDecoder().decode(sender.body), StandardCharsets.UTF_8));
    }

    @Test void batchQueuesUntilDispatch() {
        RecordingSender sender = new RecordingSender();
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sender);
        batch.send("alice", "First");
        batch.send("bob", "Second");
        assertEquals(0, sender.calls);
        assertEquals(new ScheduledBatchNotification.Summary(2, 2, 0), batch.sendBatch());
        assertEquals(2, sender.calls);
        assertEquals("bob", sender.recipient);
        assertEquals("Second", sender.body);
    }

    @Test void batchCountsPartialFailureAndClearsQueue() {
        ScheduledBatchNotification batch = new ScheduledBatchNotification(new SmsMessageSender());
        batch.send("alice", "Short");
        batch.send("bob", "x".repeat(161));
        assertEquals(new ScheduledBatchNotification.Summary(2, 1, 1), batch.sendBatch());
        assertEquals(new ScheduledBatchNotification.Summary(0, 0, 0), batch.sendBatch());
    }

    @Test void batchCountsUnavailableProviderAsFailures() {
        SmsMessageSender sms = new SmsMessageSender();
        sms.setAvailable(false);
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms);
        batch.send("alice", "Hello");
        assertEquals(new ScheduledBatchNotification.Summary(1, 0, 1), batch.sendBatch());
    }

    @Test void sameNotificationSwitchesSenderAtRuntime() {
        RecordingSender first = new RecordingSender();
        RecordingSender second = new RecordingSender();
        Notification notification = new UrgentNotification(first);
        notification.send("alice", "First");
        notification.setSender(second);
        notification.send("bob", "Second");
        assertEquals(1, first.calls);
        assertEquals("[URGENT] First", first.body);
        assertEquals(1, second.calls);
        assertEquals("[URGENT] Second", second.body);
    }

    @Test void pushWorksWithExistingNotification() {
        String output = outputOf(() -> new UrgentNotification(new PushNotificationSender()).send("device", "Alert"));
        assertTrue(output.contains("[Push] Device: device"));
        assertTrue(output.contains("[URGENT] Alert"));
    }

    @Test void marketingAddsPromotionAndUnsubscribeInstruction() {
        RecordingSender sender = new RecordingSender();
        new MarketingNotification(sender).send("alice", "Discount");
        assertEquals("Special offer", sender.title);
        assertEquals("[PROMO] Discount\nReply STOP to unsubscribe.", sender.body);
    }

    @Test void marketingWorksWithEveryProvider() {
        for (MessageSender sender : List.of(new TelegramMessageSender(), new EmailMessageSender(),
                new SmsMessageSender(), new KafkaEventSender(), new PushNotificationSender())) {
            String output = outputOf(() -> new MarketingNotification(sender).send("user", "Sale"));
            assertTrue(output.contains("Successful: " + sender.getChannelName()));
        }
    }

    @Test void invalidProviderInputIsRejected() {
        MessageSender sender = new EmailMessageSender();
        assertFalse(sender.sendMessage(" ", "Title", "Body"));
        assertFalse(sender.sendMessage(null, "Title", "Body"));
        assertFalse(sender.sendMessage("alice", null, "Body"));
        assertFalse(sender.sendMessage("alice", "Title", null));
        assertFalse(sender.sendMessage("alice", "Title", " "));
    }

    @Test void notificationRejectsInvalidInputAndNullSender() {
        assertThrows(NullPointerException.class, () -> new UrgentNotification(null));
        Notification notification = new UrgentNotification(new RecordingSender());
        assertThrows(NullPointerException.class, () -> notification.setSender(null));
        assertThrows(IllegalArgumentException.class, () -> notification.send("", "Hello"));
        assertThrows(IllegalArgumentException.class, () -> notification.send("alice", ""));
    }

    @Test void providersHaveDistinctFormatting() {
        assertTrue(outputOf(() -> new TelegramMessageSender().sendMessage("u", "T", "B")).contains("<b>T</b>"));
        String email = outputOf(() -> new EmailMessageSender().sendMessage("u", "T", "B"));
        assertTrue(email.contains("Subject: T"));
        assertTrue(email.contains("-- Notification Gateway"));
        assertTrue(outputOf(() -> new KafkaEventSender().sendMessage("topic", "T", "B")).contains("Published event:"));
        assertFalse(outputOf(() -> new SmsMessageSender().sendMessage("u", "Unique title", "B")).contains("Unique title"));
    }
}
