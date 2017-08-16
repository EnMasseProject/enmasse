package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;

import java.util.Map;
import java.util.Set;

/**
 * Interface for someone interested in addressing config.
 */
public interface ConfigListener {
    void addressesChanged(Map<String, Set<Address>> addressMap);
}
