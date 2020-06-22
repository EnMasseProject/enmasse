/*
 * Copyright 2018, 2019 EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.vertx.core.Vertx;
import org.eclipse.hono.service.HealthCheckProvider;
import org.eclipse.hono.service.HealthCheckServer;

import io.enmasse.iot.utils.MoreFutures;
import io.enmasse.model.CustomResourceDefinitions;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

@ApplicationScoped
@Startup(1)
public class Application {

    @Inject HealthCheckServer healthCheckServer;

    void onStart(@Observes final StartupEvent event) throws Exception {
        CustomResourceDefinitions.registerAll();
        MoreFutures.map(this.healthCheckServer.start()).get(15, TimeUnit.SECONDS);
    }

    public void init(@Observes StartupEvent e, Vertx vertx, Instance<AbstractVerticle> verticles) throws Exception {
        for (AbstractVerticle verticle : verticles) {
            final Promise<String> p = Promise.promise();
            vertx.deployVerticle(verticle, p);
            MoreFutures.map(p.future()).get();
        }
    }

    public void initHealth(@Observes StartupEvent e, Vertx vertx, Instance<HealthCheckProvider> providers) throws Exception {
        for (HealthCheckProvider provider : providers) {
            this.healthCheckServer.registerHealthCheckResources(provider);
        }
    }

    void onStop(@Observes final ShutdownEvent event)  throws Exception {
        MoreFutures.map(this.healthCheckServer.stop()).get(15, TimeUnit.SECONDS);
    }

}
