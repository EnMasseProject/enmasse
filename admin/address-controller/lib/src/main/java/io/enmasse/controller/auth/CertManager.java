package io.enmasse.controller.auth;

/**
 * Interface for certificate managers
 */
public interface CertManager {
    void issueCert(String secretName, String namespace, String... hostnames) throws Exception;
}
