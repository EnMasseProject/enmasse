/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.amqp;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestClient {

    private String serverHost;
    private int serverPort;
    private final Vertx vertx;
    private ProtonClient client;
    private ProtonConnection connection;
    private CountDownLatch closeLatch = new CountDownLatch(1);

    public TestClient(Vertx vertx, String serverHost, int serverPort) {
        this.vertx = vertx;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.client = ProtonClient.create(vertx);
    }

    public void subscribe(String address, Async subClosed, ProtonMessageHandler handler) {
        client.connect(new ProtonClientOptions().setConnectTimeout(10000), serverHost, serverPort, connectResult -> {
            if (connectResult.succeeded()) {
                System.out.println("Connected'");
                connection = connectResult.result();
                connection.closeHandler(c -> closeLatch.countDown());
                connection.open();
                System.out.println("Creating receiver");
                Source source = new Source();
                source.setAddress(address);
                Map<Symbol, Map<String, String>> filter = new LinkedHashMap<>();
                filter.put(Symbol.getSymbol("labels"), Collections.singletonMap("my", "label"));
                filter.put(Symbol.getSymbol("annotations"), Collections.singletonMap("my", "annotation"));
                source.setFilter(filter);
                connection.createReceiver(address).setSource(source).closeHandler(c -> {if (subClosed != null) { subClosed.complete(); }}).handler(handler).open();
            } else {
                System.out.println("Connection failed: " + connectResult.cause().getMessage());
            }
        });
    }

    public void close() throws InterruptedException {
        vertx.runOnContext(v -> {
            if (connection != null) {
                connection.close();
            }
        });
        closeLatch.await(1, TimeUnit.MINUTES);
    }
}
