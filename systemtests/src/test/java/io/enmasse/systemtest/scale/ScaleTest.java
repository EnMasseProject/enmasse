/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static io.enmasse.systemtest.TestTag.SCALE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hawkular.agent.prometheus.types.Counter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

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
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;

@Tag(SCALE)
class ScaleTest extends TestBase implements ITestBaseIsolated {
    private final static Logger LOGGER = CustomLogger.getLogger();
    private final String namespace = "scale-test-namespace";
    final String addressSpacePlanName = "test-addressspace-plan";
    final String queuePlanName = "test-queue-plan";
    final String anycastPlanName = "test-anycast-plan";
    final String authServiceName = "scale-authservice";
    AddressSpace addressSpace = null;
    final UserCredentials userCredentials = new UserCredentials("scale-test-user", "password");
    private final int uuidSize = 36;

    @BeforeEach
    void init() throws Exception {
        kubernetes.createNamespace(namespace);
        setupEnv();
        SystemtestsKubernetesApps.setupScaleTestEnv(kubernetes);
    }

    @AfterEach
    void tearDown(ExtensionContext extensionContext) throws Exception {
        Path logsPath = TestUtils.getScaleTestLogsPath(extensionContext);
        GlobalLogCollector.saveInfraState(logsPath);
        SystemtestsKubernetesApps.cleanScaleTestEnv(kubernetes, logsPath);
        kubernetes.deleteNamespace(namespace);
    }

    @Test
    void testScaleTestToolsWork() throws Exception {
        int initialAddresses = 5;
        var addresses = createInitialAddresses(initialAddresses).stream().map(a->a.getSpec().getAddress()).collect(Collectors.toList());

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
                return probeClientMetrics.getSuccessTotal().getValue()>0;
            }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting successfull connections");

            assertTrue(probeClientMetrics.getSuccessTotal().getValue()>0);
            assertTrue(probeClientMetrics.getFailureTotal().getValue()==0);

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
                var counter = msgClientMetrics.getAcceptedDeliveries(AddressType.QUEUE);
                return counter.isPresent() && counter.get().getValue() >= 0;
            }, Duration.ofSeconds(35), Duration.ofSeconds(5), () -> "Client is not reporting accepted deliveries");
            Thread.sleep(30000);
            assertTrue(msgClientMetrics.getConnectSuccess().getValue()>0);
            assertTrue(msgClientMetrics.getConnectFailure().getValue()==0);
            metricAssertTrue(msgClientMetrics.getRejectedDeliveries(AddressType.QUEUE), c -> c == null || c.getValue() == 0);
            metricAssertTrue(msgClientMetrics.getAcceptedDeliveries(AddressType.QUEUE), c -> c != null && c.getValue() >= 0);
            metricAssertTrue(msgClientMetrics.getReceivedDeliveries(AddressType.QUEUE), c -> c != null && c.getValue() >= 0);
        }
    }

    @Test
    void testMessagingPerformance() throws Exception {
        int initialAddresses = 12000;
//        int initialAddresses = 50;
        var addresses = createInitialAddresses(initialAddresses);
        Collections.sort(addresses, (a1, a2) -> {
            var address1 = a1.getSpec().getAddress();
            var address2 = a2.getSpec().getAddress();
            return address1.substring(address1.length()-uuidSize).compareTo(address2.substring(address2.length()-uuidSize));
        });

//        LOGGER.info("#######################################");
//        LOGGER.info("#######################################");
//        LOGGER.info("Addresses are {}", addresses.stream().map(a -> a.getSpec().getAddress()).collect(Collectors.toList()).toString());
//        LOGGER.info("#######################################");
//        LOGGER.info("#######################################");


        var endpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        Supplier<ScaleTestClientConfiguration<MessagingClientMetricsClient>> clientProvider = () -> {
            ScaleTestClientConfiguration<MessagingClientMetricsClient> client = new ScaleTestClientConfiguration<>();
            client.setClientType(ScaleTestClientType.messaging);
            client.setHostname(endpoint.getHost());
            client.setPort(endpoint.getPort());
            client.setUsername(userCredentials.getUsername());
            client.setPassword(userCredentials.getPassword());
            return client;
        };

        int totalConnections = 0;

        List<ScaleTestClientConfiguration<MessagingClientMetricsClient>> clients = new ArrayList<>();
        int anycastLinksPerConnection = 1;
        int queueLinksPerConnection = 1;
        int addressesPerGroup = 200;

        try {

        while (true) {
            //determine load
            List<List<Address>> addressesGroups = new ArrayList<>();
            for (int i = 0; i < addresses.size() / addressesPerGroup; i++) {
                addressesGroups.add(addresses.subList(i * addressesPerGroup, (i + 1) * addressesPerGroup));
            }

            //deploy clients and start messaging
            for (var group : addressesGroups) {
                var anycasts = group.stream().filter(a -> a.getSpec().getType().equals(AddressType.ANYCAST.toString())).collect(Collectors.toList());
                var queues = group.stream().filter(a -> a.getSpec().getType().equals(AddressType.QUEUE.toString())).collect(Collectors.toList());

                var clientAnycasts = clientProvider.get();
                clientAnycasts.setAddressesType(AddressType.ANYCAST);
                clientAnycasts.setAddresses(anycasts.stream().map(a -> a.getSpec().getAddress()).toArray(String[]::new));
                clientAnycasts.setLinksPerConnection(anycastLinksPerConnection);
                clients.add(clientAnycasts);
                SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, clientAnycasts);
                int newAnycastConnections = (anycasts.size()/anycastLinksPerConnection) * 2; // *2 because of sender and receiver
                totalConnections += newAnycastConnections;

                var clientQueues = clientProvider.get();
                clientQueues.setAddressesType(AddressType.QUEUE);
                clientQueues.setAddresses(queues.stream().map(a -> a.getSpec().getAddress()).toArray(String[]::new));
                clientQueues.setLinksPerConnection(queueLinksPerConnection);
                clients.add(clientQueues);
                SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, clientQueues);
                int newQueuesConnections = (queues.size()/queueLinksPerConnection) * 2; // *2 because of sender and receiver
                totalConnections += newQueuesConnections;
            }

            long sleepMs = 4 * totalConnections;

            LOGGER.info("#######################################");
            LOGGER.info("Created total {} connections, waiting {} s for system to react", totalConnections, sleepMs/1000);
            LOGGER.info("#######################################");

            Thread.sleep(sleepMs);

            //check system status
            for (ScaleTestClientConfiguration<MessagingClientMetricsClient> client : clients) {
                MessagingClientMetricsClient metricsClient;
                if (client.getMetricsClient() == null) {
                    var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, client.getClientId());
                    client.setMetricsClient(new MessagingClientMetricsClient(metricsEndpoint));
                }
                metricsClient = client.getMetricsClient();

                TestUtils.waitUntilConditionOrFail(() -> {
                    var counter = metricsClient.getAcceptedDeliveries(client.getAddressesType());
                    return counter.isPresent() && counter.get().getValue() >= 0;
                }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting accepted deliveries");

                assertTrue(metricsClient.getConnectFailure().getValue() == 0);
                assertTrue(metricsClient.getConnectSuccess().getValue() > 0);
                assertTrue(metricsClient.getDisconnects().getValue() == 0);

                metricAssertTrue(metricsClient.getRejectedDeliveries(client.getAddressesType()), c -> c == null || c.getValue() == 0);
                metricAssertTrue(metricsClient.getAcceptedDeliveries(client.getAddressesType()), c -> c != null && c.getValue() >= 0);
                metricAssertTrue(metricsClient.getReceivedDeliveries(client.getAddressesType()), c -> c != null && c.getValue() >= 0);
            }

            //increase load
//            linksPerConnection = linksPerConnection * 2;
//            addressesPerGroup = addressesPerGroup * 2;
            anycastLinksPerConnection = anycastLinksPerConnection + 1;
            if (addressesPerGroup%400 == 0) {
                queueLinksPerConnection = queueLinksPerConnection + 1;
            }
            addressesPerGroup = addressesPerGroup + 200;

            LOGGER.info("#######################################");
            LOGGER.info("Increasing load, creating groups of {} addresses, anycast addresses links per connection {} , queues links per connection {}", addressesPerGroup, anycastLinksPerConnection, queueLinksPerConnection);
            LOGGER.info("#######################################");

        }

        } finally {
          LOGGER.info("#######################################");
          LOGGER.info("Total connections created {}", totalConnections);
          LOGGER.info("Final addresses per group {}", addressesPerGroup);
          LOGGER.info("Final anycast links per connection {}", anycastLinksPerConnection);
          LOGGER.info("Final queue links per connection {}", queueLinksPerConnection);
          LOGGER.info("#######################################");
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
    }

    List<Address> createInitialAddresses(int totalAddresses) throws Exception {
        if (totalAddresses % 5 != 0) {
            throw new IllegalArgumentException("Addresses must by multiple of 5");
        }
        int addresses = 0;
        int iterator = 0;

        while (addresses < totalAddresses) {
            try {
                getResourceManager().appendAddresses(false, getTenantAddressBatch(addressSpace).toArray(new Address[0]));
                addresses += 5;
                if (iterator % 200 == 0) {
                    List<Address> currentAddresses = kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
                    AddressUtils.waitForDestinationsReady(currentAddresses.toArray(new Address[0]));
                }
            } catch (IllegalStateException ex) {
                LOGGER.error("Failed to wait for addresses");
                int operableAddresses = (int) kubernetes.getAddressClient().inNamespace(namespace).list().getItems().stream()
                        .filter(address -> address.getStatus().getPhase().equals(Phase.Active)).count();
                LOGGER.info("----------------------------------------------------------");
                LOGGER.info("Total operable addresses {}", operableAddresses);
                LOGGER.info("----------------------------------------------------------");
                if (operableAddresses >= totalAddresses) {
                    break;
                }
            }
            iterator++;
        }
        List<Address> currentAddresses = kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
        AddressUtils.waitForDestinationsReady(currentAddresses.toArray(new Address[0]));
        return currentAddresses;
    }

    private void metricAssertTrue(Optional<Counter> counter, Predicate<Counter> predicate) {
        assertTrue(predicate.test(counter.orElse(null)));
    }

}

