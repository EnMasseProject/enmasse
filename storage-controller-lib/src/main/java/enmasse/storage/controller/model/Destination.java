package enmasse.storage.controller.model;

/**
 * @author lulf
 */
public final class Destination {
    private final String address;
    private final boolean storeAndForward;
    private final boolean multicast;

    public Destination(String address, boolean storeAndForward, boolean multicast) {
        this.address = address;
        this.storeAndForward = storeAndForward;
        this.multicast = multicast;
    }

    public String address() {
        return address;
    }

    public boolean storeAndForward() {
        return storeAndForward;
    }

    public boolean multicast() {
        return multicast;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Destination destination = (Destination) o;
        return address.equals(destination.address);

    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
