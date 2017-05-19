package enmasse.controller.cert;

import enmasse.controller.model.Instance;
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
public class SelfSignedController extends InstanceWatcher {
    private static final Logger log = LoggerFactory.getLogger(SelfSignedController.class.getName());
    private final OpenShiftClient client;

    private SelfSignedController(OpenShiftClient client) {
        super(client);
        this.client = client;
    }

    @Override
    protected void instanceChanged(Instance instance) throws Exception {

        if (instance.certSecret().isPresent() && instance.messagingHost().isPresent() && instance.mqttHost().isPresent() && instance.consoleHost().isPresent()) {
            String secret = instance.certSecret().get();
            // TODO: Have this sign certificates with OpenShift CA
            log.info("Creating self-signed certificates for " + instance);

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
            data.put("server-key.pem", encoder.encodeToString(FileUtils.readFileToByteArray(keyFile)));
            data.put("server-cert.pem", encoder.encodeToString(FileUtils.readFileToByteArray(certFile)));
            client.secrets().inNamespace(instance.id().getNamespace()).withName(secret).edit()
                    .addToData(data)
                    .done();
        }
    }

    @Override
    protected void instanceDeleted(Instance instance) {
    }

    public static SelfSignedController create(OpenShiftClient controllerClient) {
        return new SelfSignedController(controllerClient);
    }
}
