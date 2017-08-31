/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.config.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import io.enmasse.config.service.amqp.AMQPServer;
import io.enmasse.config.service.config.ConfigSubscriptionConfig;
import io.enmasse.config.service.kubernetes.KubernetesResourceDatabase;
import io.enmasse.config.service.model.ResourceDatabase;
import io.enmasse.config.service.podsense.PodSenseSubscriptionConfig;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServerOptions;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.impl.ProtonSaslExternalImpl;

import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;

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
            System.out.println("Error running config service");
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
        return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
    }

    private static String getAuthenticationToken() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    private static class ExternalSaslAuthenticator implements ProtonSaslAuthenticator {
        private Sasl sasl;
        private boolean succeeded;

        @Override
        public void init(final NetSocket socket,
                         final ProtonConnection protonConnection,
                         final Transport transport) {
            sasl = transport.sasl();
            sasl.server();
            sasl.allowSkip(false);
            sasl.setMechanisms(ProtonSaslExternalImpl.MECH_NAME);
            succeeded = false;
        }

        @Override
        public void process(final Handler<Boolean> completionHandler) {
            if (sasl == null) {
                throw new IllegalStateException("Init was not called with the associated transport");
            }
            // Note we do not record the identity with which the client authenticated, nor to we take any notice of
            // an alternative identity passed in the response
            boolean done = false;
            String[] remoteMechanisms = sasl.getRemoteMechanisms();
            if (remoteMechanisms.length > 0) {
                String chosen = remoteMechanisms[0];
                if (ProtonSaslExternalImpl.MECH_NAME.equals(chosen)) {
                    // TODO - should handle the case of no initial response per the SASL spec, (i.e. send an empty challenge)
                    // however this was causing errors in some clients
                    // Missing initial response can be detected with: sasl.recv(new byte[0], 0, 0) == -1
                    sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
                    succeeded = true;
                } else {
                    sasl.done(Sasl.SaslOutcome.PN_SASL_AUTH);
                }
                done = true;
            }
            completionHandler.handle(done);
        }

        @Override
        public boolean succeeded() {
            return succeeded;
        }
    }
}
