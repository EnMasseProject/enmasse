/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.utils;

import com.google.common.collect.Ordering;
import com.google.common.io.BaseEncoding;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.broker.BrokerManagement;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.manager.SharedResourceManager;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.time.WaitPhase;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.VertxException;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The type Test utils.
 */

public class TestUtils {

    /**
     * The interface Timeout handler.
     *
     * @param <X> the type parameter
     */
    public interface TimeoutHandler<X extends Throwable> {
        /**
         * Timeout.
         *
         * @throws X the x
         */
        void timeout() throws X;
    }

    private static final Random R = new Random();
    private static Logger LOGGER = CustomLogger.getLogger();
    private static final Kubernetes KUBERNETES = Kubernetes.getInstance();

    /**
     * Wait for n replicas.
     *
     * @param expectedReplicas the expected replicas
     * @param labelSelector    the label selector
     * @param budget           the budget
     * @throws InterruptedException the interrupted exception
     */
    public static void waitForNReplicas(int expectedReplicas,Map<String, String> labelSelector,
                                        TimeoutBudget budget) throws InterruptedException {
        waitForNReplicas(expectedReplicas, labelSelector, Collections.emptyMap(), budget);
    }

    /**
     * wait for expected count of Destination replicas in address space
     *
     * @param addressSpace     the address space
     * @param expectedReplicas the expected replicas
     * @param readyRequired    the ready required
     * @param destination      the destination
     * @param budget           the budget
     * @param checkInterval    the check interval
     * @throws Exception the exception
     */
    public static void waitForNBrokerReplicas(AddressSpace addressSpace, int expectedReplicas, boolean readyRequired,
                                              Address destination, TimeoutBudget budget, long checkInterval) throws Exception {
        Address address = Kubernetes.getInstance().getAddressClient(
                addressSpace.getMetadata().getNamespace()).withName(destination.getMetadata().getName()).get();
        Map<String, String> labels = new HashMap<>();
        labels.put("role", "broker");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));

        for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
            if (brokerStatus.getState().equals(BrokerState.Active)) {
                waitForNReplicas(
                        expectedReplicas,
                        readyRequired,
                        labels,
                        Collections.singletonMap("cluster_id", brokerStatus.getClusterId()),
                        budget,
                        checkInterval);
            }
        }
    }

    /**
     * Wait for n broker replicas.
     *
     * @param addressSpace     the address space
     * @param expectedReplicas the expected replicas
     * @param destination      the destination
     * @param budget           the budget
     * @throws Exception the exception
     */
    public static void waitForNBrokerReplicas(AddressSpace addressSpace, int expectedReplicas,
                                              Address destination, TimeoutBudget budget) throws Exception {
        waitForNBrokerReplicas(addressSpace, expectedReplicas, true, destination, budget, 5000);
    }


    /**
     * Wait for expected count of replicas
     *
     * @param expectedReplicas   count of expected replicas
     * @param readyRequired      the ready required
     * @param labelSelector      labels on scaled pod
     * @param annotationSelector annotations on sclaed pod
     * @param budget             timeout budget - throws Exception when timeout is reached
     * @param checkInterval      the check interval
     * @throws InterruptedException the interrupted exception
     */
    public static void waitForNReplicas(int expectedReplicas, boolean readyRequired,
                                        Map<String, String> labelSelector, Map<String, String> annotationSelector,
                                        TimeoutBudget budget, long checkInterval) throws InterruptedException {
        int actualReplicas;
        do {
            final List<Pod> pods;
            if (annotationSelector.isEmpty()) {
                pods = Kubernetes.getInstance().listPods(labelSelector);
            } else {
                pods = Kubernetes.getInstance().listPods(labelSelector, annotationSelector);
            }
            if (!readyRequired) {
                actualReplicas = pods.size();
            } else {
                actualReplicas = numReady(pods);
            }
            LOGGER.info("Have {} out of {} replicas. Expecting={}, ReadyRequired={}", actualReplicas, pods.size(), expectedReplicas, readyRequired);

            if (budget.timeoutExpired()) {
                // our time budged expired ... throw exception
                String message = format("Only '%s' out of '%s' in state 'Running' before timeout %s", actualReplicas, expectedReplicas,
                        pods.stream().map(pod -> pod.getMetadata().getName()).collect(Collectors.joining(",")));
                throw new RuntimeException(message);
            }
            // try again next cycle
            Thread.sleep(checkInterval);
        } while (actualReplicas != expectedReplicas);
        // finished successfully
    }

    private static void waitForNReplicas(int expectedReplicas, Map<String, String> labelSelector,
                                         Map<String, String> annotationSelector, TimeoutBudget budget) throws InterruptedException {
        waitForNReplicas(expectedReplicas, true, labelSelector, annotationSelector, budget, 5000);

    }

    /**
     * Wait for subscribers console.
     *
     * @param addressSpace the address space
     * @param destination  the destination
     * @throws Exception the exception
     */
    public static void waitForSubscribersConsole(AddressSpace addressSpace, Address destination) throws Exception {
        int budget = 60; //seconds
        waitForSubscribersConsole(addressSpace, destination, budget);
    }

    /**
     * wait for expected count of subscribers on topic (check via console)
     *
     * @param budget timeout budget in seconds
     */
    private static void waitForSubscribersConsole(AddressSpace addressSpace, Address destination, int budget) throws Exception {
        final UserCredentials clusterUser = new UserCredentials(KubeCMDClient.getOCUser());

        SeleniumProvider selenium = null;
        try {
            SeleniumManagement.deployFirefoxApp();
            selenium = ConsoleUtils.getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(selenium, KUBERNETES.getConsoleRoute(addressSpace), addressSpace, clusterUser);
            console.openWebConsolePage();
            console.openAddressesPageWebConsole();

            selenium.waitUntilPropertyPresent(budget, 2, () -> console.getAddressItem(destination).getReceiversCount());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (selenium != null) {
                selenium.tearDownDrivers();
            }
            SeleniumManagement.removeFirefoxApp();
        }
    }

    /**
     * wait for expected count of subscribers on topic
     *
     * @param brokerManagement the broker management
     * @param addressSpace     the address space
     * @param topic            name of topic
     * @throws Exception the exception
     */
    public static void waitForSubscribers(BrokerManagement brokerManagement, AddressSpace addressSpace, String topic, String plan) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        waitForSubscribers(brokerManagement, addressSpace, topic, budget, plan);
    }

    private static void waitForSubscribers(BrokerManagement brokerManagement, AddressSpace addressSpace, String topic,
                                           TimeoutBudget budget, String plan) throws Exception {
        ResourceManager resourcesManager = (addressSpace.getMetadata().getName().contains("shared") ? SharedResourceManager.getInstance()
                : IsolatedResourcesManager.getInstance());
        AmqpClient queueClient = null;
        try {
            queueClient = resourcesManager.getAmqpClientFactory().createQueueClient(addressSpace);
            queueClient.setConnectOptions(queueClient.getConnectOptions().setCredentials(Environment.getInstance().getSharedManagementCredentials()));
            String replyQueueName = "reply-queue";
            Address replyQueue = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, replyQueueName))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress(replyQueueName)
                    .withPlan(plan)
                    .endSpec()
                    .build();
            resourcesManager.appendAddresses(replyQueue);

            boolean done = false;
            int actualSubscribers;
            do {
                actualSubscribers = MessagingUtils.getSubscriberCount(brokerManagement, queueClient, replyQueue, topic);
                LOGGER.info("Have " + actualSubscribers + " subscribers. Expecting " + 2);
                if (actualSubscribers != 2) {
                    Thread.sleep(1000);
                } else {
                    done = true;
                }
            } while (budget.timeLeft() >= 0 && !done);
            if (!done) {
                throw new RuntimeException("Only " + actualSubscribers + " out of " + 2 + " subscribed before timeout");
            }
        } finally {
            Objects.requireNonNull(queueClient).close();
        }
    }

    private static void waitForBrokerReplicas(AddressSpace addressSpace, Address destination,
                                       int expectedReplicas, TimeoutBudget budget) throws Exception {
        TestUtils.waitForNBrokerReplicas(addressSpace, expectedReplicas, true, destination, budget, 5000);
    }

    /**
     * Wait for broker replicas.
     *
     * @param addressSpace     the address space
     * @param destination      the destination
     * @param expectedReplicas the expected replicas
     * @throws Exception the exception
     */
    public static void waitForBrokerReplicas(AddressSpace addressSpace, Address destination, int expectedReplicas) throws
            Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        waitForBrokerReplicas(addressSpace, destination, expectedReplicas, budget);
    }

    /**
     * Wait for router replicas.
     *
     * @param addressSpace     the address space
     * @param expectedReplicas the expected replicas
     * @throws Exception the exception
     */
    public static void waitForRouterReplicas(AddressSpace addressSpace, int expectedReplicas) throws
            Exception {
        TimeoutBudget budget = new TimeoutBudget(3, TimeUnit.MINUTES);
        Map<String, String> labels = new HashMap<>();
        labels.put("name", "qdrouterd");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        TestUtils.waitForNReplicas(expectedReplicas, labels, budget);
    }

    /**
     * Wait for pods to terminate.
     *
     * @param uids the uids
     * @throws Exception the exception
     */
    public static void waitForPodsToTerminate(List<String> uids) throws Exception {
        LOGGER.info("Waiting for following pods to be deleted {}", uids);
        TestUtils.assertWaitForValue(true, () -> (KUBERNETES.listPods(KUBERNETES.getInfraNamespace()).stream()
                .noneMatch(pod -> uids.contains(pod.getMetadata().getUid()))), new TimeoutBudget(2, TimeUnit.MINUTES));
    }

    /**
     * Wait for destinations are in isReady=true state within default timeout (10 MINUTE)
     *
     * @param destinations the destinations
     */
    protected void waitForDestinationsReady(Address... destinations) {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        AddressUtils.waitForDestinationsReady(budget, destinations);
    }




    /**
     * Check ready status of all pods in list
     *
     * @param pods list of pods
     * @return pod count
     */
    private static int numReady(List<Pod> pods) {
        return (int) pods.stream().filter(pod -> isPodReady(pod, true)).count();
    }

    /**
     * Is pod ready boolean.
     *
     * @param pod   the pod
     * @param doLog the do log
     * @return the boolean
     */
    public static boolean isPodReady(final Pod pod, final boolean doLog) {

        if (!"Running".equals(pod.getStatus().getPhase())) {
            if (doLog) {
                LOGGER.info("POD {} in status : {}", pod.getMetadata().getName(), pod.getStatus().getPhase());
            }
            return false;
        }

        var nonReadyContainers = pod.getStatus().getContainerStatuses().stream()
                .filter(cs -> !Boolean.TRUE.equals(cs.getReady()))
                .map(ContainerStatus::getName)
                .collect(Collectors.toList());

        if (!nonReadyContainers.isEmpty()) {
            if (doLog) {
                LOGGER.info("POD {} non-ready containers: [{}]", pod.getMetadata().getName(),
                        String.join(", ", nonReadyContainers));
            }
            return false;
        }

        return true;
    }

    /**
     * Wait for expected count of pods within AddressSpace
     *
     * @param client      client for manipulation with kubernetes cluster
     * @param namespace   the namespace
     * @param numExpected count of expected pods
     * @param budget      timeout budget - this method throws Exception when timeout is reached
     * @throws InterruptedException the interrupted exception
     */
    public static void waitForExpectedReadyPods(Kubernetes client, String namespace, int numExpected,
                                                TimeoutBudget budget) throws InterruptedException {
        boolean shouldRetry;
        do {
            LOGGER.info("Waiting for expected ready pods: {}", numExpected);
            shouldRetry = false;
            List<Pod> pods = listReadyPods(client, namespace);
            while (budget.timeLeft() >= 0 && pods.size() != numExpected) {
                Thread.sleep(2000);
                pods = listReadyPods(client, namespace);
                LOGGER.info("Got {} pods, expected: {}", pods.size(), numExpected);
            }
            if (pods.size() != numExpected) {
                throw new IllegalStateException("Unable to find " + numExpected + " pods. Found : " + printPods(pods));
            }
            for (Pod pod : pods) {
                try {
                    client.waitUntilPodIsReady(pod);
                } catch (NullPointerException | IllegalArgumentException e) {
                    // TODO: remove NPE guard once upgrade to Fabric8 kubernetes-client 4.6.0 or beyond is complete.
                    // (kubernetes-client 450b94745b68403293a55956be2aa7ec483c0a6c)
                    LOGGER.warn("Failed to await pod {} {}", pod, e);
                    shouldRetry = true;
                    break;
                }
            }
        } while (shouldRetry);
    }

    /**
     * Print name of all pods in list
     *
     * @param pods list of pods that should be printed
     * @return Formatted string of pods
     */
    private static String printPods(List<Pod> pods) {
        return pods.stream()
                .map(pod -> "{" + pod.getMetadata().getName() + ", " + pod.getStatus().getPhase() + "}")
                .collect(Collectors.joining(","));
    }

    /**
     * Get list of all running pods from specific AddressSpace
     *
     * @param kubernetes   client for manipulation with kubernetes cluster
     * @param addressSpace the address space
     * @return list
     */
    public static List<Pod> listRunningPods(Kubernetes kubernetes, AddressSpace addressSpace){
        return kubernetes.listPods(Collections.singletonMap("infraUuid",
                AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace))).stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .collect(Collectors.toList());
    }

    /**
     * Get list of all ready pods
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @return list
     */
    public static List<Pod> listReadyPods(Kubernetes kubernetes) {
        return kubernetes.listPods().stream()
                .filter(pod -> pod.getStatus().getContainerStatuses().stream().allMatch(ContainerStatus::getReady))
                .collect(Collectors.toList());
    }

    /**
     * Get list of all ready pods
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @param namespace  the namespace
     * @return list
     */
    public static List<Pod> listReadyPods(Kubernetes kubernetes, String namespace) {
        return kubernetes.listPods(namespace).stream()
                .filter(pod -> pod.getStatus().getContainerStatuses().stream().allMatch(ContainerStatus::getReady))
                .collect(Collectors.toList());
    }

    /**
     * List broker pods list.
     *
     * @param kubernetes   the kubernetes
     * @param addressSpace the address space
     * @return the list
     */
    public static List<Pod> listBrokerPods(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("role", "broker");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPods(labels);
    }

    /**
     * List router pods list.
     *
     * @param kubernetes   the kubernetes
     * @param addressSpace the address space
     * @return the list
     */
    public static List<Pod> listRouterPods(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("capability", "router");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPods(labels);
    }

    /**
     * List admin console pods list.
     *
     * @param kubernetes   the kubernetes
     * @param addressSpace the address space
     * @return the list
     */
    public static List<Pod> listAdminConsolePods(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
            labels.put("name", "admin");
        } else {
            labels.put("name", "agent");
        }
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPods(labels);
    }

    /**
     * List persistent volume claims list.
     *
     * @param kubernetes   the kubernetes
     * @param addressSpace the address space
     * @return the list
     */
    public static List<PersistentVolumeClaim> listPersistentVolumeClaims(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPersistentVolumeClaims(labels);
    }

    /**
     * Generate message body with prefix
     *
     * @param prefix      the prefix
     * @param numMessages the num messages
     * @return the list
     */
    public static List<String> generateMessages(String prefix, int numMessages) {
        return IntStream.range(0, numMessages).mapToObj(i -> prefix + i).collect(Collectors.toList());
    }

    /**
     * Generate message body with "testmessage" content and without prefix
     *
     * @param numMessages the num messages
     * @return the list
     */
    public static List<String> generateMessages(int numMessages) {
        return generateMessages("testmessage", numMessages);
    }

    /**
     * Check if endpoint is accessible
     *
     * @param endpoint the endpoint
     * @return the boolean
     */
    public static boolean resolvable(Endpoint endpoint) {
        return waitUntilCondition(() -> {
            try {
                InetAddress[] addresses = Inet4Address.getAllByName(endpoint.getHost());
                return addresses.length > 0;
            } catch (UnknownHostException ignore) {
            }
            return false;
        }, Duration.ofSeconds(10), Duration.ofSeconds(1));
    }

    /**
     * Wait until Namespace will be removed
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @param namespace  project/namespace to remove
     * @throws Exception the exception
     */
    public static void waitForNamespaceDeleted(Kubernetes kubernetes, String namespace) throws Exception {
        waitUntilCondition(
                () -> !kubernetes.listNamespaces().contains(namespace),
                Duration.ofMinutes(10), Duration.ofSeconds(1),
                () -> {
                    throw new TimeoutException("Timed out waiting for namespace " + namespace + " to disappear");
                });
    }

    /**
     * Repeat request n-times in a row
     *
     * @param <T>       the type parameter
     * @param retry     count of remaining retries
     * @param fn        request function
     * @param reconnect the reconnect
     * @return t
     * @throws Exception the exception
     */
    public static <T> T doRequestNTimes(int retry, Callable<T> fn, Optional<Runnable> reconnect) throws Exception {
        try {
            return fn.call();
        } catch (Exception ex) {
            if (ex.getCause() instanceof VertxException && ex.getCause().getMessage().contains("Connection was closed")) {
                if (reconnect.isPresent()) {
                    LOGGER.warn("connection was closed, trying to reconnect...");
                    reconnect.get().run();
                }
            }
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                LOGGER.info("{} remaining iterations", retry);
                return doRequestNTimes(retry - 1, fn, reconnect);
            } else {
                if (ex.getCause() != null) {
                    ex.getCause().printStackTrace();
                } else {
                    ex.printStackTrace();
                }
                throw ex;
            }
        }
    }

    /**
     * Repeat command n-times
     *
     * @param <T>   the type parameter
     * @param retry count of remaining retries
     * @param fn    request function
     * @return The value from the first successful call to the callable
     * @throws InterruptedException the interrupted exception
     */
    public static <T> T runUntilPass(int retry, Callable<T> fn) throws InterruptedException {
        for (int i = 0; i < retry; i++) {
            try {
                LOGGER.info("Running command, attempt: {}", i);
                return fn.call();
            } catch (Exception ex) {
                LOGGER.info("Command failed", ex);
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException(format("Command wasn't pass in %s attempts", retry));
    }

    /**
     * Repeat command n-times.
     *
     * @param retries  Number of retries.
     * @param callable Code to execute.
     * @throws InterruptedException the interrupted exception
     */
    public static void runUntilPass(int retries, ThrowingCallable callable) throws InterruptedException {
        runUntilPass(retries, () -> {
            callable.call();
            return null;
        });
    }

    /**
     * Delete address space created by sc.
     *
     * @param kubernetes   the kubernetes
     * @param addressSpace the address space
     * @param logCollector the log collector
     * @throws Exception the exception
     */
    public static void deleteAddressSpaceCreatedBySC(Kubernetes kubernetes, AddressSpace addressSpace,
                                                     GlobalLogCollector logCollector) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS_SPACE);
        logCollector.collectEvents();
        logCollector.collectApiServerJmapLog();
        logCollector.collectLogsTerminatedPods();
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddressSpaceCreatedBySC");
        kubernetes.deleteNamespace(addressSpace.getMetadata().getNamespace());
        waitForNamespaceDeleted(kubernetes, addressSpace.getMetadata().getNamespace());
        AddressSpaceUtils.waitForAddressSpaceDeleted(addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    /**
     * Gets firefox driver.
     *
     * @return the firefox driver
     * @throws Exception the exception
     */
    public static RemoteWebDriver getFirefoxDriver() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.getFirefoxSeleniumAppEndpoint(Kubernetes.getInstance());
        FirefoxOptions options = new FirefoxOptions();
        // https://github.com/mozilla/geckodriver/issues/330 enable the emission of console.info(), warn() etc
        // to stdout of the browser process.  Works around the fact that Firefox logs are not available through
        // WebDriver.manage().logs().
        options.addPreference("devtools.console.stdout.content", true);
        return getRemoteDriver(endpoint.getHost(), endpoint.getPort(), options);
    }

    /**
     * Gets chrome driver.
     *
     * @return the chrome driver
     * @throws Exception the exception
     */
    public static RemoteWebDriver getChromeDriver() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.getChromeSeleniumAppEndpoint(Kubernetes.getInstance());
        return getRemoteDriver(endpoint.getHost(), endpoint.getPort(), new ChromeOptions());
    }

    private static RemoteWebDriver getRemoteDriver(String host, int port, Capabilities options) throws Exception {
        int attempts = 60;
        URL hubUrl = new URL(format("http://%s:%s/wd/hub", host, port));
        for (int i = 0; i < attempts; i++) {
            if (isReachable(hubUrl)) {
                return new RemoteWebDriver(hubUrl, options);
            }
            Thread.sleep(2000);
        }
        throw new IllegalStateException("Selenium webdriver cannot connect to selenium container");
    }

    private static boolean isReachable(URL url) {
        LOGGER.info("Trying to connect to {}", url.toString());
        try {
            url.openConnection();
            url.getContent();
            LOGGER.info("Client is able to connect to the selenium hub");
            return true;
        } catch (Exception ex) {
            LOGGER.warn("Cannot connect to hub: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Wait until condition.
     *
     * @param fn       the fn
     * @param expected the expected
     * @param budget   the budget
     * @throws Exception the exception
     */
    public static void waitUntilCondition(Callable<String> fn, String expected, TimeoutBudget budget) throws Exception {
        String actual = "Too small time out budget!!";
        while (!budget.timeoutExpired()) {
            actual = fn.call();
            LOGGER.debug(actual);
            if (actual.contains(expected)) {
                return;
            }
            LOGGER.debug("next iteration, remaining time: {}", budget.timeLeft());
            Thread.sleep(2000);
        }
        throw new IllegalStateException(format("Expected: '%s' in content, but was: '%s'", expected, actual));
    }

    /**
     * Wait until condition.
     *
     * @param forWhat   the for what
     * @param condition the condition
     * @param budget    the budget
     */
    public static void waitUntilCondition(final String forWhat, final Predicate<WaitPhase> condition,
                                          final TimeoutBudget budget) {

        Objects.requireNonNull(condition);
        Objects.requireNonNull(budget);

        LOGGER.info("Waiting {} ms for - {}", budget.timeLeft(), forWhat);

        waitUntilCondition(

                () -> condition.test(WaitPhase.LOOP),
                budget.remaining(), Duration.ofSeconds(5),

                () -> {
                    // try once more
                    if (condition.test(WaitPhase.LAST_TRY)) {
                        LOGGER.info("Successfully wait for: {} , it passed on last try", forWhat);
                        return;
                    }

                    throw new IllegalStateException("Failed to wait for: " + forWhat);
                });

        LOGGER.info("Successfully waited for: {}, it took {} ms", forWhat, budget.timeSpent());

    }

    /**
     * Wait for a condition, fail otherwise.
     *
     * @param condition              The condition to check, returning {@code true} means success.
     * @param timeout                the timeout
     * @param delay                  The delay between checks.
     * @param timeoutMessageSupplier The supplier of a timeout message.
     * @throws AssertionFailedError In case the timeout expired
     */
    public static void waitUntilConditionOrFail(final BooleanSupplier condition,
                                                final Duration timeout, final Duration delay,
                                                final Supplier<String> timeoutMessageSupplier) {

        Objects.requireNonNull(timeoutMessageSupplier);
        waitUntilConditionOrThrow(condition, timeout, delay, () -> new AssertionFailedError(timeoutMessageSupplier.get()));
    }

    /**
     * Wait for a condition, throw exception otherwise.
     *
     * @param <X>               the type parameter
     * @param condition         The condition to check, returning {@code true} means success.
     * @param timeout           the timeout
     * @param delay             The delay between checks.
     * @param exceptionSupplier The supplier of the exception to throw.
     * @throws X x
     */
    private static <X extends Throwable> void waitUntilConditionOrThrow(final BooleanSupplier condition,
                                                                        final Duration timeout, final Duration delay,
                                                                        final Supplier<X> exceptionSupplier) throws X {

        Objects.requireNonNull(exceptionSupplier);

        waitUntilCondition(condition, timeout, delay, () -> {
            throw exceptionSupplier.get();
        });

    }


    /**
     * Wait for a condition, call handler otherwise.
     *
     * @param <X>            The type of exception thrown by the timeout handler.
     * @param condition      The condition to check, returning {@code true} means success.
     * @param timeout        the timeout
     * @param delay          The delay between checks.
     * @param timeoutHandler The handler to call in case of the timeout.
     * @throws X the x
     */
    public static <X extends Throwable> void waitUntilCondition(final BooleanSupplier condition,
                                                                final Duration timeout, final Duration delay,
                                                                final TimeoutHandler<X> timeoutHandler) throws X {

        Objects.requireNonNull(timeoutHandler);

        if (!waitUntilCondition(condition, timeout, delay)) {
            timeoutHandler.timeout();
        }

    }

    /**
     * Wait for condition, return result.
     * <p>
     * This will check will put a priority on checking the condition, and only wait, when there is remaining time budget left.
     *
     * @param condition The condition to check, returning {@code true} means success.
     * @param timeout   the timeout
     * @param delay     The delay between checks.
     * @return {@code true} if the condition was met, {@code false otherwise}.
     */
    public static boolean waitUntilCondition(final BooleanSupplier condition, final Duration timeout, final Duration delay) {

        Objects.requireNonNull(condition);
        Objects.requireNonNull(timeout);
        Objects.requireNonNull(delay);

        final Instant deadline = Instant.now().plus(timeout);

        while (true) {

            // test first

            if (condition.getAsBoolean()) {
                return true;
            }

            // if the timeout has expired ... stop

            final Duration remaining = Duration.between(deadline, Instant.now());
            if (!remaining.isNegative()) {
                return false;
            }

            // otherwise sleep

            LOGGER.debug("next iteration, remaining time: {}", remaining);
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

    }

    /**
     * Wait for changed resource version.
     *
     * @param budget                 the budget
     * @param namespace              the namespace
     * @param name                   the name
     * @param currentResourceVersion the current resource version
     */
    public static void waitForChangedResourceVersion(final TimeoutBudget budget,
                                                     final String namespace, final String name,
                                                     final String currentResourceVersion) {
        waitForChangedResourceVersion(budget, currentResourceVersion, () ->
                Kubernetes.getInstance().getAddressSpaceClient(namespace).withName(name)
                        .get().getMetadata().getResourceVersion());
    }

    private static void waitForChangedResourceVersion(final TimeoutBudget budget,
                                                      final String currentResourceVersion,
                                                      final ThrowingSupplier<String> provideNewResourceVersion) {
        Objects.requireNonNull(currentResourceVersion, "'currentResourceVersion' must not be null");

        waitUntilCondition("Resource version to change away from: " + currentResourceVersion, phase -> {
            try {
                final String newVersion = provideNewResourceVersion.get();
                return !currentResourceVersion.equals(newVersion);
            } catch (RuntimeException e) {
                throw e; // don't pollute the cause chain
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, budget);
    }

    /**
     * Gets global console route.
     *
     * @return the global console route
     */
    public static String getGlobalConsoleRoute() {
        return Kubernetes.getInstance().getConsoleServiceClient().withName("console").get().getStatus().getUrl();
    }

    /**
     * Return a number of random characters in the range of {@code 0-9a-f}.
     *
     * @param length the number of characters to return
     * @return The random string, never {@code null}.
     */
    public static String randomCharacters(int length) {
        var b = new byte[(int) Math.ceil(length / 2.0)];
        R.nextBytes(b);
        return BaseEncoding.base16().encode(b).substring(length % 2);
    }

    /**
     * Wait until deployed.
     *
     * @param namespace the namespace
     */
    public static void waitUntilDeployed(String namespace) {
        TestUtils.waitUntilCondition("All pods and container is ready", waitPhase -> {
            List<Pod> pods = Kubernetes.getInstance().listPods(namespace);
            if (pods.size() > 0) {
                LOGGER.info("-------------------------------------------------------------");
                for (Pod pod : pods) {
                    List<ContainerStatus> initContainers = pod.getStatus().getInitContainerStatuses();
                    for (ContainerStatus s : initContainers) {
                        if (!s.getReady()) {
                            LOGGER.info("Pod {} is in ready state, init container is not in ready state",
                                    pod.getMetadata().getName());
                            return false;
                        }
                    }
                    List<ContainerStatus> containers = pod.getStatus().getContainerStatuses();
                    for (ContainerStatus s : containers) {
                        if (!s.getReady()) {
                            LOGGER.info("Pod {} is in ready state, container {} is not in ready state",
                                    pod.getMetadata().getName(), s.getName());
                            return false;
                        }
                    }
                    LOGGER.info("Pod {} is in ready state", pod.getMetadata().getName());
                }
                return true;
            }
            return false;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    /**
     * Wait for console rolling update.
     *
     * @param namespace the namespace
     */
    public static void waitForConsoleRollingUpdate(String namespace) {
        TestUtils.waitUntilCondition("Wait for console rolling update to complete", waitPhase -> {
            List<Pod> pods = Kubernetes.getInstance().listPods(namespace);
            pods.removeIf(pod -> !pod.getSpec().getContainers().get(0).getName().equals("console-proxy"));
            return pods.size() == 1;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

    /**
     * Clean all enmasse resources from namespace.
     *
     * @param namespace the namespace
     */
    public static void cleanAllEnmasseResourcesFromNamespace(String namespace) {
        Kubernetes kube = Kubernetes.getInstance();
        var brInfraConfigClient = kube.getBrokeredInfraConfigClient(namespace);
        var stInfraConfigClient = kube.getStandardInfraConfigClient(namespace);
        var addressSpaceClient = kube.getAddressSpaceClient(namespace);
        var addressClient = kube.getAddressClient(namespace);
        var addrSpacePlanClient = kube.getAddressSpacePlanClient(namespace);
        var addPlanClient = kube.getAddressPlanClient(namespace);
        var authServiceClient = kube.getAuthenticationServiceClient(namespace);
        var consoleClient = kube.getConsoleServiceClient(namespace);

        brInfraConfigClient.list().getItems().forEach(cr ->
                brInfraConfigClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        stInfraConfigClient.list().getItems().forEach(cr ->
                stInfraConfigClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addressSpaceClient.list().getItems().forEach(cr ->
                addressSpaceClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addressClient.list().getItems().forEach(cr ->
                addressClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addrSpacePlanClient.list().getItems().forEach(cr ->
                addrSpacePlanClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addPlanClient.list().getItems().forEach(cr ->
                addPlanClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        authServiceClient.list().getItems().forEach(cr ->
                authServiceClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        consoleClient.list().getItems().forEach(cr ->
                consoleClient.withName(cr.getMetadata().getName()).cascading(true).delete());
    }

    /**
     * Wait for pod ready.
     *
     * @param name      the name
     * @param namespace the namespace
     */
    public static void waitForPodReady(String name, String namespace) {
        TestUtils.waitUntilCondition(format("Pod is ready %s", name), waitPhase -> {
            try {
                Pod pod = Kubernetes.getInstance().listPods(namespace).stream().filter(
                        p -> p.getMetadata().getName().contains(name)).findFirst().get();
                return TestUtils.isPodReady(pod, true);
            } catch (Exception ex) {
                return false;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    public static void waitForSchemaInSync(AddressSpacePlan addressSpacePlan) {
        TestUtils.waitUntilCondition(String.format("Address space plan %s is applied", addressSpacePlan.getMetadata().getName()),
                waitPhase -> Kubernetes.getInstance().getSchemaClient().inNamespace(
                        addressSpacePlan.getMetadata().getNamespace()).list().getItems().stream()
                        .anyMatch(schema -> schema.getSpec().getPlans().stream()
                                .anyMatch(plan -> plan.getName().contains(addressSpacePlan.getMetadata().getName()))),
                new TimeoutBudget(15, TimeUnit.MINUTES));
    }

    @FunctionalInterface
    public interface ThrowingCallable {
        /**
         * Call.
         *
         * @throws Exception the exception
         */
        void call() throws Exception;
    }

    public static void assertDefaultEnabled(final Boolean enabled) {
        if (enabled != null && !Boolean.TRUE.equals(enabled)) {
            fail("Default value must be 'null' or 'true'");
        }
    }

    /**
     * body for rest api tests
     */
    public static void runRestApiTest(ResourceManager manager,
                                      AddressSpace addressSpace, Address d1, Address d2) throws Exception {
        List<String> destinationsNames = Arrays.asList(d1.getSpec().getAddress(), d2.getSpec().getAddress());
        manager.setAddresses(d1);
        manager.appendAddresses(d2);

        //d1, d2
        List<String> response = AddressUtils.getAddresses(addressSpace).stream().map(
                address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return all addresses", response, is(destinationsNames));
        LOGGER.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        Address res = Kubernetes.getInstance().getAddressClient(
                addressSpace.getMetadata().getNamespace()).withName(d2.getMetadata().getName()).get();
        assertThat("Rest api does not return specific address",
                res.getSpec().getAddress(), is(d2.getSpec().getAddress()));

        manager.deleteAddresses(d1);

        //d2
        response = AddressUtils.getAddresses(addressSpace).stream().map(
                address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return right addresses", response,
                is(destinationsNames.subList(1, 2)));
        LOGGER.info("address {} successfully deleted", d1.getSpec().getAddress());

        manager.deleteAddresses(d2);

        //empty
        List<Address> listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        LOGGER.info("addresses {} successfully deleted", d2.getSpec().getAddress());

        manager.setAddresses(d1, d2);
        manager.deleteAddresses(d1, d2);

        listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        LOGGER.info("addresses {} successfully deleted", Arrays.toString(destinationsNames.toArray()));
    }

    //================================================================================================
    //==================================== Asserts methods ===========================================
    //================================================================================================
    public static <T extends Comparable<T>> void assertSorted(String message, Iterable<T> list) {
        assertSorted(message, list, false);
    }

    public static <T> void assertSorted(String message, Iterable<T> list, Comparator<T> comparator) {
        assertSorted(message, list, false, comparator);
    }

    public static <T extends Comparable<T>> void assertSorted(String message, Iterable<T> list, boolean reverse) {
        LOGGER.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.natural().isOrdered(list), message);
        } else {
            assertTrue(Ordering.natural().reverse().isOrdered(list), message);
        }
    }

    public static <T> void assertSorted(String message, Iterable<T> list, boolean reverse, Comparator<T> comparator) {
        LOGGER.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.from(comparator).isOrdered(list), message);
        } else {
            assertTrue(Ordering.from(comparator).reverse().isOrdered(list), message);
        }
    }

    public static <T> void assertWaitForValue(T expected, Callable<T> fn, TimeoutBudget budget) throws Exception {
        T got = null;
        LOGGER.info("waiting for expected value '{}' ...", expected);
        while (budget.timeLeft() >= 0) {
            got = fn.call();
            if (Objects.equals(expected, got)) {
                return;
            }
            Thread.sleep(100);
        }
        fail(format("Incorrect result value! expected: '%s', got: '%s'", expected, Objects.requireNonNull(got)));
    }

    /**
     * Log with separator.
     *
     * @param logger   the logger
     * @param messages the messages
     */
    public static void logWithSeparator(Logger logger, String... messages) {
        logger.info("--------------------------------------------------------------------------------");
        for (String message : messages) {
            logger.info(message);
        }
    }


}
