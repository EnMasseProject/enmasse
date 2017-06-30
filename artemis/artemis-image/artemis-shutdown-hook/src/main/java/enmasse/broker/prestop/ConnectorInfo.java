package enmasse.broker.prestop;

/**
 * Info about an AMQP connector
 */
public class ConnectorInfo {
    private final String name;

    public ConnectorInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectorInfo that = (ConnectorInfo) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
