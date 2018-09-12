/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KubeKeycloakFactory implements KeycloakFactory {
    private static final Logger log = LoggerFactory.getLogger(KubeKeycloakFactory.class.getName());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final NamespacedOpenShiftClient openShiftClient;
    private final String keycloakConfigName;
    private final String keycloakCredentialsSecretName;
    private final String keycloakCertSecretName;

    public KubeKeycloakFactory(NamespacedOpenShiftClient openShiftClient, String keycloakConfigName, String keycloakCredentialsSecretName, String keycloakCertSecretName) {
        this.openShiftClient = openShiftClient;
        this.keycloakConfigName = keycloakConfigName;
        this.keycloakCredentialsSecretName = keycloakCredentialsSecretName;
        this.keycloakCertSecretName = keycloakCertSecretName;
    }

    @Override
    public Keycloak createInstance() {
        ConfigMap keycloakConfig = openShiftClient.configMaps().withName(keycloakConfigName).get();
        Secret credentials = openShiftClient.secrets().withName(keycloakCredentialsSecretName).get();

        String keycloakUri = String.format("https://%s:8443/auth", keycloakConfig.getData().get("hostname"));
        Base64.Decoder b64dec = Base64.getDecoder();
        String adminUser = new String(b64dec.decode(credentials.getData().get("admin.username")), StandardCharsets.UTF_8);
        String adminPassword = new String(b64dec.decode(credentials.getData().get("admin.password")), StandardCharsets.UTF_8);
        log.info("User keycloak URI {}", keycloakUri);

        Secret certificate = openShiftClient.secrets().withName(keycloakCertSecretName).get();

        KeyStore trustStore = createKeyStore(b64dec.decode(certificate.getData().get("tls.crt")));
        ResteasyClient resteasyClient = new ResteasyClientBuilder()
                .establishConnectionTimeout(30, TimeUnit.SECONDS)
                .connectionPoolSize(1)
                .asyncExecutor(executorService)
                .trustStore(trustStore)
                .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                .build();
        return KeycloakBuilder.builder()
                .serverUrl(keycloakUri)
                .realm("master")
                .username(adminUser)
                .password(adminPassword)
                .clientId("admin-cli")
                .resteasyClient(resteasyClient)
                .build();
    }

    private static KeyStore createKeyStore(byte [] ca) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keyStore.setCertificateEntry("keycloak",
                    cf.generateCertificate(new ByteArrayInputStream(ca)));

            return keyStore;
        } catch (Exception ignored) {
            log.warn("Error creating keystore. Ignoring", ignored);
            return null;
        }
    }

}
