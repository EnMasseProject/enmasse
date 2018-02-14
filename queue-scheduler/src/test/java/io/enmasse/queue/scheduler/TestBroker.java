/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TestBroker extends AbstractVerticle implements Broker {
    private final String id;
    private final String schedulerHost;
    private final int schedulerPort;
    private final Set<String> addressSet = new LinkedHashSet<>();
    private volatile ProtonConnection connection;
    private volatile String deploymentId;

    public TestBroker(String id, String schedulerHost, int schedulerPort) {
        this.id = id;
        this.schedulerHost = schedulerHost;
        this.schedulerPort = schedulerPort;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public void start(Future<Void> promise) throws Exception {
        ProtonClient client = ProtonClient.create(vertx);
        connectToScheduler(client, promise);
    }

    @Override
    public void stop(Future<Void> promise) {
        vertx.runOnContext(id -> {
            if (connection != null) {
                connection.close();
            }
            promise.complete();
        });
    }

    private void connectToScheduler(ProtonClient client, Future<Void> promise) {
        client.connect(schedulerHost, schedulerPort, openResult -> {
            if (openResult.succeeded()) {
                connection = openResult.result();
                connection.setContainer(id);
                connection.open();
                vertx.runOnContext(id -> promise.complete());
            } else {
                vertx.setTimer(2000, id -> connectToScheduler(client, promise));
            }
        });
    }

    @Override
    public synchronized void createQueue(String address) {
        addressSet.add(address);
    }

    @Override
    public synchronized void deleteQueue(String address) {
        addressSet.remove(address);
    }

    @Override
    public synchronized Set<String> getQueueNames() {
        return Collections.unmodifiableSet(addressSet);
    }

    public void undeploy(Vertx vertx) {
        vertx.undeploy(deploymentId);
    }
}
