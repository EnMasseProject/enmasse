/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.k8s.api.ConfigMapSchemaApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaApi;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();

        TemplateOptions templateOptions = new TemplateOptions(env);
        File templateDir = getEnv(env, "TEMPLATE_DIR")
                .map(File::new)
                .orElse(null);

        String certDir = getEnvOrThrow(env, "CERT_DIR");
        String addressSpace = getEnvOrThrow(env, "ADDRESS_SPACE");
        OpenShiftClient openShiftClient = new DefaultOpenShiftClient();
        SchemaApi schemaApi = new ConfigMapSchemaApi(openShiftClient, openShiftClient.getNamespace());

        Kubernetes kubernetes = new KubernetesHelper(new DefaultOpenShiftClient(), templateDir);
        BrokerSetGenerator clusterGenerator = new TemplateBrokerSetGenerator(kubernetes, templateOptions, addressSpace);

        EventLogger eventLogger = kubernetes.createEventLogger(Clock.systemUTC(), "standard-controller");


        AddressController addressController = new AddressController(
                addressSpace,
                kubernetes.createAddressApi(),
                kubernetes,
                clusterGenerator,
                certDir,
                eventLogger,
                schemaApi);

        log.info("Deploying address space controller for " + addressSpace);
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(addressController, result -> {
            if (result.succeeded()) {
                log.info("Address space controller for {} deployed", addressSpace);
            } else {
                log.warn("Unable to deploy address controller for {}", addressSpace);
            }
        });
        vertx.createHttpServer()
                .requestHandler(request -> request.response().setStatusCode(200).end()).listen(8889);
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }
}
