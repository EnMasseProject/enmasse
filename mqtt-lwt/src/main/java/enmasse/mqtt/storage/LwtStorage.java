/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.storage;

import enmasse.mqtt.messages.AmqpWillMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * LWT Storage Service interface
 */
public interface LwtStorage {

    /**
     * Open and connect to the storage service
     *
     * @param handler   handler called at the end of connection attempt
     */
    void open(Handler<AsyncResult<Void>> handler);

    /**
     * Store the provided "will" information
     *
     * @param clientId  client identifier for the "will" information
     * @param willMessage   "will" information to store
     * @param handler   handler called with the result code
     */
    void add(String clientId, AmqpWillMessage willMessage, Handler<AsyncResult<Integer>> handler);

    /**
     * Get "will" information for the specified client
     *
     * @param clientId  client identifier for which getting "will" information
     * @param handler   handler called with the "will" information retrieved
     */
    void get(String clientId, Handler<AsyncResult<AmqpWillMessage>> handler);

    /**
     * Update "will" information for the specified client
     *
     * @param clientId  client identifier for which updating "will" information
     * @param willMessage   "will" information to update
     * @param handler   handler called with the result code
     */
    void update(String clientId, AmqpWillMessage willMessage, Handler<AsyncResult<Integer>> handler);

    /**
     * Delete "will" information for the specified client
     *
     * @param clientId  client identifier for which deleting "will" information
     * @param handler   handler called with the result code
     */
    void delete(String clientId, Handler<AsyncResult<Integer>> handler);

    /**
     * Close and disconnect from the storage service
     */
    void close();
}
