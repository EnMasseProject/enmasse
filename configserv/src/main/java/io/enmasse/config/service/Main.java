/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import io.enmasse.amqp.ExternalSaslAuthenticator;
import io.enmasse.config.service.amqp.AMQPServer;
import io.enmasse.config.service.config.ConfigSubscriptionConfig;
import io.enmasse.config.service.kubernetes.KubernetesResourceDatabase;
import io.enmasse.config.service.model.ResourceDatabase;
import io.enmasse.config.service.podsense.PodSenseSubscriptionConfig;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonServerOptions;

/**
 * Main entrypoint for configuration service with arg parsing.
 */
public class Main {

    public static void main(String [] args) {
        try {
            Map<String, String> env = System.getenv();
            String openshiftUri = String.format("https://%s:%s", getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST"), getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT"));
            String listenAddress = env.getOrDefault("CONFIGURATION_SERVICE_LISTEN_ADDRESS", "0.0.0.0");
            String certDir =  env.get("CERT_DIR");
            boolean useTls = certDir != null;
            int listenPort = Integer.parseInt(env.getOrDefault("CONFIGURATION_SERVICE_LISTEN_PORT", useTls ? "5671" : "5672"));

            String namespace = getNamespace();

            Config config = new ConfigBuilder().withMasterUrl(openshiftUri).withOauthToken(getAuthenticationToken()).withNamespace(namespace).build();
            KubernetesClient client = new DefaultKubernetesClient(config);

            Map<String, ResourceDatabase> databaseMap = new LinkedHashMap<>();
            databaseMap.put("v1/addresses", new KubernetesResourceDatabase<>(client, new ConfigSubscriptionConfig()));
            databaseMap.put("podsense", new KubernetesResourceDatabase<>(client, new PodSenseSubscriptionConfig()));

            ProtonServerOptions options = useTls ? createOptionsForTls(certDir) : new ProtonServerOptions();

            Vertx vertx = Vertx.vertx();
            AMQPServer server = new AMQPServer(listenAddress, listenPort, databaseMap, options);

            if(useTls) {
                server.setAuthenticatorFactory(ExternalSaslAuthenticator::new);
            }

            vertx.deployVerticle(server);

        } catch (IllegalArgumentException e) {
            System.out.println("Error parsing environment: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error running config service: " + e.getMessage());
            System.exit(1);
        }
    }

    private static ProtonServerOptions createOptionsForTls(final String certDir) {
        ProtonServerOptions options = new ProtonServerOptions();
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        pemKeyCertOptions.setCertPath(certDir + File.separator + "tls.crt");
        pemKeyCertOptions.setKeyPath(certDir + File.separator + "tls.key");
        options.setPemKeyCertOptions(pemKeyCertOptions);
        options.setClientAuth(ClientAuth.REQUIRED);
        options.setSsl(true);
        PemTrustOptions pemTrustOptions = new PemTrustOptions();
        pemTrustOptions.addCertPath(certDir + File.separator + "ca.crt");
        options.setPemTrustOptions(pemTrustOptions);
        return options;
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private static String getNamespace() throws IOException {
        String namespace = System.getenv("KUBERNETES_NAMESPACE");
        if (namespace == null) {
            return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
        } else {
            return namespace;
        }
    }

    private static String getAuthenticationToken() throws IOException {
        String token = System.getenv("KUBERNETES_TOKEN");
        if (token == null) {
            return readFile(new File(SERVICEACCOUNT_PATH, "token"));
        } else {
            return token;
        }
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

}
