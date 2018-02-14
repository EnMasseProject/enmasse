/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.discovery;

import java.util.Set;

/**
 * Interface for listeners on host set changes.
 */
public interface DiscoveryListener {
    void hostsChanged(Set<Host> hosts);
}
