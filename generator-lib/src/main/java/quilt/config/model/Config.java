package quilt.config.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author lulf
 */
public final class Config {
    private final Collection<Broker> brokerList;

    public Config(Collection<Broker> brokerList) {
        this.brokerList = Collections.unmodifiableCollection(brokerList);
    }

    public Collection<Broker> brokers() {
        return brokerList;
    }
}
