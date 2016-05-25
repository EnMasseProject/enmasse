package quilt.config.subscription.service.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a ConfigMap that can be subscribed to.
 *
 * @author lulf
 */
public class ConfigMap {
    private final String name;
    private final List<ConfigSubscriber> subscriberList = new ArrayList<>();

    private String version = null;
    private Map<String, String> values = Collections.emptyMap();

    public ConfigMap(String name) {
        this.name = name;
    }

    /**
     * Subscribe for updates to this configuration.
     *
     * @param subscriber The subscriber handle.
     */
    public synchronized void subscribe(ConfigSubscriber subscriber) {
        subscriberList.add(subscriber);
        // Notify only when we have values
        if (version != null) {
            subscriber.configUpdated(name, version, values);
        }
    }

    /**
     * Notify this instance that the config has been updated. It is the responsibility of this ConfigMap to notify subscribers.
     *
     * @param resourceVersion
     * @param data
     */
    public synchronized void configUpdated(String resourceVersion, Map<String, String> data) {
        version = resourceVersion;
        values = data;
        subscriberList.stream().forEach(subscription -> subscription.configUpdated(name, version, values));
    }
}
