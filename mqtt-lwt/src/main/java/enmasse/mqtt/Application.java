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
 * EnMasse MQTT Last Will and Testament service main application class
 */
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Vertx vertx = Vertx.vertx();
    private final MqttLwtOptions options;
    private final MqttLwt mqttLwt;

    private AtomicBoolean running = new AtomicBoolean();

    public Application(MqttLwtOptions options, MqttLwt mqttLwt) {
        this.options = options;
        this.mqttLwt = mqttLwt;
    }

    public void registerVerticles() {

        if (this.running.compareAndSet(false, true)) {


            try {
                CountDownLatch latch = new CountDownLatch(1);

                Future<Void> startFuture = Future.future();
                startFuture.setHandler(done -> {
                    if (done.succeeded()) {
                        latch.countDown();
                    } else {
                        LOG.error("Could not start MQTT LWT service", done.cause());
                    }
                });

                // start deploying more verticle instances
                this.deployVerticles(1, startFuture);

                // wait for deploying end
                long startupTimeout = this.options.getStartupTimeout().getSeconds();
                if (latch.await(startupTimeout, TimeUnit.SECONDS)) {
                    LOG.info("MQTT LWT service startup completed successfully");
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
     * @param instanceCount     number of verticle instances to deploy
     * @param resultHandler     handler called when the deploy ends
     */
    private void deployVerticles(int instanceCount, Future<Void> resultHandler) {

        LOG.debug("Starting up {} instances of MQTT LWT service verticle", instanceCount);

        Future<Void> result = Future.future();

        this.vertx.deployVerticle(this.mqttLwt, done -> {
            if (done.succeeded()) {
                LOG.debug("Verticle instance deployed [{}]", done.result());
                result.complete();
            } else {
                LOG.debug("Failed to deploy verticle instance {}", done.cause());
                result.fail(done.cause());
            }
        });

        // combine all futures related to verticle instances deploy
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
                        LOG.error("Could not shut down MQTT LWT service cleanly", done.cause());
                    }
                    latch.countDown();
                });

                if (latch.await(timeout, TimeUnit.SECONDS)) {
                    LOG.info("MQTT LWT service shut down completed");
                    shutdownHandler.handle(Boolean.TRUE);
                } else {
                    LOG.error("Shut down of MQTT LWT service timed out, aborting...");
                    shutdownHandler.handle(Boolean.FALSE);
                }
            }

        } catch (InterruptedException e) {
            LOG.error("Shut down of MQTT LWT service has been interrupted, aborting...");
            shutdownHandler.handle(Boolean.FALSE);
        }
    }

    public static void main(String[] args) {

        if (System.getProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME) == null) {
            System.setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        }

        Map<String, String> env = System.getenv();

        MqttLwtOptions options = MqttLwtOptions.fromEnv(env);

        LOG.info("MQTT LWT starting with options: {}", options);

        MqttLwt mqttLwt = new MqttLwt(options);

        Application app = new Application(options, mqttLwt);
        app.registerVerticles();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();
                LOG.info("MQTT LWT shutdown");
                app.shutdown();
            }
        });
    }
}
