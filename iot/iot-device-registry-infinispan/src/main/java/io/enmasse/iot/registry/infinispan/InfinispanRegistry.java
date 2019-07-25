/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.hono.service.AbstractBaseApplication;
import org.eclipse.hono.service.HealthCheckProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan("org.eclipse.hono.service.auth")
@ComponentScan("org.eclipse.hono.service.metric")
@ComponentScan("io.enmasse.iot.registry.infinispan")
@Configuration
@EnableAutoConfiguration
public class InfinispanRegistry extends AbstractBaseApplication {

        /**
         * All the verticles.
         */
        private List<Verticle> verticles;

        /**
         * All the health check providers.
         */
        private List<HealthCheckProvider> healthCheckProviders;


    @Autowired
    public void setVerticles(final List<Verticle> verticles) {
        this.verticles = verticles;
    }

    @Autowired
    public void setHealthCheckProviders(final List<HealthCheckProvider> healthCheckProviders) {
        this.healthCheckProviders = healthCheckProviders;
    }

    @Override
    protected final Future<?> deployVerticles() {

        return super.deployVerticles().compose(ok -> {

            @SuppressWarnings("rawtypes")
            final List<Future> futures = new LinkedList<>();

            for (final Verticle verticle : this.verticles) {
                log.info("Deploying: {}", verticle);
                final Future<String> result = Future.future();
                getVertx().deployVerticle(verticle, result);
            }

            return CompositeFuture.all(futures);

        });

    }

    /**
     * Registers any additional health checks that the service implementation components provide.
     *
     * @return A succeeded future.
     */
    @Override
    protected Future<Void> postRegisterServiceVerticles() {
        return super.postRegisterServiceVerticles().compose(ok -> {
            this.healthCheckProviders.forEach(this::registerHealthchecks);
            return Future.succeededFuture();
        });
    }

    /**
     * Starts the Device Registry Server.
     *
     * @param args command line arguments to pass to the server.
     */
    public static void main(final String[] args) {
        SpringApplication.run(InfinispanRegistry.class, args);
    }
}
