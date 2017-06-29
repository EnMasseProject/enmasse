package enmasse.queue.scheduler;

import java.util.Map;
import java.util.Set;

/**
 * Interface for someone interested in addressing config.
 */
public interface ConfigListener {
    void addressesChanged(Map<String, Set<String>> addressMap);
}
