/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.LabelKeys;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.CoreMatchers.is;

public class ArtemisUtils {

    public static Map<String, Object> getAddressSettings(Kubernetes kubernetes, AddressSpace addressSpace, String addressName) throws Exception {
        Map<String, String> brokerLabels = new HashMap<>();
        brokerLabels.put(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        brokerLabels.put(LabelKeys.ROLE, "broker");

        UserCredentials credentials = getSupportCredentials(addressSpace);

        kubernetes.awaitPodsReady(kubernetes.getInfraNamespace(), new TimeoutBudget(5, TimeUnit.MINUTES));

        List<Pod> brokerPods = Kubernetes.getInstance().listPods(brokerLabels);
        assertThat(brokerPods.size(), is(1));

        String podName = brokerPods.get(0).getMetadata().getName();
        ExecutionResultData jmxResponse = KubeCMDClient.runOnPod(kubernetes.getInfraNamespace(), podName, Optional.of("broker"),
                "curl",
                "--silent", "--insecure",
                "--user", String.format("%s:%s", credentials.getUsername(), credentials.getPassword()),
                "-H", "Origin: https://localhost:8161",
                String.format("https://localhost:8161/console/jolokia/exec/org.apache.activemq.artemis:broker=\"%s\"/getAddressSettingsAsJSON/%s", podName, addressName));

        String responseJson = jmxResponse.getTrimmedStdOut();
        Map<String, Object> map = jsonResponseToMap(responseJson);

        Map<String, Object> addressSettings = jsonResponseToMap((String) map.get("value"));

        return addressSettings;
    }

    public static UserCredentials getSupportCredentials(AddressSpace addressSpace) {
        Map<String, String> secretLabels = new HashMap<>();
        secretLabels.put(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        secretLabels.put(LabelKeys.ROLE, "support-credentials");

        Secret supportSecret = Kubernetes.getInstance().listSecrets(secretLabels).get(0);
        Map<String, String> data = supportSecret.getData();
        String supportUser = new String(Base64.getDecoder().decode(data.get("username")), StandardCharsets.UTF_8);
        String supportPassword = new String(Base64.getDecoder().decode(data.get("password")), StandardCharsets.UTF_8);

        return new UserCredentials(supportUser, supportPassword);
    }

    private static Map<String, Object> jsonResponseToMap(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

}
