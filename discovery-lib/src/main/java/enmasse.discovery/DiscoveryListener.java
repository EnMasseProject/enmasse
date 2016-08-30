package enmasse.discovery;

import java.util.Set;

/**
 * @author Ulf Lilleengen
 */
public interface DiscoveryListener {
    void hostsChanged(Set<Host> hosts);
}
