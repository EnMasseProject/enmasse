/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EnMasse MQTT gateway main application class
 */
@SpringBootApplication // same as using @Configuration, @EnableAutoConfiguration and @ComponentScan
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Vertx vertx = Vertx.vertx();

    @Value(value = "${enmasse.mqtt.maxinstances:1}")
    private int maxInstances;
    @Value(value = "${enmasse.mqtt.startuptimeout:20}")
    private int startupTimeout;
    @Autowired
    private MqttGateway mqttGateway;

    private AtomicBoolean running = new AtomicBoolean();

    @PostConstruct
    public void registerVerticles() {

        if (this.running.compareAndSet(false, true)) {

            // instance count is upper bounded to the number of available processors
            int instanceCount =
                    (this.maxInstances > 0 && this.maxInstances < Runtime.getRuntime().availableProcessors()) ?
                            this.maxInstances :
                            Runtime.getRuntime().availableProcessors();


            try {

                CountDownLatch latch = new CountDownLatch(1);

                Future<Void> startFuture = Future.future();
                startFuture.setHandler(done -> {
                    if (done.succeeded()) {
                        latch.countDown();
                    } else {
                        LOG.error("Could not start MQTT gateway", done.cause());
                    }
                });

                // start deploying more verticle instances
                this.deployVerticles(instanceCount, startFuture);

                // wait for deploying end
                if (latch.await(this.startupTimeout, TimeUnit.SECONDS)) {
                    LOG.info("MQTT gateway startup completed successfully");
                } else {
                    LOG.error("Startup timed out after {} seconds, shutting down ...", this.startupTimeout);
                    this.shutdown();
                }

            } catch (InterruptedException e) {

                LOG.error("Startup process has been interrupted, shutting down ...");
                this.shutdown();
            }
        }
    }

    /**
     * Execute verticles deploy operation
     *
     * @param instanceCount     number of verticle instances to deploy
     * @param resultHandler     handler called when the deploy ends
     */
    private void deployVerticles(int instanceCount, Future<Void> resultHandler) {

        LOG.debug("Starting up {} instances of MQTT gateway verticle", instanceCount);

        List<Future> results = new ArrayList<>();

        for (int i = 1; i <= instanceCount; i++) {

            int instanceId = i;
            Future<Void> result = Future.future();
            results.add(result);

            this.vertx.deployVerticle(this.mqttGateway, done -> {
                if (done.succeeded()) {
                    LOG.debug("Verticle instance {} deployed [{}]", instanceId, done.result());
                    result.complete();
                } else {
                    LOG.debug("Failed to deploy verticle instance {}", instanceId, done.cause());
                    result.fail(done.cause());
                }
            });
        }

        // combine all futures related to verticle instances deploy
        CompositeFuture.all(results).setHandler(done -> {
            if (done.succeeded()) {
                resultHandler.complete();
            } else {
                resultHandler.fail(done.cause());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        if (this.running.compareAndSet(true, false)) {
            this.shutdown(this.startupTimeout, result -> {
                // do nothing ?
            });
        }
    }

    /**
     * Execute Vert.x shutdown with related verticles
     *
     * @param timeout   max timeout to wait for shutdown
     * @param shutdownHandler   handler called when the shutdown ends
     */
    private void shutdown(int timeout, Handler<Boolean> shutdownHandler) {

        try {

            CountDownLatch latch = new CountDownLatch(1);

            if (this.vertx != null) {

                this.vertx.close(done -> {
                    if (done.failed()) {
                        LOG.error("Could not shut down MQTT gateway cleanly", done.cause());
                    }
                    latch.countDown();
                });

                if (latch.await(timeout, TimeUnit.SECONDS)) {
                    LOG.info("MQTT gateway shut down completed");
                    shutdownHandler.handle(Boolean.TRUE);
                } else {
                    LOG.error("Shut down of MQTT gateway timed out, aborting...");
                    shutdownHandler.handle(Boolean.FALSE);
                }
            }

        } catch (InterruptedException e) {
            LOG.error("Shut down of MQTT gateway has been interrupted, aborting...");
            shutdownHandler.handle(Boolean.FALSE);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
