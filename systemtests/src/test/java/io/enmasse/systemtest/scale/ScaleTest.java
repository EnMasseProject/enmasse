/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.Phase;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouterBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.scale.metrics.MessagingClientMetricsClient;
import io.enmasse.systemtest.scale.metrics.ProbeClientMetricsClient;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.SCALE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(SCALE)
class ScaleTest extends TestBase implements ITestBaseIsolated {
    private final static Logger LOGGER = CustomLogger.getLogger();
    private final String namespace = "scale-test-namespace";
    final String addressSpacePlanName = "test-addressspace-plan";
    final String queuePlanName = "test-queue-plan";
    final String anycastPlanName = "test-anycast-plan";
    final String authServiceName = "scale-authservice";
    AddressSpace addressSpace = null;
    Endpoint messagingEndpoint;
    final UserCredentials userCredentials = new UserCredentials("scale-test-user", "password");
    List<ScaleTestClientConfiguration> clients;
    Supplier<ScaleTestClientConfiguration> clientProvider;

    @BeforeEach
    void init() throws Exception {
        kubernetes.createNamespace(namespace);
        setupEnv();
    }

    @AfterEach
    void tearDown(ExtensionContext extensionContext) throws Exception {
        GlobalLogCollector.saveInfraState(TestUtils.getScaleTestLogsPath(extensionContext));
        kubernetes.deleteNamespace(namespace);
        SystemtestsKubernetesApps.cleanScaleTestEnv(kubernetes);
    }

    @Test
    void testNumberOfSupportedAddresses() throws Exception {
        int operableAddresses;
        int iterator = 0;
        int failureThreshold = 15_000;
        List<Address> addressBatch = new LinkedList<>();

        while (true) {
            try {
                List<Address> addr = getTenantAddressBatch(addressSpace);
                addressBatch.addAll(addr);
                getResourceManager().appendAddresses(false, addr.toArray(new Address[0]));
                if (iterator % 100 == 0) {
                    List<Address> currentAddresses = kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
                    AddressUtils.waitForDestinationsReady(new TimeoutBudget(30, TimeUnit.MINUTES), currentAddresses.toArray(new Address[0]));

                    ScaleTestClientConfiguration client = connectProbeClient(addressBatch.toArray(Address[]::new));
                    addressBatch.clear();

                    var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, client.getClientId());
                    ProbeClientMetricsClient probeClientMetrics = new ProbeClientMetricsClient(metricsEndpoint);

                    Thread.sleep(20_000);
                    assertTrue(probeClientMetrics.getSuccessTotal().getValue() > 0);
                    assertEquals(0, probeClientMetrics.getFailureTotal().getValue());
                }
            } catch (IllegalStateException ex) {
                log.error("Failed to wait for addresses");
                operableAddresses = (int) kubernetes.getAddressClient().inNamespace(namespace).list().getItems().stream()
                        .filter(address -> address.getStatus().getPhase().equals(Phase.Active)).count();
                log.info("----------------------------------------------------------");
                log.info("Total operable addresses {}", operableAddresses);
                log.info("----------------------------------------------------------");
                assertThat(operableAddresses, greaterThan(failureThreshold));
                break;
            }
            iterator++;
        }
    }

    @Test
    void testScaleTestToolsWork() throws Exception {
        int initialAddresses = 5;
        var addresses = createInitialAddresses(initialAddresses).stream().map(a -> a.getSpec().getAddress()).collect(Collectors.toList());

        var endpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        Supplier<ScaleTestClientConfiguration> clientProvider = () -> {
            ScaleTestClientConfiguration client = new ScaleTestClientConfiguration();
            client.setClientType(ScaleTestClientType.probe);
            client.setHostname(endpoint.getHost());
            client.setPort(endpoint.getPort());
            client.setUsername(userCredentials.getUsername());
            client.setPassword(userCredentials.getPassword());
            return client;
        };
        {
            ScaleTestClientConfiguration client = clientProvider.get();
            client.setAddresses(addresses.toArray(new String[0]));

            SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, client);

            var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, client.getClientId());
            ProbeClientMetricsClient probeClientMetrics = new ProbeClientMetricsClient(metricsEndpoint);

            TestUtils.waitUntilConditionOrFail(() -> {
                return probeClientMetrics.getSuccessTotal().getValue() > 0;
            }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting successfull connections");

            assertTrue(probeClientMetrics.getSuccessTotal().getValue() > 0);
            assertTrue(probeClientMetrics.getFailureTotal().getValue() == 0);

            SystemtestsKubernetesApps.deleteScaleTestClient(kubernetes, client, TestUtils.getScaleTestLogsPath(TestInfo.getInstance().getActualTest()));
        }
        {
            ScaleTestClientConfiguration client = clientProvider.get();
            client.setClientType(ScaleTestClientType.messaging);
            client.setAddresses(addresses.toArray(new String[0]));

            SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, client);

            var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, client.getClientId());
            MessagingClientMetricsClient msgClientMetrics = new MessagingClientMetricsClient(metricsEndpoint);

            TestUtils.waitUntilConditionOrFail(() -> {
                return msgClientMetrics.getConnectSuccess().getValue() > 0;
            }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting successfull connections");

            assertTrue(msgClientMetrics.getConnectSuccess().getValue() > 0);
            assertTrue(msgClientMetrics.getConnectFailure().getValue() == 0);

            SystemtestsKubernetesApps.deleteScaleTestClient(kubernetes, client, TestUtils.getScaleTestLogsPath(TestInfo.getInstance().getActualTest()));
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////
    // Help methods
    ///////////////////////////////////////////////////////////////////////////////////
    private List<Address> getTenantAddressBatch(AddressSpace addressSpace) {
        List<Address> addresses = new ArrayList<>(4);
        String batchSuffix = UUID.randomUUID().toString();
        IntStream.range(0, 4).forEach(i -> addresses.add(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-anycast-" + i + "-" + batchSuffix))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.ANYCAST.toString())
                .withAddress("test-anycast-" + i + "-" + batchSuffix)
                .withPlan(anycastPlanName)
                .endSpec()
                .build()));

        addresses.add(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-" + batchSuffix))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("test-queue-" + batchSuffix)
                .withPlan(queuePlanName)
                .endSpec()
                .build());
        return addresses;
    }

    private void setupEnv() throws Exception {
        //Custom infra config
        LOGGER.info("Creating addressspace/address plans");
        StandardInfraConfig testInfra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-1-standard")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("2Gi")
                        .withStorage("2Gi")
                        .endResources()
                        .withPodTemplate(new PodTemplateSpecBuilder()
                                .withNewSpec()
                                .addNewContainer()
                                .withName("broker")
                                .withNewReadinessProbe()
                                .withFailureThreshold(6)
                                .withInitialDelaySeconds(20)
                                .endReadinessProbe()
                                .endContainer()
                                .endSpec()
                                .build())
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withNewResources()
                        .withMemory("3Gi")
                        .endResources()
                        .withMinReplicas(2)
                        .withLinkCapacity(2000)
                        .build())
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("2Gi")
                        .endResources()
                        .build())
                .endSpec()
                .build();
        getResourceManager().createInfraConfig(testInfra);

        //Custom address plans
        AddressPlan testQueuePlan = PlanUtils.createAddressPlanObject(queuePlanName,
                AddressType.QUEUE,
                Arrays.asList(
                        new ResourceRequest("broker", 0.001),
                        new ResourceRequest("router", 0.0002)));
        AddressPlan testAnycastPlan = PlanUtils.createAddressPlanObject(anycastPlanName,
                AddressType.ANYCAST,
                Collections.singletonList(
                        new ResourceRequest("router", 0.0002)));

        getResourceManager().createAddressPlan(testQueuePlan);
        getResourceManager().createAddressPlan(testAnycastPlan);

        //Custom addressspace plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10_000),
                new ResourceAllowance("router", 10_000),
                new ResourceAllowance("aggregate", 10_000));
        List<AddressPlan> addressPlans = Arrays.asList(testQueuePlan, testAnycastPlan);

        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject(addressSpacePlanName, testInfra.getMetadata().getName(), AddressSpaceType.STANDARD, resources, addressPlans);
        getResourceManager().createAddressSpacePlan(addressSpacePlan);

        LOGGER.info("Create custom auth service");
        //Custom auth service
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject(authServiceName, false);
        resourcesManager.createAuthService(standardAuth);

        LOGGER.info("Create addressspace");
        //Create address space
        addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("addressspace-scale")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(addressSpacePlanName)
                .withNewAuthenticationService()
                .withName(authServiceName)
                .endAuthenticationService()
                .endSpec()
                .build();
        getResourceManager().createAddressSpace(addressSpace);
        getResourceManager().createOrUpdateUser(addressSpace, userCredentials);

        messagingEndpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        Supplier<ScaleTestClientConfiguration> clientProvider = () -> {
            ScaleTestClientConfiguration client = new ScaleTestClientConfiguration();
            client.setClientType(ScaleTestClientType.probe);
            client.setHostname(messagingEndpoint.getHost());
            client.setPort(messagingEndpoint.getPort());
            client.setUsername(userCredentials.getUsername());
            client.setPassword(userCredentials.getPassword());
            return client;
        };
    }

    List<Address> createInitialAddresses(int addresses) throws Exception {
        int operableAddresses = 0;
        int iterator = 0;

        while (operableAddresses < addresses) {
            try {
                getResourceManager().appendAddresses(false, getTenantAddressBatch(addressSpace).toArray(new Address[0]));
                if (iterator % 200 == 0) {
                    List<Address> currentAddresses = kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
                    AddressUtils.waitForDestinationsReady(currentAddresses.toArray(new Address[0]));
                    operableAddresses = currentAddresses.size();
                }
            } catch (IllegalStateException ex) {
                LOGGER.error("Failed to wait for addresses");
                operableAddresses = (int) kubernetes.getAddressClient().inNamespace(namespace).list().getItems().stream()
                        .filter(address -> address.getStatus().getPhase().equals(Phase.Active)).count();
                LOGGER.info("----------------------------------------------------------");
                LOGGER.info("Total operable addresses {}", operableAddresses);
                LOGGER.info("----------------------------------------------------------");
                if (operableAddresses >= addresses) {
                    break;
                }
            }
            iterator++;
        }
        List<Address> currentAddresses = kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
        AddressUtils.waitForDestinationsReady(currentAddresses.toArray(new Address[0]));
        return kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
    }

    private ScaleTestClientConfiguration connectProbeClient(Address... addresses) throws Exception {
        ScaleTestClientConfiguration client = clientProvider.get();
        client.setAddresses(Arrays.stream(addresses).map(address -> address.getSpec().getAddress()).toArray(String[]::new));
        SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, client);
        Thread.sleep(20_000);
        return client;
    }

}

