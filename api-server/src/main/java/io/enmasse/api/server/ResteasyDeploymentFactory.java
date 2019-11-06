/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import io.enmasse.api.auth.AllowAllAuthInterceptor;
import io.enmasse.api.auth.ApiHeaderConfig;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.AuthInterceptor;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.v1.http.HttpApiRootService;
import io.enmasse.api.v1.http.HttpClusterUserService;
import io.enmasse.api.v1.http.HttpOpenApiService;
import io.enmasse.api.v1.http.HttpRootService;
import io.enmasse.api.v1.http.HttpUserService;
import io.enmasse.api.v1.http.SwaggerSpecEndpoint;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.user.api.UserApi;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestFilter;
import java.time.Clock;

public class ResteasyDeploymentFactory {
    private static final Logger log = LoggerFactory.getLogger(ResteasyDeploymentFactory.class);

    private final RequestLogger requestLogger;
    private final ContainerRequestFilter authInterceptor;
    private final SwaggerSpecEndpoint swaggerSpecEndpoint;
    private final HttpOpenApiService openApiService;
    private final HttpUserService userService;
    private final HttpClusterUserService clusterUserService;
    private final HttpRootService rootService;
    private final HttpApiRootService apiRootService;


    public ResteasyDeploymentFactory(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, AuthApi authApi, UserApi userApi, Clock clock, AuthenticationServiceRegistry authenticationServiceRegistry, ApiHeaderConfig apiHeaderConfig, Metrics metrics, boolean isRbacEnabled) {
        requestLogger = new RequestLogger(metrics);

        if (isRbacEnabled) {
            log.info("Enabling RBAC for REST API");
            authInterceptor = new AuthInterceptor(authApi, apiHeaderConfig, path -> path.equals("/swagger.json"));
        } else {
            log.info("Disabling authentication and authorization for REST API");
            authInterceptor = new AllowAllAuthInterceptor();
        }

        swaggerSpecEndpoint = new SwaggerSpecEndpoint();
        openApiService = new HttpOpenApiService();
        userService = new HttpUserService(addressSpaceApi, userApi, authenticationServiceRegistry, clock);
        clusterUserService = new HttpClusterUserService(userApi, authenticationServiceRegistry, clock);
        rootService = new HttpRootService();
        apiRootService = new HttpApiRootService();
    }

    public org.jboss.resteasy.spi.ResteasyDeployment getInstance() {
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();

        deployment.getProviderFactory().registerProvider(DefaultExceptionMapper.class);
        deployment.getProviderFactory().registerProviderInstance(requestLogger);
        deployment.getProviderFactory().registerProviderInstance(authInterceptor);
        deployment.getRegistry().addSingletonResource(swaggerSpecEndpoint);
        deployment.getRegistry().addSingletonResource(openApiService);
        deployment.getRegistry().addSingletonResource(userService);
        deployment.getRegistry().addSingletonResource(clusterUserService);
        deployment.getRegistry().addSingletonResource(rootService);
        deployment.getRegistry().addSingletonResource(apiRootService);
        return deployment;
    }
}
