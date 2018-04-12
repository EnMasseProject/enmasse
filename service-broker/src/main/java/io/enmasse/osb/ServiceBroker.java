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
import io.enmasse.osb.keycloak.KeycloakApi;
import io.enmasse.osb.keycloak.KeycloakInstance;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        KeycloakApi keycloakApi = createKeycloakApi(options);

        vertx.deployVerticle(new HTTPServer(addressSpaceApi, schemaProvider, authApi, options.getCertDir(), options.getEnableRbac(), keycloakApi, options.getListenPort(), options.getConsolePrefix()),
                result -> {
                    if (result.succeeded()) {
                        log.info("EnMasse Service Broker started");
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }

    private KeycloakApi createKeycloakApi(ServiceBrokerOptions options) throws IOException, GeneralSecurityException {
        KeyStore keyStore = convertCertToKeyStore(options.getKeycloakCa());
        return () -> new KeycloakInstance(KeycloakBuilder.builder()
                .serverUrl(options.getKeycloakUrl())
                .realm("master")
                .username(options.getKeycloakAdminUser())
                .password(options.getKeycloakAdminPassword())
                .clientId("admin-cli")
                .resteasyClient(new ResteasyClientBuilder()
                        .establishConnectionTimeout(30, TimeUnit.SECONDS)
                        .trustStore(keyStore)
                        .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                        .build())
                .build());

    }

    private static KeyStore convertCertToKeyStore(String cert) throws IOException, GeneralSecurityException {
        List<X509Certificate> certs = new ArrayList<>();
        try (InputStream is = new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8))) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                do {
                    certs.add((X509Certificate)cf.generateCertificate(is));
                } while(is.available() != 0);
            } catch (CertificateException e) {
                if(certs.isEmpty()) {
                    throw new InternalServerErrorException("No auth service certificate found in secret", e);
                }
            }
        }

        KeyStore inMemoryKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        inMemoryKeyStore.load(null, null);
        int i = 1;
        for(X509Certificate crt : certs) {
            inMemoryKeyStore.setCertificateEntry(String.valueOf(i++), crt);
        }
        return inMemoryKeyStore;
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
