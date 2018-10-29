/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;

/**
 * EnMasse MQTT gateway main application class
 */
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Vertx vertx = Vertx.vertx();
    private final MqttGatewayOptions options;
    private final MqttGateway mqttGateway;

    private AtomicBoolean running = new AtomicBoolean();

    public Application(MqttGatewayOptions options, MqttGateway mqttGateway) {
        this.options = options;
        this.mqttGateway = mqttGateway;
    }

    public void registerVerticles() {

        if (this.running.compareAndSet(false, true)) {

            long startupTimeout = this.options.getStartupTimeout().getSeconds();
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
                this.deployVerticles(startFuture);

                // wait for deploying end
                if (latch.await(startupTimeout, TimeUnit.SECONDS)) {
                    LOG.info("MQTT gateway startup completed successfully");
                } else {
                    LOG.error("Startup timed out after {} seconds, shutting down ...", startupTimeout);
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
     * @param resultHandler     handler called when the deploy ends
     */
    private void deployVerticles(Future<Void> resultHandler) {

        LOG.debug("Starting up MQTT gateway verticle");

        Future<Void> result = Future.future();

        this.vertx.deployVerticle(this.mqttGateway, done -> {
            if (done.succeeded()) {
                LOG.debug("Verticle instance deployed [{}]", done.result());
                result.complete();
            } else {
                LOG.debug("Failed to deploy verticle instance {}", done.cause());
                result.fail(done.cause());
            }
        });

        result.setHandler(done -> {
            if (done.succeeded()) {
                resultHandler.complete();
            } else {
                resultHandler.fail(done.cause());
            }
        });

    }

    public void shutdown() {
        if (this.running.compareAndSet(true, false)) {
            this.shutdown(this.options.getStartupTimeout().getSeconds(), result -> {
                // do nothing ?
            });
        }
    }

    /**
     * Execute Vert.x shutdown with related verticles
     *  @param timeout   max timeout to wait for shutdown
     * @param shutdownHandler   handler called when the shutdown ends
     */
    private void shutdown(long timeout, Handler<Boolean> shutdownHandler) {

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

        if (System.getProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME) == null) {
            System.setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        }

        Map<String, String> env = System.getenv();

        MqttGatewayOptions options = MqttGatewayOptions.fromEnv(env);

        LOG.info("MqttGateway starting with options: {}", options);

        MqttGateway gateway = new MqttGateway(options);

        Application app = new Application(options, gateway);
        app.registerVerticles();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();
                LOG.info("MqttGateway shutdown");
                app.shutdown();
            }
        });
    }
}
