package enmasse.controller.instance.cert;

import enmasse.controller.model.Instance;

import java.io.IOException;

/**
 * Interface for certificate managers
 */
public interface CertManager {
    void updateCerts(Instance instance) throws Exception;
}
