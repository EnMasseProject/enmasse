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

    public static Map<String, Object> getAddressSettings(Kubernetes kubernetes, AddressSpace addressSpace, String addressName)  {
        Map<String, String> brokerLabels = new HashMap<>();
        brokerLabels.put(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        brokerLabels.put(LabelKeys.ROLE, "broker");

        UserCredentials credentials = getSupportCredentials(addressSpace);

        try {
            kubernetes.awaitPodsReady(kubernetes.getInfraNamespace(), Collections.singletonMap("app", "enmasse"), new TimeoutBudget(5, TimeUnit.MINUTES));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        List<Pod> brokerPods = Kubernetes.getInstance().listPods(brokerLabels);
        assertThat(brokerPods.size(), is(1));

        String podName = brokerPods.get(0).getMetadata().getName();
        Map<String, Object> addressSettings = getAddressSettings(kubernetes, podName, credentials, addressName);

        return addressSettings;
    }

    public static Map<String, Object> getAddressSettings(Kubernetes kubernetes, String brokerPodName, UserCredentials supportCredentials, String addressName) {
        ExecutionResultData jmxResponse = KubeCMDClient.runOnPod(kubernetes.getInfraNamespace(), brokerPodName, Optional.of("broker"),
                "curl",
                "--silent", "--insecure",
                "--user", String.format("%s:%s", supportCredentials.getUsername(), supportCredentials.getPassword()),
                "-H", "Origin: https://localhost:8161",
                String.format("https://localhost:8161/console/jolokia/exec/org.apache.activemq.artemis:broker=\"%s\"/getAddressSettingsAsJSON/%s", brokerPodName, addressName));
        assertThat(String.format("Failed to invoke getAddressSettingsAsJSON query: %s : %s", jmxResponse.getStdOut(), jmxResponse.getStdErr()), jmxResponse.getRetCode(), is(true));

        String responseJson = jmxResponse.getTrimmedStdOut();
        Map<String, Object> map = jsonResponseToMap(responseJson);

        return jsonResponseToMap((String) map.get("value"));
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
