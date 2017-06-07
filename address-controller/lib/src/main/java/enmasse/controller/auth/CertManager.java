package enmasse.controller.auth;

import enmasse.controller.model.Instance;

import java.io.IOException;

/**
 * Interface for certificate managers
 */
public interface CertManager {
    void issueCert(String secretName, String namespace, String... hostnames) throws Exception;
}
