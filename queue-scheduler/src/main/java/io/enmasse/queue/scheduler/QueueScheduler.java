/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;
import org.apache.qpid.proton.amqp.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Acts as an arbiter deciding in which broker a queue should run.
 */
public class QueueScheduler extends AbstractVerticle implements ConfigListener {
    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class.getName());
    private static final Symbol groupSymbol = Symbol.getSymbol("qd.route-container-group");

    private final SchedulerState schedulerState;
    private final BrokerFactory brokerFactory;
    private ProtonSaslAuthenticatorFactory saslAuthenticatorFactory;
    private volatile ProtonServer server;

    private final int port;
    private final String certDir;

    public QueueScheduler(BrokerFactory brokerFactory, SchedulerState schedulerState, int listenPort, String certDir) {
        this.brokerFactory = brokerFactory;
        this.schedulerState = schedulerState;
        this.port = listenPort;
        this.certDir = certDir;
    }

    // This is a temporary hack until Artemis can support sasl anonymous
    public void setProtonSaslAuthenticatorFactory(ProtonSaslAuthenticatorFactory saslAuthenticatorFactory) {
        this.saslAuthenticatorFactory = saslAuthenticatorFactory;
    }

    private static String getGroupId(ProtonConnection connection) {
        Map<Symbol, Object> connectionProperties = connection.getRemoteProperties();
        if (connectionProperties.containsKey(groupSymbol)) {
            return (String) connectionProperties.get(groupSymbol);
        } else {
            return connection.getRemoteContainer();
        }
    }

    @Override
    public void start() {
        ProtonServerOptions options = new ProtonServerOptions();
        if (certDir != null) {
            options.setSsl(true)
                .setClientAuth(ClientAuth.REQUIRED)
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .setKeyPath(new File(certDir, "tls.key").getAbsolutePath())
                        .setCertPath(new File(certDir, "tls.crt").getAbsolutePath()))
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()));
        }

        server = ProtonServer.create(vertx, options);
        server.saslAuthenticatorFactory(saslAuthenticatorFactory);
        server.connectHandler(connection -> {
            connection.setContainer("queue-scheduler");
            connection.openHandler(result -> {
                connection.open();
                connectionOpened(connection);
            }).closeHandler(conn -> {
                log.info("Broker connection " + connection.getRemoteContainer() + " closed");
                executeBlocking(() -> schedulerState.brokerRemoved(getGroupId(connection), connection.getRemoteContainer()),
                        "Error removing broker");
                connection.close();
                connection.disconnect();
            }).disconnectHandler(protonConnection -> {
                log.info("Broker connection " + connection.getRemoteContainer() + " disconnected");
                executeBlocking(() -> schedulerState.brokerRemoved(getGroupId(connection), connection.getRemoteContainer()),
                        "Error removing broker");
                connection.disconnect();
            });
        });

        server.listen(port, event -> {
            if (event.succeeded()) {
                log.info("QueueScheduler is up and running");
            } else {
                log.error("Error starting queue scheduler", event.cause());
            }
        });
    }

    private void connectionOpened(ProtonConnection connection) {
        log.info("Connection opened from " + connection.getRemoteContainer());
        Future<Broker> broker = brokerFactory.createBroker(connection);
        broker.setHandler(result -> {
            if (result.succeeded()) {
                executeBlocking(() -> schedulerState.brokerAdded(getGroupId(connection), connection.getRemoteContainer(), result.result()), "Error adding broker");
            } else {
                log.info("Error getting broker instance", result.cause());
            }
        });
    }

    @Override
    public void stop() {
        log.info("Stopping server!");
        if (server != null) {
            server.close();
        }
    }

    @Override
    public void addressesChanged(Map<String, Set<Address>> addressMap) {
        executeBlocking(() -> schedulerState.addressesChanged(addressMap), "Error handling address change");
    }

    private void executeBlocking(Task task, String errorMessage) {
        vertx.executeBlocking(promise -> {
            try {
                task.run();
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, true, result -> {
            if (result.failed()) {
                log.error(errorMessage, result.cause());
            }
        });
    }

    private interface Task {
        void run() throws Exception;
    }

    public int getPort() {
        if (server == null) {
            return 0;
        } else {
            return server.actualPort();
        }
    }
}
