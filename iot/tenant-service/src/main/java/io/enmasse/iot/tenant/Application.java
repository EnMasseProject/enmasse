/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.hono.service.AbstractApplication;
import org.eclipse.hono.service.auth.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import io.enmasse.iot.tenant.impl.TenantServiceImpl;
import io.enmasse.iot.tenant.utils.HonoUtils;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

@SpringBootApplication
public class Application extends AbstractApplication {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private ApplicationContext context;
    private AuthenticationService authenticationService;

    @Autowired
    public void context(ApplicationContext context) {
        this.context = context;
    }

    @Autowired
    public final void setAuthenticationService(final AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(authenticationService);
    }

    @Override
    protected Future<Void> deployRequiredVerticles(int maxInstances) {

        final Vertx vertx = getVertx();

        final List<Future<?>> deploymentTracker = new ArrayList<>();

        deploymentTracker.add(deployAuthenticationService(vertx));
        deployTenantServices(maxInstances, vertx, deploymentTracker);

        return HonoUtils.toVoidResult(deploymentTracker);
    }

    private Future<?> deployAuthenticationService(Vertx vertx) {
        final Future<String> future = Future.future();
        vertx.deployVerticle((Verticle) this.authenticationService, future);
        return future;
    }

    private void deployTenantServices(int maxInstances, final Vertx vertx, final List<Future<?>> deploymentTracker) {
        for (int i = 0; i < maxInstances; i++) {
            final TenantServiceImpl serviceInstance = this.context.getBean(TenantServiceImpl.class);

            final Future<String> deployTracker = Future.future();
            vertx.deployVerticle(serviceInstance, deployTracker);
            deploymentTracker.add(deployTracker);

            registerHealthchecks(serviceInstance);
        }
    }

}
