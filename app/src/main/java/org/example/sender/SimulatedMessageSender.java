package org.example.sender;

/* Common validation and availability control for simulated providers. */
public abstract class SimulatedMessageSender implements MessageSender {
    private boolean available = true;

    public void setAvailable(boolean available) { this.available = available; }


    @Override
    public boolean isAvailable() { return available; }

    @Override
    public final boolean sendMessage(String recipient, String title, String body) {
        if (!isAvailable() || recipient == null || recipient.isBlank()
                || title == null || body == null || body.isBlank() || !accepts(body)) {
            return false;
        }
        System.out.println(format(recipient, title, body));
        return true;
    }

    protected boolean accepts(String body) { return true; }
    protected abstract String format(String recipient, String title, String body);
}
