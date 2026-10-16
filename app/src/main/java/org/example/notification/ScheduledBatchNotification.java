package org.example.notification;

import java.util.ArrayList;
import java.util.List;
import org.example.sender.MessageSender;

public class ScheduledBatchNotification extends Notification {
    private record PendingMessage(String recipient, String message) { }
    public record Summary(int total, int successful, int failed) { }
    private final List<PendingMessage> pending = new ArrayList<>();

    public ScheduledBatchNotification(MessageSender sender) { super(sender); }

    @Override
    public void send(String recipient, String message) {
        validate(recipient, message);
        pending.add(new PendingMessage(recipient, message));
    }

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
