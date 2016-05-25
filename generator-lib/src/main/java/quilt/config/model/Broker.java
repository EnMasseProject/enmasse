package quilt.config.model;

/**
 * @author lulf
 */
public final class Broker {
    private final String address;
    private final boolean storeAndForward;
    private final boolean multicast;

    public Broker(String address, boolean storeAndForward, boolean multicast) {
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

        Broker broker = (Broker) o;
        return address.equals(broker.address);

    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
