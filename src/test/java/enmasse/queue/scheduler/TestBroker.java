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

package enmasse.queue.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;


public class TestBroker extends AbstractVerticle implements Broker {
    private final String id;
    private final String schedulerHost;
    private final int schedulerPort;
    private final Set<String> addressSet = new LinkedHashSet<>();
    private volatile ProtonConnection connection;

    public TestBroker(String id, String schedulerHost, int schedulerPort) {
        this.id = id;
        this.schedulerHost = schedulerHost;
        this.schedulerPort = schedulerPort;
    }

    @Override
    public void start() throws Exception {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(schedulerHost, schedulerPort, openResult -> {
            assertTrue(openResult.succeeded());
            connection = openResult.result();
            connection.setContainer(id);
            connection.open();
        });
    }

    @Override
    public synchronized void deployQueue(String address) {
        addressSet.add(address);
    }

    @Override
    public synchronized void deleteQueue(String address) {
        addressSet.remove(address);

    }

    @Override
    public synchronized long numQueues() {
        return addressSet.size();
    }

    public synchronized Set<String> getAddressSet() {
        return Collections.unmodifiableSet(addressSet);
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
