/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import static io.enmasse.systemtest.TestTag.SCALE;
import static io.enmasse.systemtest.utils.AssertionPredicate.isNotPresent;
import static io.enmasse.systemtest.utils.AssertionPredicate.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import io.enmasse.systemtest.scale.downtime.DowntimeData;
import io.enmasse.systemtest.scale.metrics.MessagingClientMetricsClient;
import io.enmasse.systemtest.scale.metrics.ProbeClientMetricsClient;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;

@Tag(SCALE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScaleTest extends TestBase implements ITestBaseIsolated {
    private final static Logger LOGGER = CustomLogger.getLogger();

    private final ScaleTestEnvironment env = ScaleTestEnvironment.getInstance();

    private final int addressesPerTenant = env.getAddressesPerTenant();

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
        AddressUtils.disableVerboseLogs();
        getResourceManager().disableVerboseLogging();
        logCollector.disableVerboseLogging();
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
    @Order(1)
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

            TestUtils.waitUntilConditionOrFail(() -> probeClientMetrics.getSuccessTotal().getValue() > 0, Duration.ofSeconds(25), Duration.ofSeconds(5), () -> "Client is not reporting successfull connections");

            assertTrue(probeClientMetrics.getSuccessTotal().getValue() > 0);
            assertEquals(0, probeClientMetrics.getFailureTotal().getValue());

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
            assertTrue(msgClientMetrics.getConnectSuccess().getValue() > 0);
            assertEquals(0, msgClientMetrics.getConnectFailure().getValue());

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
    @Order(2)
    void testMessagingPerformance() throws Exception {
        final int initialAddresses = env.getPerfInitialAddresses();
        final int addressesPerGroupIncrease = env.getPerfAddressesPerGroupIncrease();
        final int anycastLinksPerConnIncrease = env.getPerfAnycastLinksPerConnIncrease();
        final int queueLinksPerConnIncrease = env.getPerfQueueLinksPerConnIncrease();

        int anycastLinksPerConnection = env.getPerfInitialAnycastLinksPerConn();
        int queueLinksPerConnection = env.getPerfInitialQueueLinksPerConn();
        int addressesPerGroup = env.getPerfInitialAddressesPerGroup();

        var addresses = createInitialAddresses(initialAddresses);

        var endpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        ScaleTestManager manager = new ScaleTestManager(endpoint, userCredentials);

        manager.getPerformanceResults().setTotalAddressesCreated(addresses.size());

        try {

            Executors.newSingleThreadExecutor().execute(manager::monitorMetrics);

            while (true) {
                //determine load
                List<List<Address>> addressesGroups = new ArrayList<>();
                for (int i = 0; i < addresses.size() / addressesPerGroup; i++) {
                    addressesGroups.add(addresses.subList(i * addressesPerGroup, (i + 1) * addressesPerGroup));
                }

                //deploy clients and start messaging
                for (var groupOfAddresses : addressesGroups) {
                    checkMetrics(manager.getMonitoringResult());
                    var config = new ScaleTestClientConfiguration();
                    config.setAddressesPerTenant(addressesPerTenant);
                    config.setSendMessagePeriod(env.getPerfSendMessagesPeriod());
                    config.setReceiversPerTenant(env.getPerfReceiversPerTenant());
                    config.setSendersPerTenant(env.getPerfSendersPerTenant());
                    manager.deployTenantClient(groupOfAddresses, config);
                    checkMetrics(manager.getMonitoringResult());
                }

                manager.sleep();

                checkMetrics(manager.getMonitoringResult());

                //increase load
                if (anycastLinksPerConnection % 2 == 0) {
                    queueLinksPerConnection += queueLinksPerConnIncrease;
                }
                anycastLinksPerConnection += anycastLinksPerConnIncrease;

                int tenantsPerGroup = addressesPerGroup / addressesPerTenant;
                //because of messaging-client grouping algorithm linksPerConnection cannot be bigger than addresses passed to client
                //and because one tenant has one queue, in every address group we will have the same number of tenants and queues
                //this limit is likely to always apply before to queues than anycast addresses, because anycast addresses are 4 and queues only 1
                if (queueLinksPerConnection > tenantsPerGroup) {
                    addressesPerGroup += addressesPerGroupIncrease;
                }
                LOGGER.info("#######################################");
                LOGGER.info("Increasing load, creating groups of {} addresses, anycast addresses links per connection {}"
                        + ", queues links per connection {}", addressesPerGroup, anycastLinksPerConnection, queueLinksPerConnection);
                LOGGER.info("#######################################");
            }

        } finally {
            LOGGER.info("waiting for gathering of last metris");
            Thread.sleep(env.getMetricsUpdatePeriodMillis() + 1000);
            manager.stopMonitoring();
            manager.gatherPerformanceResults();
            saveResultsFile("messaging_performance_results.json", manager.getPerformanceResults());
            Map<String, Object> data = new HashMap<>();
            data.put("addresses", addresses.size());
            data.put("connections", manager.getConnections());
            data.put("anycastLinksPerConnection", anycastLinksPerConnection);
            data.put("queueLinksPerConnection", queueLinksPerConnection);
            data.put("anycastSenderMedianThroughput", manager.getPerformanceResults().getAddresses().get(AddressType.ANYCAST.toString()).getGlobalSenders().getEstimateTotalThroughputMedian());
            data.put("anycastReceiverMedianThroughput", manager.getPerformanceResults().getAddresses().get(AddressType.ANYCAST.toString()).getGlobalReceivers().getEstimateTotalThroughputMedian());
            data.put("queueSenderMedianThroughput", manager.getPerformanceResults().getAddresses().get(AddressType.QUEUE.toString()).getGlobalSenders().getEstimateTotalThroughputMedian());
            data.put("queueReceiverMedianThroughput", manager.getPerformanceResults().getAddresses().get(AddressType.QUEUE.toString()).getGlobalReceivers().getEstimateTotalThroughputMedian());
            savePlotCSV("messaging_performance.csv", data);
            LOGGER.info("#######################################");
            LOGGER.info("Total addresses created {}", addresses.size());
            LOGGER.info("Total connections created {}", manager.getConnections());
            LOGGER.info("Total clients deployed {}", manager.getClients());
            LOGGER.info("Final addresses per group {}", addressesPerGroup);
            LOGGER.info("Final anycast links per connection {}", anycastLinksPerConnection);
            LOGGER.info("Final queue links per connection {}", queueLinksPerConnection);
            LOGGER.info("#######################################");
        }
    }

    @Test
    @Order(3)
    void testNumberOfSupportedAddresses() throws Exception {
        int operableAddresses;
        int iterator = 0;
        int failureThreshold = env.getScaleAddressesFailureThreshold();
        List<Address> addressBatch = new LinkedList<>();
        var endpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        ScaleTestManager manager = new ScaleTestManager(endpoint, userCredentials);

        Executors.newSingleThreadExecutor().execute(manager::monitorMetrics);
        while (true) {
            try {
                List<Address> addr = getTenantAddressBatch(addressSpace);
                addressBatch.addAll(addr);
                getResourceManager().appendAddresses(false, addr.toArray(new Address[0]));
                if (iterator % 200 == 0) {
                    List<Address> currentAddresses = kubernetes.getAddressClient().inNamespace(namespace).list().getItems();
                    AddressUtils.waitForDestinationsReady(new TimeoutBudget(15, TimeUnit.MINUTES), currentAddresses.toArray(new Address[0]));

                    manager.deployTenantClient(addressBatch, addressesPerTenant, env.getScaleSendMessagesPeriod());
                    addressBatch.clear();
                    manager.sleep();
                    TestUtils.listRouterPods(kubernetes, addressSpace).forEach(
                            pod -> assertEquals("Running", pod.getStatus().getPhase())
                    );
                }
                checkMetrics(manager.getMonitoringResult());
            } catch (Exception | AssertionError ex) {
                manager.stopMonitoring();
                LOGGER.error("Failed to wait for addresses/connections");
                LOGGER.error(ex.getMessage());
                operableAddresses = (int) kubernetes.getAddressClient().inNamespace(namespace).list().getItems().stream()
                        .filter(address -> address.getStatus().getPhase().equals(Phase.Active)).count();
                LOGGER.info("#######################################");
                LOGGER.info("Total addresses created {}", operableAddresses);
                LOGGER.info("Total connections created {}", manager.getConnections());
                LOGGER.info("#######################################");
                break;
            }
            iterator++;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("operable_addresses", operableAddresses);
        data.put("connection_count", manager.getConnections());
        data.put("links", manager.getConnections() * 5);
        saveResultsFile("operable_addresses.json", data);
        savePlotCSV("operable-addresses.csv", data);
        assertThat(operableAddresses, greaterThan(failureThreshold));
    }

    @Test
    @Order(4)
    public void testFailureRecovery() throws Exception {
        int addressesToCreate = env.getFaultToleranceInitialAddresses();
        int addressesPerGroup = env.getFaultToleranceAddressesPerGroup();

        //create addresses
        var addresses = createInitialAddresses(addressesToCreate);

        var endpoint = AddressSpaceUtils.getMessagingRoute(addressSpace);
        ScaleTestManager manager = new ScaleTestManager(endpoint, userCredentials);

        manager.getDowntimeResult().setAddresses(addresses.size());

        //measure time to create address in normal conditions
        Duration normalTimeToAddAddress = addAddressAndMeasure();
        manager.getDowntimeResult().setNormalTimeToCreateAddress(normalTimeToAddAddress.toSeconds() + "s");

        //deploy clients and start messaging
        List<List<Address>> addressesGroups = new ArrayList<>();
        for (int i = 0; i < addresses.size() / addressesPerGroup; i++) {
            addressesGroups.add(addresses.subList(i * addressesPerGroup, (i + 1) * addressesPerGroup));
        }

        //deploy clients and start messaging
        for (var groupOfAddresses : addressesGroups) {
            manager.deployTenantClient(groupOfAddresses, addressesPerTenant, env.getFaultToleranceSendMessagesPeriod());
        }
        manager.getDowntimeResult().setClientsDeployed(addressesGroups.size());

        manager.sleep();

        try {

            //iterate over pods
            List<Pod> enmassePods = kubernetes.listPods();
            int runningPodsBefore = enmassePods.size();

            for (var pod : enmassePods) {
                kubernetes.deletePod(kubernetes.getInfraNamespace(), pod.getMetadata().getName());
                Thread.sleep(5_000);

                DowntimeData data = new DowntimeData();
                data.setName(pod.getMetadata().getName());
                manager.getDowntimeResult().getDowntimeData().add(data);
                //time to create address
                var createTime = addAddressAndMeasure();
                data.setCreateAddressTime(createTime.toSeconds() + "s");
                //clients downtime
                manager.measureClientsDowntime(data);

                TestUtils.waitForExpectedReadyPods(kubernetes, kubernetes.getInfraNamespace(), runningPodsBefore, new TimeoutBudget(10, TimeUnit.MINUTES));
            }

        } finally {
            saveResultsFile("failure_recovery_results.json", manager.getDowntimeResult());
            Map<String, Object> data = new HashMap<>();
            data.put("addresses", addresses.size());
            data.put("connections", manager.getConnections());
            data.put("normalTimeToCreateAddress", manager.getDowntimeResult().getNormalTimeToCreateAddress());
            savePlotCSV("failure_recovery.csv", data);
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
                        .withMinReplicas(3)
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
        getResourceManager().createAddressSpacePlan(addressSpacePlan, false);
        Thread.sleep(120_000);

        LOGGER.info("Create custom auth service");
        //Custom auth service
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject(authServiceName, true, "5Gi", "3Gi", true, authServiceName);
        resourcesManager.createAuthService(standardAuth, false);
        setVerboseGCAuthservice(authServiceName);
        Thread.sleep(120_000);
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
        getResourceManager().createOrUpdateUser(addressSpace, userCredentials, false);
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
        currentAddresses.sort((a1, a2) -> {
            var address1 = a1.getSpec().getAddress();
            var address2 = a2.getSpec().getAddress();
            return address1.substring(address1.length() - uuidSize).compareTo(address2.substring(address2.length() - uuidSize));
        });
        return currentAddresses;
    }

    private void setVerboseGCAuthservice(String authservice) {
        Supplier<Deployment> deploymentFinder = () -> kubernetes.getClient().apps()
                .deployments()
                .inNamespace(kubernetes.getInfraNamespace())
                .withName(authservice)
                .get();
        TestUtils.waitUntilConditionOrFail(() -> deploymentFinder.get() != null,
                Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                () -> "scale authservice not created");
        List<EnvVar> envVars = deploymentFinder.get()
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0)
                .getEnv();
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

    private Duration addAddressAndMeasure() throws Exception {
        long start = System.currentTimeMillis();
        var addresses = getTenantAddressBatch(addressSpace).toArray(new Address[0]);
        getResourceManager().appendAddresses(false, addresses);
        AddressUtils.waitForDestinationsReady(addresses);
        long end = System.currentTimeMillis();
        long duration = end - start;
        return Duration.ofMillis(duration);
    }

    private void checkMetrics(AtomicReference<MetricsMonitoringResult> result) {
        if (result.get().isError()) {
            Assertions.fail(result.get().getErrors().toString());
        }
    }

    private void saveResultsFile(String filename, Object resultsObject) throws Exception {
        var logsPath = TestUtils.getScaleTestLogsPath(TestInfo.getInstance().getActualTest()).resolve("result");
        LOGGER.info("Saving results into {}", logsPath);
        var mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
        Files.createDirectories(logsPath);
        Files.write(logsPath.resolve(filename), mapper.writeValueAsBytes(resultsObject));
    }

    private void savePlotCSV(String fileName, Map<String, Object> data) throws IOException {
        var logsPath = environment.testLogDir().resolve("plot-data");
        LOGGER.info("Saving plot data into {}", logsPath);
        Files.createDirectories(logsPath);
        try (BufferedWriter writer = Files.newBufferedWriter(logsPath.resolve(fileName), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write(String.join(",", data.keySet().toArray(new String[0])));
            writer.newLine();
            writer.write(String.join(",", data.values().stream().map(value -> value.toString().replaceAll("\\D+", "")).toArray(String[]::new)));
            writer.newLine();
        }
    }

}

