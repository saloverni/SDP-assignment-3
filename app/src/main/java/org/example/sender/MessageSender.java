package org.example.sender;

/** Implementor: delivery channels are independent of notification policies. */
public interface MessageSender {
    boolean sendMessage(String recipient, String title, String body);
    boolean isAvailable();
    String getChannelName();
}
