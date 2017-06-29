package enmasse.controller.auth;

import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Controller that creates self-signed certificates for instances.
 */
public class SelfSignedCertManager implements CertManager {
    private static final Logger log = LoggerFactory.getLogger(SelfSignedCertManager.class.getName());
    private final OpenShiftClient client;

    public SelfSignedCertManager(OpenShiftClient controllerClient) {
        this.client = controllerClient;
    }

    @Override
    public void issueCert(String secretName, String namespace, String ... hostnames) throws Exception {
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret != null) { // && instance.messagingHost().isPresent() && instance.mqttHost().isPresent() && instance.consoleHost().isPresent()) {
            // TODO: Have this sign certificates with OpenShift CA

            String keyKey = "server-key.pem";
            String certKey = "server-cert.pem";
            if (secret.getData() != null && secret.getData().containsKey(keyKey) && secret.getData().containsKey(certKey)) {
                return;
            }

            log.info("Creating self-signed certificates for " + hostnames);
            File keyFile = new File("/tmp/server-key.pem");
            File certFile = new File("/tmp/server-cert.pem");
            ProcessBuilder keyGenBuilder = new ProcessBuilder("openssl", "req", "-new", "-x509", "-batch", "-nodes",
                    "-out", certFile.getAbsolutePath(), "-keyout", keyFile.getAbsolutePath()); //, "-subj", "/CN=" + instance.messagingHost().get() + "," + instance.mqttHost().get() + "," + instance.consoleHost().get());
            log.info("Generating keys using " + keyGenBuilder.command());
            Process keyGen = keyGenBuilder.start();
            if (!keyGen.waitFor(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Key generation timed out");
            }

            Map<String, String> data = new LinkedHashMap<>();
            Base64.Encoder encoder = Base64.getEncoder();
            data.put(keyKey, encoder.encodeToString(FileUtils.readFileToByteArray(keyFile)));
            data.put(certKey, encoder.encodeToString(FileUtils.readFileToByteArray(certFile)));
            client.secrets().inNamespace(namespace).withName(secretName).edit()
                    .addToData(data)
                    .done();
        }
    }

    public static SelfSignedCertManager create(OpenShiftClient controllerClient) {
        return new SelfSignedCertManager(controllerClient);
    }

}
