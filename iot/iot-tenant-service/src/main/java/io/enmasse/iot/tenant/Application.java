/*
 * Copyright 2018, 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.hono.service.AbstractBaseApplication;
import org.eclipse.hono.service.HealthCheckProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.enmasse.model.CustomResourceDefinitions;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Verticle;

@SpringBootApplication
public class Application extends AbstractBaseApplication {

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


    public static void main(final String[] args) {
        CustomResourceDefinitions.registerAll();
        SpringApplication.run(Application.class, args);
    }

}
