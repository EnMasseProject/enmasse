package enmasse.storage.controller.model;

/**
 * The different address types for a broker.
 */
public enum AddressType {
    QUEUE("queue"),
    TOPIC("topic");

    private final String name;
    AddressType(String name) {
        this.name = name;
    }

    public static void validate(String type) {
        if (!QUEUE.name.equals(type) && !TOPIC.name.equals(type)) {
            throw new IllegalArgumentException("Unknown address type " + type);
        }
    }
}
