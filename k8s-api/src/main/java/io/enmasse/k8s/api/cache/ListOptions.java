/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

public class ListOptions {
    private String resourceVersion;
    private int timeoutSeconds;

    public ListOptions setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
        return this;
    }

    public ListOptions setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }
}
