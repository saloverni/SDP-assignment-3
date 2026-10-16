# Multi-Channel Notification Gateway

A Java application that uses the Bridge pattern to separate notification types from delivery providers. All providers are simulated and print messages to the console; no external services or credentials are required.

## Requirements and Usage

The project uses Java 21, Gradle Wrapper, and JUnit 5. Install JDK 21 or newer. Internet access is required on the first run to download Gradle and dependencies. Gradle can automatically provision the Java 21 toolchain.

```bash
./gradlew run
./gradlew test
./gradlew build
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

The entry point is `app/src/main/java/org/example/Main.java`. The test report is generated at `app/build/reports/tests/test/index.html`.

## Delivery Providers

| Provider | Behavior |
| --- | --- |
| `TelegramMessageSender` | Formats the title with `<b>` tags. |
| `EmailMessageSender` | Includes a subject and signature. |
| `SmsMessageSender` | Sends only the body, limited to 160 Unicode code points. Rejects longer messages without truncation. |
| `KafkaEventSender` | Simulates publishing an event containing the recipient, title, and payload. |
| `PushNotificationSender` | Simulates an alert sent to a device. |

`SimulatedMessageSender` provides shared input validation and availability control. Calling `setAvailable(false)` disables a provider. Unavailable providers and invalid delivery input return `false` without publishing a message.

The SMS limit applies to the final body, including notification prefixes and transformations. GSM segmentation is not simulated.

## Notification Types

| Notification | Behavior |
| --- | --- |
| `UrgentNotification` | Adds `[URGENT]`, checks provider availability, and tries fallback providers in order if the primary provider is unavailable or delivery fails. |
| `EncryptedNotification` | Encodes the UTF-8 message using Base64 before delivery. Base64 is reversible encoding, not secure encryption. |
| `ScheduledBatchNotification` | Queues recipients and messages, then sends them together through `sendBatch()`. |
| `MarketingNotification` | Adds a promotional prefix and an unsubscribe instruction. |

Urgent delivery stops at the first successful provider. If all providers fail, it prints a failure message. Fallback delivery does not replace the primary provider.

For batch notifications, `send(recipient, message)` adds a delivery to the queue. `sendBatch()` uses the currently selected provider, returns a `Summary`, prints totals, and clears the queue, including failed deliveries. The caller controls dispatch time; there is no background timer. Repeated recipients are treated as separate deliveries. An empty batch returns zero totals.

Notification methods reject blank recipients or messages with `IllegalArgumentException`. Passing a null provider throws `NullPointerException`.

## Demo

`Main` demonstrates:

1. Urgent notification through Telegram.
2. Base64-encoded notification through Email.
3. Batch delivery through SMS, including an oversized message that fails.
4. Switching the same notification object from Telegram to Email at runtime.
5. Unavailable Telegram with SMS fallback.
6. Marketing notification through Push.
7. Base64-encoded event through Kafka.

The SMS batch prints:

```text
Total: 3
Successful: 2
Failed: 1
```

## Bridge Architecture

The two independent dimensions are **notification behavior** and **delivery channel**.

The bridge is the `protected MessageSender sender` field in `Notification`. Each notification delegates delivery through the `MessageSender` interface. The constructor selects the initial provider, and `setSender()` replaces it at runtime without changing the notification object.

Combining both dimensions through inheritance would require classes such as `UrgentTelegramNotification` and `UrgentEmailNotification`, duplicating behavior across up to N × M combinations. Bridge separates these into N notification types and M providers.

A new provider implements `MessageSender` or extends `SimulatedMessageSender`. `PushNotificationSender` demonstrates this extension: existing notification classes require no changes.

A new notification type extends `Notification` and delegates delivery to `sender`. `MarketingNotification` demonstrates this extension: existing provider classes require no changes. Provider restrictions, such as the SMS length limit, still apply.

This is Bridge rather than Adapter because the design connects two independently extensible hierarchies through composition. It does not translate an existing incompatible interface into another interface.

Notification classes depend only on `MessageSender`, without concrete provider imports or `instanceof` checks. `Main` assembles the concrete combinations.

## UML Class Diagram

```mermaid
classDiagram
    class Notification {
        <<Abstraction>>
        #MessageSender sender
        +setSender(MessageSender sender) void
        +send(String recipient, String message) void
    }
    class UrgentNotification {
        <<Refined Abstraction>>
        -List~MessageSender~ fallbacks
        +send(String recipient, String message) void
    }
    class EncryptedNotification {
        <<Refined Abstraction>>
        +send(String recipient, String message) void
    }
    class ScheduledBatchNotification {
        <<Refined Abstraction>>
        -List~PendingMessage~ pending
        +send(String recipient, String message) void
        +sendBatch() Summary
    }
    class MarketingNotification {
        <<Refined Abstraction>>
        +send(String recipient, String message) void
    }
    class MessageSender {
        <<interface / Implementor>>
        +sendMessage(String recipient, String title, String body) boolean
        +isAvailable() boolean
        +getChannelName() String
    }
    class SimulatedMessageSender {
        <<abstract>>
        -boolean available
        +setAvailable(boolean available) void
        +sendMessage(String recipient, String title, String body) boolean
        #accepts(String body) boolean
        #format(String recipient, String title, String body) String
    }
    class TelegramMessageSender {
        <<Concrete Implementor>>
    }
    class EmailMessageSender {
        <<Concrete Implementor>>
    }
    class SmsMessageSender {
        <<Concrete Implementor>>
        +MAX_LENGTH$ int
    }
    class KafkaEventSender {
        <<Concrete Implementor>>
    }
    class PushNotificationSender {
        <<Concrete Implementor>>
    }
    Notification <|-- UrgentNotification
    Notification <|-- EncryptedNotification
    Notification <|-- ScheduledBatchNotification
    Notification <|-- MarketingNotification
    Notification o--> MessageSender : Bridge / has-a
    UrgentNotification o--> MessageSender : fallback providers
    MessageSender <|.. SimulatedMessageSender
    SimulatedMessageSender <|-- TelegramMessageSender
    SimulatedMessageSender <|-- EmailMessageSender
    SimulatedMessageSender <|-- SmsMessageSender
    SimulatedMessageSender <|-- KafkaEventSender
    SimulatedMessageSender <|-- PushNotificationSender
```

## Tests

`NotificationGatewayTest` contains 21 JUnit 5 tests covering:

- Successful delivery and unavailable providers.
- Urgent prefixes, fallback ordering, delivery failures, and exhausted fallbacks.
- SMS boundaries at 160 and 161 characters, including Unicode.
- Base64 transformation and UTF-8 round trips.
- Batch queuing, summary counts, partial failures, unavailable providers, and queue clearing.
- Runtime provider switching.
- Push with existing notifications and Marketing with every provider.
- Input validation and provider-specific formatting.
