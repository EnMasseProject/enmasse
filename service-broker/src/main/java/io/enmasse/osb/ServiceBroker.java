/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.osb;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.common.CachingSchemaProvider;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapSchemaApi;
import io.enmasse.k8s.api.SchemaApi;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceBroker extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServiceBroker.class.getName());
    private final NamespacedOpenShiftClient controllerClient;
    private final ServiceBrokerOptions options;

    private ServiceBroker(ServiceBrokerOptions options) {
        this.controllerClient = new DefaultOpenShiftClient();
        this.options = options;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        SchemaApi schemaApi = new ConfigMapSchemaApi(controllerClient, controllerClient.getNamespace());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider(schemaApi);
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);
        AuthApi authApi = new KubeAuthApi(controllerClient, options.getImpersonateUser(), controllerClient.getConfiguration().getOauthToken());

        vertx.deployVerticle(new HTTPServer(addressSpaceApi, schemaProvider, authApi, options.getCertDir(), options.getEnableRbac(), options.getKeycloakUrl(), options.getKeycloakAdminUser(), options.getKeycloakAdminPassword(), options.getListenPort()),
                result -> {
                    if (result.succeeded()) {
                        log.info("EnMasse Service Broker started");
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }

    public static void main(String args[]) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new ServiceBroker(ServiceBrokerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
