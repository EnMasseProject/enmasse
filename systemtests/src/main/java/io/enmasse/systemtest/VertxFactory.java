package io.enmasse.systemtest;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * Creates minimal vertx instances
 */
public class VertxFactory {
    public static Vertx create() {
        VertxOptions options = new VertxOptions()
                .setWorkerPoolSize(1)
                .setInternalBlockingPoolSize(1)
                .setEventLoopPoolSize(1);
        return Vertx.vertx(options);
    }
}
