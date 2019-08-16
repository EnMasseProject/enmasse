/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.time.Clock;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.k8s.api.CachingSchemaProvider;
import io.enmasse.k8s.api.ConfigMapAddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.KubeEventLogger;
import io.enmasse.k8s.api.KubeSchemaApi;
import io.enmasse.k8s.api.LogEventLogger;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.model.CustomResourceDefinitions;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.vertx.core.Vertx;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class StandardController {
    private static final Logger log = LoggerFactory.getLogger(StandardController.class.getName());

    static {
        try {
            CustomResourceDefinitions.registerAll();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw new ExceptionInInitializerError(t);
        }
    }

    public static void main(String[] args) throws Exception {
        StandardController standardController = null;
        try {
            Map<String, String> env = System.getenv();
            StandardControllerOptions options = StandardControllerOptions.fromEnv(env);
            log.info("StandardController starting with options: {}", options);
            standardController = new StandardController(options);
            standardController.start();
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address space controller: " + e.getMessage());
            System.exit(1);
        } finally {
            if (standardController != null) {
                Runtime.getRuntime().addShutdownHook(new Thread(standardController::stop));
            }
        }
    }

    private final NamespacedKubernetesClient kubeClient;
    private final StandardControllerOptions options;
    private AddressController addressController;
    private HTTPServer httpServer;

    public StandardController(StandardControllerOptions options) {
        this.kubeClient = new DefaultKubernetesClient();
        this.options = options;
    }

    public void start() throws Exception {

        SchemaApi schemaApi = KubeSchemaApi.create(kubeClient, kubeClient.getNamespace(), options.getVersion(), false);
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        Kubernetes kubernetes = new KubernetesHelper(kubeClient, options.getTemplateDir(), options.getInfraUuid());
        BrokerSetGenerator clusterGenerator = new TemplateBrokerSetGenerator(kubernetes, options, System.getenv());

        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(kubeClient, kubeClient.getNamespace(), Clock.systemUTC(), "standard-controller")
                : new LogEventLogger();

        Metrics metrics = new Metrics();

        Vertx vertx = Vertx.vertx();

        BrokerClientFactory brokerClientFactory = new MutualTlsBrokerClientFactory(vertx, options);

        addressController = new AddressController(
                options,
                new ConfigMapAddressApi(kubeClient, options.getInfraUuid()),
                kubernetes,
                clusterGenerator,
                eventLogger,
                schemaProvider,
                vertx,
                metrics,
                new RandomBrokerIdGenerator(),
                brokerClientFactory);

        log.info("Starting standard controller for " + options.getAddressSpace());
        addressController.start();

        httpServer = new HTTPServer( 8889, metrics);
        httpServer.start();
    }

    public void stop() {
        try {
            log.info("StandardController stopping");

            if (httpServer != null) {
                httpServer.stop();
            }
        } finally {
            try {
                if (addressController != null) {
                    try {
                        addressController.stop();
                    } catch (Exception ignore) {
                    }
                }
            } finally {
                kubeClient.close();
                log.info("StandardController stopped");
            }
        }
    }
}
