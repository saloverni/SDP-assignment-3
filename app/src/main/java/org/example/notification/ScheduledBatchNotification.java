package org.example.notification;

import java.util.ArrayList;
import java.util.List;
import org.example.sender.MessageSender;

/** Queues deliveries; the caller decides when to dispatch the batch. */
public class ScheduledBatchNotification extends Notification {
    private record PendingMessage(String recipient, String message) { }
    public record Summary(int total, int successful, int failed) { }
    private final List<PendingMessage> pending = new ArrayList<>();

    public ScheduledBatchNotification(MessageSender sender) { super(sender); }

    /** Adds a recipient and message to the next scheduled batch. */
    @Override
    public void send(String recipient, String message) {
        validate(recipient, message);
        pending.add(new PendingMessage(recipient, message));
    }

    /** Sends the queue using the currently selected provider and clears it. */
    public Summary sendBatch() {
        int total = pending.size();
        int successful = 0;
        for (PendingMessage entry : pending) {
            if (sender.sendMessage(entry.recipient(), "Batch notification", entry.message())) {
                successful++;
            }
        }
        pending.clear();
        Summary summary = new Summary(total, successful, total - successful);
        System.out.printf("Total: %d%nSuccessful: %d%nFailed: %d%n",
                summary.total(), summary.successful(), summary.failed());
        return summary;
    }
}
