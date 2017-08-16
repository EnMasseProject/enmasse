/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.controller.auth;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
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
        if (secret != null) {
            // TODO: Have this sign certificates with OpenShift CA

            String keyKey = "server-key.pem";
            String certKey = "server-cert.pem";
            if (secret.getData() != null && secret.getData().containsKey(keyKey) && secret.getData().containsKey(certKey)) {
                return;
            }

            log.info("Creating self-signed certificates for " + Arrays.toString(hostnames));
            File keyFile = new File("/tmp/server-key.pem");
            File certFile = new File("/tmp/server-cert.pem");
            ProcessBuilder keyGenBuilder = new ProcessBuilder("openssl", "req", "-new", "-x509", "-batch", "-nodes",
                    "-out", certFile.getAbsolutePath(), "-keyout", keyFile.getAbsolutePath()); //, "-subj", "/CN=" + addressspace.messagingHost().get() + "," + addressspace.mqttHost().get() + "," + addressspace.consoleHost().get());
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
