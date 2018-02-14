/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.model;

/**
 * Represents a database of resources that can be subscribed to.
 */
public interface ResourceDatabase {
    void subscribe(ObserverKey observerKey, Subscriber subscriber) throws Exception;
}
