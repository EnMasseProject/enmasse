/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.common.CachingSchemaProvider;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapSchemaApi;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;

public class ApiServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ApiServer.class.getName());
    private final NamespacedOpenShiftClient client;
    private final ApiServerOptions options;

    private ApiServer(ApiServerOptions options) {
        this.client = new DefaultOpenShiftClient();
        this.options = options;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        SchemaApi schemaApi = new ConfigMapSchemaApi(client, options.getNamespace());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        if (options.getRestapiRouteName() != null) {
            ensureRouteExists(client, options);
        }

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(client);

        AuthApi authApi = new KubeAuthApi(client, client.getConfiguration().getOauthToken());

        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(client,
                options.getStandardAuthserviceConfigName(),
                options.getStandardAuthserviceCredentialsSecretName(),
                options.getStandardAuthserviceCertSecretName());
        Clock clock = Clock.systemUTC();
        UserApi userApi = new KeycloakUserApi(keycloakFactory, clock);

        String clientCa = null;
        String requestHeaderClientCa = null;
        try {
            ConfigMap extensionApiserverAuthentication = client.configMaps().inNamespace(options.getApiserverClientCaConfigNamespace()).withName(options.getApiserverClientCaConfigName()).get();
            clientCa = extensionApiserverAuthentication.getData().get("client-ca.file");
            requestHeaderClientCa = extensionApiserverAuthentication.getData().get("requestheader-client-ca-file");
        } catch (KubernetesClientException e) {
            log.info("Unable to retrieve config for client CA. Skipping", e);
        }

        HTTPServer httpServer = new HTTPServer(addressSpaceApi, schemaProvider, options.getCertDir(), clientCa, requestHeaderClientCa, authApi, userApi, options.isEnableRbac());

        vertx.deployVerticle(httpServer, new DeploymentOptions().setWorker(true), result -> {
            if (result.succeeded()) {
                log.info("API Server started successfully");
            } else {
                log.error("API Server failed to start", result.cause());
            }
        });
    }

    private void ensureRouteExists(NamespacedOpenShiftClient client, ApiServerOptions options) throws IOException {
        if (isOpenShift(client)) {
            Route restapiRoute= client.routes().withName(options.getRestapiRouteName()).get();
            if (restapiRoute == null) {
                log.info("Creating REST API external route {}", options.getRestapiRouteName());
                String caCertificate = new String(Files.readAllBytes(new File(options.getCertDir(), "tls.crt").toPath()), StandardCharsets.UTF_8);
                client.routes().createNew()
                        .editOrNewMetadata()
                        .withName(options.getRestapiRouteName())
                        .addToLabels("app", "enmasse")
                        .endMetadata()
                        .editOrNewSpec()
                        .editOrNewPort()
                        .withNewTargetPort("https")
                        .endPort()
                        .editOrNewTo()
                        .withKind("Service")
                        .withName("api-server")
                        .endTo()
                        .editOrNewTls()
                        .withTermination("reencrypt")
                        .withCaCertificate(caCertificate)
                        .endTls()
                        .endSpec()
                        .done();
            }
        } else {
            Service restapiService = client.services().withName(options.getRestapiRouteName()).get();
            if (restapiService == null) {
                log.info("Creating REST API external service {}", options.getRestapiRouteName());
                client.services().createNew()
                        .editOrNewMetadata()
                        .withName(options.getRestapiRouteName())
                        .addToLabels("app", "enmasse")
                        .endMetadata()
                        .editOrNewSpec()
                        .addNewPort()
                        .withName("https")
                        .withPort(443)
                        .withTargetPort(new IntOrString("https"))
                        .endPort()
                        .addToSelector("component", "api-server")
                        .withType("LoadBalancer")
                        .endSpec()
                        .done();
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
