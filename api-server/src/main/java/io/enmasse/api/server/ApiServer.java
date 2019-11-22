/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.api.auth.ApiHeaderConfig;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.common.OpenShift;
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
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.vertx.core.*;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Clock;
import java.util.List;
import java.util.Map;

public class ApiServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ApiServer.class.getName());
    private final NamespacedKubernetesClient client;
    private final ApiServerOptions options;
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        try {
            CustomResourceDefinitions.registerAll();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw new ExceptionInInitializerError(t);
        }
    }

    private ApiServer(ApiServerOptions options) throws IOException {
        Config config = new ConfigBuilder().build();
        OkHttpClient httpClient = HttpClientUtils.createHttpClient(config);
        httpClient = httpClient.newBuilder()
                .connectTimeout(options.getKubernetesApiConnectTimeout())
                .writeTimeout(options.getKubernetesApiWriteTimeout())
                .readTimeout(options.getKubernetesApiReadTimeout())
                .build();
        this.client = new DefaultKubernetesClient(httpClient, config);
        this.options = options;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        boolean isOpenShift = OpenShift.isOpenShift(client);
        SchemaApi schemaApi = KubeSchemaApi.create(client, client.getNamespace(), options.getVersion(), isOpenShift);
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        AddressSpaceApi addressSpaceApi = KubeAddressSpaceApi.create(client, null, options.getVersion());

        AuthApi authApi = new KubeAuthApi(client, client.getConfiguration().getOauthToken());

        AuthenticationServiceRegistry authenticationServiceRegistry = new SchemaAuthenticationServiceRegistry(schemaProvider);

        Clock clock = Clock.systemUTC();
        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(client);
        KeycloakUserApi keycloakUserApi = new KeycloakUserApi(keycloakFactory, clock, options.getUserApiTimeout());
        schemaProvider.registerListener(newSchema -> keycloakUserApi.retainAuthenticationServices(newSchema.findAuthenticationServiceType(AuthenticationServiceType.standard)));
        UserApi userApi = new DelegateUserApi(Map.of(AuthenticationServiceType.none, new NullUserApi(),
                AuthenticationServiceType.external, new NullUserApi(),
                AuthenticationServiceType.standard, keycloakUserApi));

        String clientCa;
        String requestHeaderClientCa;
        ApiHeaderConfig apiHeaderConfig = ApiHeaderConfig.DEFAULT_HEADERS_CONFIG;
        try {
            ConfigMap extensionApiserverAuthentication = client.configMaps().inNamespace(options.getApiserverClientCaConfigNamespace()).withName(options.getApiserverClientCaConfigName()).get();
            clientCa = validateCert("client-ca", extensionApiserverAuthentication.getData().get("client-ca-file"));
            apiHeaderConfig = parseApiHeaderConfig(extensionApiserverAuthentication, apiHeaderConfig);
            requestHeaderClientCa = validateCert("request-header-client-ca", extensionApiserverAuthentication.getData().get("requestheader-client-ca-file"));
        } catch (KubernetesClientException e) {
            log.info("Unable to retrieve config for client CA. Skipping", e);
            clientCa = null;
            requestHeaderClientCa = null;
        }

        Metrics metrics = new Metrics();

        HTTPHealthServer httpHealthServer = new HTTPHealthServer(options.getVersion(), metrics);

        ResteasyDeploymentFactory resteasyDeploymentFactory = new ResteasyDeploymentFactory(addressSpaceApi, schemaProvider, authApi, userApi, clock, authenticationServiceRegistry, apiHeaderConfig, metrics, options.isEnableRbac());
        String finalRequestHeaderClientCa = requestHeaderClientCa;
        String finalClientCa = clientCa;
        vertx.deployVerticle(() -> new HTTPServer(options, resteasyDeploymentFactory, finalClientCa, finalRequestHeaderClientCa),
                new DeploymentOptions().setWorker(true).setInstances(options.getNumWorkerThreads()), result -> {
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

    private ApiHeaderConfig parseApiHeaderConfig(ConfigMap extensionApiserverAuthentication, ApiHeaderConfig defaultConfig) throws IOException {
        String userJson = extensionApiserverAuthentication.getData().get("requestheader-username-headers");
        String groupJson = extensionApiserverAuthentication.getData().get("requestheader-group-headers");
        String extraJson = extensionApiserverAuthentication.getData().get("requestheader-extra-headers-prefix");

        List<String> userHeader = defaultConfig.getUserHeaders();
        if (userJson != null) {
            userHeader = mapper.readValue(userJson, new TypeReference<List<String>>() {});
        }

        List<String> groupHeader = defaultConfig.getGroupHeaders();
        if (groupJson != null) {
            groupHeader = mapper.readValue(groupJson, new TypeReference<List<String>>() {});
        }

        List<String> extraHeader = defaultConfig.getExtraHeadersPrefix();
        if (extraJson != null) {
            extraHeader = mapper.readValue(extraJson, new TypeReference<List<String>>() {});
        }

        return new ApiHeaderConfig(userHeader, groupHeader, extraHeader);
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

    public static void main(String args[]) {
        try {
            final ApiServerOptions options = ApiServerOptions.fromEnv(System.getenv());
            Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(options.getNumWorkerThreads()));
            log.info("ApiServer starting with options: {}", options);
            vertx.deployVerticle(new ApiServer(options));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting API server: " + e.getMessage());
            System.exit(1);
        }
    }
}
