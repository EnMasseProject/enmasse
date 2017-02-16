/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.mqtt.storage.impl;

import enmasse.mqtt.storage.LwtStorage;
import enmasse.mqtt.storage.WillMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * In memory implementation of the LWT Storage service
 */
public class InMemoryLwtStorage implements LwtStorage {

    @Override
    public void open(Handler<AsyncResult<Void>> handler) {

        handler.handle(Future.succeededFuture());
    }

    @Override
    public void add(String clientId, WillMessage willMessage, Handler<AsyncResult<Integer>> handler) {

        // TODO
    }

    @Override
    public void get(String clientId, Handler<AsyncResult<WillMessage>> handler) {

        // TODO
    }

    @Override
    public void update(String clientId, WillMessage willMessage, Handler<AsyncResult<Integer>> handler) {

        // TODO
    }

    @Override
    public void delete(String clientId, Handler<AsyncResult<Integer>> handler) {

        // TODO
    }

    @Override
    public void close() {

        // TODO
    }
}
