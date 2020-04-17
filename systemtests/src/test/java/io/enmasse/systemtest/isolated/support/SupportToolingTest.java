/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.config.LabelKeys;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.clients.ClientUtils;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import javax.security.sasl.AuthenticationException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag(ISOLATED)
class SupportToolingTest extends TestBase implements ITestBaseIsolated {
    private static Logger log = CustomLogger.getLogger();

    @ParameterizedTest(name = "testBrokerSupportTooling-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testBrokerSupportTooling(String type) throws Exception {

        AddressSpace space = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("support-tooling-" + type)
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type)
                .withPlan(AddressSpaceType.STANDARD.toString().equals(type) ? AddressSpacePlans.STANDARD_SMALL : AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        Address addr = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(space.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(space, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(AddressSpaceType.STANDARD.toString().equals(type) ? DestinationPlan.STANDARD_SMALL_QUEUE : DestinationPlan.BROKERED_QUEUE)
                .endSpec()
                .build();

        isolatedResourcesManager.createAddressSpaceList(space);
        resourcesManager.setAddresses(addr);

        Map<String, String> brokerLabels = new HashMap<>();
        brokerLabels.put(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(space));
        brokerLabels.put(LabelKeys.ROLE, "broker");

        Map<String, String> secretLabels = new HashMap<>();
        secretLabels.put(LabelKeys.INFRA_UUID, AddressSpaceUtils.getAddressSpaceInfraUuid(space));
        secretLabels.put(LabelKeys.ROLE, "support-credentials");

        Secret supportSecret = kubernetes.listSecrets(secretLabels).get(0);
        Map<String, String> data = supportSecret.getData();
        String supportUser = new String(Base64.getDecoder().decode(data.get("username")), StandardCharsets.UTF_8);
        String supportPassword = new String(Base64.getDecoder().decode(data.get("password")), StandardCharsets.UTF_8);

        // Workaround - addresses may report ready before the broker pod backing the address report ready=true.  This happens because broker liveness/readiness is judged on a
        // Jolokia based probe. As jolokia becomes available after AMQP management, address can be ready when the broker is not. See https://github.com/EnMasseProject/enmasse/issues/2979
        kubernetes.awaitPodsReady(new TimeoutBudget(5, TimeUnit.MINUTES));

        List<Pod> brokerPods = kubernetes.listPods(brokerLabels);
        assertThat(brokerPods.size(), is(1));

        brokerPods.forEach(bp -> {
            String podName = bp.getMetadata().getName();
            ExecutionResultData jmxResponse = KubeCMDClient.runOnPod(kubernetes.getInfraNamespace(), podName, Optional.of("broker"),
                    "curl",
                    "--silent", "--insecure",
                    "--user", String.format("%s:%s", supportUser, supportPassword),
                    String.format("https://localhost:8161/console/jolokia/read/org.apache.activemq.artemis:broker=\"%s\"/AddressMemoryUsage", podName)
            );

            assertThat(jmxResponse.getRetCode(), is(true));
            Map<String, Object> readValue = jsonResponseToMap(jmxResponse.getStdOut());
            assertThat(readValue.get("status"), is(200));

            ExecutionResultData artemisCmdResponse = KubeCMDClient.runOnPod(kubernetes.getInfraNamespace(), podName, Optional.of("broker"),
                    "bash",
                    "-c",
                    "cd ${ARTEMIS_HOME:-${AMQ_HOME}}; ${0} ${@}", /* Handles upstream/downstream locations */
                    "./bin/artemis",
                    "address",
                    "show",
                    "--user", supportUser,
                    "--password", supportPassword
            );
            assertThat(artemisCmdResponse.getRetCode(), is(true));
            List<String> addresses = Arrays.asList(artemisCmdResponse.getStdOut().split("\n"));
            assertThat(addresses, hasItem(addr.getSpec().getAddress()));

            if (AddressSpaceType.STANDARD.toString().equals(type)) {
                // FIXME: can't protect the brokered address space owing to #4295
                Assertions.assertThrows(AuthenticationException.class,
                        () -> new ClientUtils().connectAddressSpace(resourcesManager, space, new UserCredentials(supportUser, supportPassword)),
                        "Must not be able to connect to the address space for messaging using support credentials");
            }
        });
    }

    private Map<String, Object> jsonResponseToMap(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
