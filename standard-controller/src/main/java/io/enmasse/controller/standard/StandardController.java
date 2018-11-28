/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.k8s.api.*;
import io.enmasse.metrics.api.Metric;
import io.enmasse.metrics.api.Metrics;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Map;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class StandardController {
    private static final Logger log = LoggerFactory.getLogger(StandardController.class.getName());

    static {
        try {
            AdminCrd.registerCustomCrds();
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

    private final NamespacedOpenShiftClient openShiftClient;
    private final StandardControllerOptions options;
    private AddressController addressController;
    private HTTPServer httpServer;

    public StandardController(StandardControllerOptions options) {
        this.openShiftClient = new DefaultOpenShiftClient();
        this.options = options;
    }

    public void start() throws Exception {

        SchemaApi schemaApi = KubeSchemaApi.create(openShiftClient, openShiftClient.getNamespace(), isOpenShift(openShiftClient));
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        Kubernetes kubernetes = new KubernetesHelper(openShiftClient, options.getTemplateDir(), options.getInfraUuid());
        BrokerSetGenerator clusterGenerator = new TemplateBrokerSetGenerator(kubernetes, options);

        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(openShiftClient, openShiftClient.getNamespace(), Clock.systemUTC(), "standard-controller")
                : new LogEventLogger();

        Metrics metrics = new Metrics();


        addressController = new AddressController(
                options,
                new ConfigMapAddressApi(openShiftClient, openShiftClient.getNamespace(), options.getInfraUuid()),
                kubernetes,
                clusterGenerator,
                eventLogger,
                schemaProvider,
                metrics);

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
                openShiftClient.close();
                log.info("StandardController stopped");
            }
        }
    }

    private static boolean isOpenShift(NamespacedOpenShiftClient client) {
        // Need to query the full API path because Kubernetes does not allow GET on /
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);
        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve("/apis/route.openshift.io");
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            return response.code() >= 200 && response.code() < 300;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
