/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static io.enmasse.systemtest.TestTag.SCALE;
import static io.enmasse.systemtest.scale.metrics.MetricsAssertions.isNotPresent;
import static io.enmasse.systemtest.scale.metrics.MetricsAssertions.isPresent;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import io.enmasse.systemtest.scale.metrics.MetricsValidationResult;
import io.enmasse.systemtest.scale.metrics.ProbeClientMetricsClient;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;

@Tag(SCALE)
class ScaleTest extends TestBase implements ITestBaseIsolated {
    private final static Logger LOGGER = CustomLogger.getLogger();

    //performance constants
    private final int performanceInitialAddresses = 12000;
    private final int addressesPerTenant = 5;
    private final int initialAddressesPerGroup = 200;
    private final int addressesPerGroupIncrease = initialAddressesPerGroup;
    private final int initialAnycastLinksPerConn = 1;
    private final int anycastLinksPerConnIncrease = initialAnycastLinksPerConn;
    private final int initialQueueLinksPerConn = 1;
    private final int queueLinksPerConnIncrease = initialQueueLinksPerConn;

    private final String namespace = "scale-test-namespace";
    private final String addressSpacePlanName = "test-addressspace-plan";
    private final String queuePlanName = "test-queue-plan";
    private final String anycastPlanName = "test-anycast-plan";
    private final String authServiceName = "scale-authservice";
    private AddressSpace addressSpace = null;
    private final UserCredentials userCredentials = new UserCredentials("scale-test-user", "password");
    private final int uuidSize = 36;

    @BeforeAll
    void disableVerboseLogging() {
        TimeMeasuringSystem.disableResultsLogging();
    }

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

            isNotPresent(msgClientMetrics.getRejectedDeliveries(AddressType.QUEUE))
                .or(c -> c.getValue() == 0)
                .assertTrue("There are rejected deliveries");
            isPresent(msgClientMetrics.getAcceptedDeliveries(AddressType.QUEUE))
                .and(c -> c.getValue() >= 0)
                .assertTrue("There are not accepted deliveries");
            isPresent(msgClientMetrics.getReceivedDeliveries(AddressType.QUEUE))
                .or(c -> c.getValue() >= 0)
                .assertTrue("There are not received deliveries");
        }
    }

    @Test
    void testMessagingPerformance() throws Exception {
        int initialAddresses = performanceInitialAddresses;
        var addresses = createInitialAddresses(initialAddresses);

        var endpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        Supplier<ScaleTestClientConfiguration> clientProvider = () -> {
            ScaleTestClientConfiguration client = new ScaleTestClientConfiguration();
            client.setClientType(ScaleTestClientType.messaging);
            client.setHostname(endpoint.getHost());
            client.setPort(endpoint.getPort());
            client.setUsername(userCredentials.getUsername());
            client.setPassword(userCredentials.getPassword());
            return client;
        };

        int totalConnections = 0;

        Map<String, ScaleTestClient<MessagingClientMetricsClient>> clientsMap = new ConcurrentHashMap<>();
        Queue<String> clientsMonitoringQueue = new ConcurrentLinkedQueue<>();


        int anycastLinksPerConnection = initialAnycastLinksPerConn;
        int queueLinksPerConnection = initialQueueLinksPerConn;
        int addressesPerGroup = initialAddressesPerGroup;

        try {

            final var result = new AtomicReference<MetricsValidationResult>(new MetricsValidationResult());

            Executors.newSingleThreadExecutor().execute(()->{
                try {
                    String lastClientId = null;
                    while (true) {
                        String clientId = clientsMonitoringQueue.poll();
                        try {
                            if (clientId != null && !clientId.equals(lastClientId)) {
                                ScaleTestClient<MessagingClientMetricsClient> client = clientsMap.get(clientId);
                                MessagingClientMetricsClient metricsClient = client.getMetricsClient();

                                TestUtils.waitUntilConditionOrFail(() -> {
                                    var counter = metricsClient.getAcceptedDeliveries(client.getAddressesType());
                                    return counter.isPresent() && counter.get().getValue() >= 0;
                                }, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting accepted deliveries");

                                assertTrue(metricsClient.getConnectFailure().getValue() == 0, "There are connection failures");
                                assertTrue(metricsClient.getConnectSuccess().getValue() > 0, "There are not successfull connections");
                                assertTrue(metricsClient.getDisconnects().getValue() == 0, "There are disconnections");

                                isNotPresent(metricsClient.getRejectedDeliveries(client.getAddressesType()))
                                    .or(c -> c.getValue() == 0)
                                    .assertTrue("There are rejected deliveries");
                                isPresent(metricsClient.getAcceptedDeliveries(client.getAddressesType()))
                                    .and(c -> c.getValue() >= 0)
                                    .assertTrue("There are not accepted deliveries");
                                isPresent(metricsClient.getReceivedDeliveries(client.getAddressesType()))
                                    .or(c -> c.getValue() >= 0)
                                    .assertTrue("There are not received deliveries");

                                clientsMonitoringQueue.offer(clientId);

                                lastClientId = clientId;
                            } else {
                                Thread.sleep(1000L);
                                lastClientId = null;
                                if (clientId != null) {
                                    clientsMonitoringQueue.offer(clientId);
                                }
                            }
                        } catch (AssertionError e) {
                            //TODO use listener pattern
                            result.getAndUpdate(r -> r.addError("Error in client " + clientId + " " + e.getMessage()));
                            return;
                        }
                    }
                } catch (Exception e) {
                    log.error("Unexpected error in metrics checker", e);
                    result.getAndUpdate(r -> r.addError(e.getMessage()));
                    return;
                }
            });

            while (true) {
                //determine load
                List<List<Address>> addressesGroups = new ArrayList<>();
                for (int i = 0; i < addresses.size() / addressesPerGroup; i++) {
                    addressesGroups.add(addresses.subList(i * addressesPerGroup, (i + 1) * addressesPerGroup));
                }

                //deploy clients and start messaging
                for (var group : addressesGroups) {
                    checkMetrics(result);

                    var anycasts = group.stream()
                            .filter(a -> a.getSpec().getType().equals(AddressType.ANYCAST.toString()))
                            .collect(Collectors.toList());
                    var queues = group.stream()
                            .filter(a -> a.getSpec().getType().equals(AddressType.QUEUE.toString()))
                            .collect(Collectors.toList());

                    var configAnycasts = clientProvider.get();
                    configAnycasts.setAddressesType(AddressType.ANYCAST);
                    configAnycasts.setAddresses(anycasts.stream().map(a -> a.getSpec().getAddress()).toArray(String[]::new));
                    configAnycasts.setLinksPerConnection(anycastLinksPerConnection);
                    SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, configAnycasts);
                    var metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, configAnycasts.getClientId());
                    var clientAnycast = ScaleTestClient.from(configAnycasts, new MessagingClientMetricsClient(metricsEndpoint));
                    String anycastClientId = clientAnycast.getConfiguration().getClientId();
                    clientsMap.put(anycastClientId, clientAnycast);
                    clientsMonitoringQueue.offer(anycastClientId);
                    int newAnycastConnections = (anycasts.size()/anycastLinksPerConnection) * 2; // *2 because of sender and receiver
                    totalConnections += newAnycastConnections;

                    checkMetrics(result);

                    var configQueues = clientProvider.get();
                    configQueues.setAddressesType(AddressType.QUEUE);
                    configQueues.setAddresses(queues.stream().map(a -> a.getSpec().getAddress()).toArray(String[]::new));
                    configQueues.setLinksPerConnection(queueLinksPerConnection);
                    SystemtestsKubernetesApps.deployScaleTestClient(kubernetes, configQueues);
                    metricsEndpoint = SystemtestsKubernetesApps.getScaleTestClientEndpoint(kubernetes, configQueues.getClientId());
                    var clientQueues = ScaleTestClient.from(configQueues, new MessagingClientMetricsClient(metricsEndpoint));
                    String queueClientId = clientQueues.getConfiguration().getClientId();
                    clientsMap.put(queueClientId, clientQueues);
                    clientsMonitoringQueue.offer(queueClientId);
                    int newQueuesConnections = (queues.size()/queueLinksPerConnection) * 2; // *2 because of sender and receiver
                    totalConnections += newQueuesConnections;
                }

                checkMetrics(result);

                long sleepMs = 4 * totalConnections;

                LOGGER.info("#######################################");
                LOGGER.info("Created total {} connections with {} deployed clients, waiting {} s for system to react",
                        totalConnections, clientsMap.size(), sleepMs/1000);
                LOGGER.info("#######################################");

                Thread.sleep(sleepMs);

                checkMetrics(result);

                //increase load
    //          if (addressesPerGroup%400 == 0) {
                if (anycastLinksPerConnection%2 == 0) {
                    queueLinksPerConnection += queueLinksPerConnIncrease;
                }
                anycastLinksPerConnection += anycastLinksPerConnIncrease;
    //            addressesPerGroup = addressesPerGroup + 200;


                int tenantsPerGroup = addressesPerGroup / addressesPerTenant;
                if (queueLinksPerConnection > tenantsPerGroup || anycastLinksPerConnection > tenantsPerGroup) {
                    addressesPerGroup += addressesPerGroupIncrease;
                }
                LOGGER.info("#######################################");
                LOGGER.info("Increasing load, creating groups of {} addresses, anycast addresses links per connection {}"
                        + ", queues links per connection {}", addressesPerGroup, anycastLinksPerConnection, queueLinksPerConnection);
                LOGGER.info("#######################################");

            }

        } finally {
          LOGGER.info("#######################################");
          LOGGER.info("Total addresses created {}", addresses.size());
          LOGGER.info("Total connections created {}", totalConnections);
          LOGGER.info("Total clients deployed {}", clientsMap.size());
          LOGGER.info("Final addresses per group {}", addressesPerGroup);
          LOGGER.info("Final anycast links per connection {}", anycastLinksPerConnection);
          LOGGER.info("Final queue links per connection {}", queueLinksPerConnection);
          LOGGER.info("#######################################");
        }
    }

    private void checkMetrics(AtomicReference<MetricsValidationResult> result) {
        if (result.get().isError()) {
            Assertions.fail(result.get().getErrors().toString());
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    // Help methods
    ///////////////////////////////////////////////////////////////////////////////////
    private List<Address> getTenantAddressBatch(AddressSpace addressSpace) {
        List<Address> addresses = new ArrayList<>(addressesPerTenant - 1);
        String batchSuffix = UUID.randomUUID().toString();
        IntStream.range(0, addressesPerTenant - 1).forEach(i -> addresses.add(new AddressBuilder()
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
        resourcesManager.createAuthService(standardAuth, false);
        setVerboseGCAuthservice(authServiceName);
        resourcesManager.waitForAuthPods(standardAuth);

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
        if (totalAddresses % addressesPerTenant != 0) {
            throw new IllegalArgumentException("Addresses must be multiple of " + addressesPerTenant);
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
        Collections.sort(currentAddresses, (a1, a2) -> {
            var address1 = a1.getSpec().getAddress();
            var address2 = a2.getSpec().getAddress();
            return address1.substring(address1.length()-uuidSize).compareTo(address2.substring(address2.length()-uuidSize));
        });
        return currentAddresses;
    }

//  oc set-env deployment <deploymentname> _JAVA_OPTIONS="-verbose:gc"
    private void setVerboseGCAuthservice(String authservice) {
        List<EnvVar> envVars = kubernetes.getClient().apps()
                .deployments()
                .inNamespace(kubernetes.getInfraNamespace())
                .withName(authservice)
                .get().getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        List<EnvVar> updatedEnvVars = new ArrayList<EnvVar>(envVars);
        updatedEnvVars.add(new EnvVarBuilder().withName("_JAVA_OPTIONS").withValue("-verbose:gc").build());

        kubernetes.getClient().apps()
                .deployments()
                .inNamespace(kubernetes.getInfraNamespace())
                .withName(authservice)
                .edit()
                .editSpec()
                .editTemplate()
                .editSpec()
                .editFirstContainer()
                .withEnv(updatedEnvVars)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .done();
    }

}

