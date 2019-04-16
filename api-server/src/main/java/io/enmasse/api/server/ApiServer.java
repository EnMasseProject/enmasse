/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.k8s.api.*;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.model.CustomResourceDefinitions;
import io.enmasse.user.api.DelegateUserApi;
import io.enmasse.user.api.NullUserApi;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Clock;
import java.util.Map;

public class ApiServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ApiServer.class.getName());
    private final NamespacedKubernetesClient client;
    private final ApiServerOptions options;

    static {
        try {
            CustomResourceDefinitions.registerAll();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw new ExceptionInInitializerError(t);
        }
    }

    private ApiServer(ApiServerOptions options) throws IOException {
        this.client = new DefaultKubernetesClient();
        this.options = options;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        boolean isOpenShift = isOpenShift(client);
        SchemaApi schemaApi = KubeSchemaApi.create(client, client.getNamespace(), options.getVersion(), isOpenShift);
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(client);

        AuthApi authApi = new KubeAuthApi(client, client.getConfiguration().getOauthToken());

        AuthenticationServiceRegistry authenticationServiceRegistry = new SchemaAuthenticationServiceRegistry(schemaProvider);

        Clock clock = Clock.systemUTC();
        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(client);
        KeycloakUserApi keycloakUserApi = new KeycloakUserApi(keycloakFactory, clock, options.getUserApiTimeout());
        schemaProvider.registerListener(newSchema -> keycloakUserApi.pruneAuthenticationServices(newSchema.findAuthenticationServiceType(AuthenticationServiceType.standard)));
        UserApi userApi = new DelegateUserApi(Map.of(AuthenticationServiceType.none, new NullUserApi(),
                AuthenticationServiceType.external, new NullUserApi(),
                AuthenticationServiceType.standard, keycloakUserApi));

        String clientCa;
        String requestHeaderClientCa;
        try {
            ConfigMap extensionApiserverAuthentication = client.configMaps().inNamespace(options.getApiserverClientCaConfigNamespace()).withName(options.getApiserverClientCaConfigName()).get();
            clientCa = validateCert("client-ca", extensionApiserverAuthentication.getData().get("client-ca-file"));
            requestHeaderClientCa = validateCert("request-header-client-ca", extensionApiserverAuthentication.getData().get("requestheader-client-ca-file"));
        } catch (KubernetesClientException e) {
            log.info("Unable to retrieve config for client CA. Skipping", e);
            clientCa = null;
            requestHeaderClientCa = null;
        }

        Metrics metrics = new Metrics();

        HTTPHealthServer httpHealthServer = new HTTPHealthServer(options.getVersion(), metrics);
        HTTPServer httpServer = new HTTPServer(addressSpaceApi, schemaProvider, authApi, userApi, options, clientCa, requestHeaderClientCa, clock, authenticationServiceRegistry);

        vertx.deployVerticle(httpServer, new DeploymentOptions().setWorker(true), result -> {
            if (result.succeeded()) {
                vertx.deployVerticle(httpHealthServer, ar -> {
                    if (ar.succeeded()) {
                        log.info("API Server started successfully");
                        startPromise.complete();
                    } else {
                        log.error("API Server failed to start", result.cause());
                        startPromise.fail(result.cause());
                    }
                });
            } else {
                log.error("API Server failed to start", result.cause());
                startPromise.fail(result.cause());
            }
        });
    }

    private static String validateCert(String id, String ca) {
        if (ca == null) {
            return null;
        }
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cf.generateCertificates(new ByteArrayInputStream(ca.getBytes(StandardCharsets.UTF_8)));
            return ca;
        } catch (CertificateException e) {
            log.info("Error validating certificate {}. Skipping", id);
            return null;
        }
    }

    private static boolean isOpenShift(NamespacedKubernetesClient client) throws InterruptedException {
        int retries = 10;
        while (true) {
            // Need to query the full API path because Kubernetes does not allow GET on /
            OkHttpClient httpClient = client.adapt(OkHttpClient.class);
            HttpUrl url = HttpUrl.get(client.getMasterUrl()).resolve("/apis/route.openshift.io");
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                return response.code() >= 200 && response.code() < 300;
            } catch (IOException e) {
                retries--;
                if (retries <= 0) {
                    throw new UncheckedIOException(e);
                } else {
                    log.warn("Exception when checking API availability, retrying {} times", retries, e);
                }
            }
            Thread.sleep(5000);
        }
    }

    public static void main(String args[]) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new ApiServer(ApiServerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting API server: " + e.getMessage());
            System.exit(1);
        }
    }
}
