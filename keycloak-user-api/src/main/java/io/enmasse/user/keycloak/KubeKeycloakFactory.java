/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceSpecStandard;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KubeKeycloakFactory implements KeycloakFactory {
    private static final Logger log = LoggerFactory.getLogger(KubeKeycloakFactory.class.getName());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final NamespacedKubernetesClient kubeClient;
    private static final File serviceCaFile = new File("/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt");

    public KubeKeycloakFactory(NamespacedKubernetesClient kubeClient) {
        this.kubeClient = kubeClient;
    }

    @Override
    public Keycloak createInstance(AuthenticationService authenticationService) {
        if (!authenticationService.getSpec().getType().equals(AuthenticationServiceType.standard) || authenticationService.getStatus() == null) {
            throw new KeycloakUnavailableException("Standard authentication service " + authenticationService.getMetadata().getName() + " is not available");
        }
        log.info("Using standard authentication service '{}'", authenticationService.getMetadata().getName());

        AuthenticationServiceSpecStandard standard = authenticationService.getSpec().getStandard();
        String credentialsSecretNamespace = standard.getCredentialsSecret().getNamespace();
        if (credentialsSecretNamespace == null || credentialsSecretNamespace.isEmpty()) {
            credentialsSecretNamespace = kubeClient.getNamespace();
        }
        Secret credentials = kubeClient.secrets().inNamespace(credentialsSecretNamespace).withName(standard.getCredentialsSecret().getName()).get();

        String keycloakUri = String.format("https://%s:8443/auth", authenticationService.getStatus().getHost());
        Base64.Decoder b64dec = Base64.getDecoder();
        String adminUser = new String(b64dec.decode(credentials.getData().get("admin.username")), StandardCharsets.UTF_8);
        String adminPassword = new String(b64dec.decode(credentials.getData().get("admin.password")), StandardCharsets.UTF_8);
        log.info("User keycloak URI {}", keycloakUri);

        String certificateSecretNamespace = standard.getCredentialsSecret().getNamespace();
        if (certificateSecretNamespace == null || certificateSecretNamespace.isEmpty()) {
            certificateSecretNamespace = kubeClient.getNamespace();
        }
        Secret certificate = kubeClient.secrets().inNamespace(certificateSecretNamespace).withName(standard.getCertificateSecret().getName()).get();

        byte []serviceCa = null;
        try {
            serviceCa = Files.readAllBytes(serviceCaFile.toPath());
        } catch (IOException e) {
            log.warn("Unable to load service CA, ignoring", e);
        }
        KeyStore trustStore = createKeyStore(b64dec.decode(certificate.getData().get("tls.crt")), serviceCa);
        ResteasyJackson2Provider provider = new ResteasyJackson2Provider() {
            @Override
            public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
                ObjectMapper mapper = locateMapper(type, mediaType);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                super.writeTo(value, type, genericType, annotations, mediaType, httpHeaders, entityStream);
            }
        };
        ResteasyClient resteasyClient = new ResteasyClientBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .connectionPoolSize(1)
                .asyncExecutor(executorService) // executorService is the replacement but returns the wrong type
                .trustStore(trustStore)
                .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                .register(provider)
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

    private static KeyStore createKeyStore(byte [] ca, byte [] serviceCa) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            if (ca != null) {
                keyStore.setCertificateEntry("keycloak",
                        cf.generateCertificate(new ByteArrayInputStream(ca)));
            }
            if (serviceCa != null) {
                keyStore.setCertificateEntry("service-ca",
                        cf.generateCertificate(new ByteArrayInputStream(serviceCa)));
            }
            return keyStore;
        } catch (Exception ignored) {
            log.warn("Error creating keystore. Ignoring", ignored);
            return null;
        }
    }

}
