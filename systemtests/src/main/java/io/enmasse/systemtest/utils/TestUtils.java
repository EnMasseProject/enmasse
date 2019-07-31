/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.utils;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.BrokerState;
import io.enmasse.address.model.BrokerStatus;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import com.google.common.io.BaseEncoding;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestUtils {
    private static Logger log = CustomLogger.getLogger();

    private static final Random R = new Random();

    /**
     * scale up/down specific pod (type: Deployment) in address space
     */
    public static void setReplicas(Kubernetes kubernetes, String infraUuid, String deployment, int numReplicas, TimeoutBudget budget) throws InterruptedException {
        kubernetes.setDeploymentReplicas(deployment, numReplicas);
        Map<String, String> labels = new HashMap<>();
        labels.put("name", deployment);
        if (infraUuid != null) {
            labels.put("infraUuid", infraUuid);
        }
        waitForNReplicas(
                numReplicas,
                labels,
                budget);
    }

    public static void waitForNReplicas(int expectedReplicas, Map<String, String> labelSelector, TimeoutBudget budget) throws InterruptedException {
        waitForNReplicas(expectedReplicas, labelSelector, Collections.emptyMap(), budget);
    }

    /**
     * wait for expected count of Destination replicas in address space
     */
    public static void waitForNBrokerReplicas(AddressSpace addressSpace, int expectedReplicas, boolean readyRequired,
                                              Address destination, TimeoutBudget budget, long checkInterval) throws Exception {
        Address address = Kubernetes.getInstance().getAddressClient(addressSpace.getMetadata().getNamespace()).withName(destination.getMetadata().getName()).get();
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

    public static void waitForNBrokerReplicas(AddressSpace addressSpace, int expectedReplicas, Address destination, TimeoutBudget budget) throws Exception {
        waitForNBrokerReplicas(addressSpace, expectedReplicas, true, destination, budget, 5000);
    }


    /**
     * Wait for expected count of replicas
     *
     * @param expectedReplicas   count of expected replicas
     * @param labelSelector      labels on scaled pod
     * @param annotationSelector annotations on sclaed pod
     * @param budget             timeout budget - throws Exception when timeout is reached
     * @throws InterruptedException
     */
    public static void waitForNReplicas(int expectedReplicas, boolean readyRequired,
                                        Map<String, String> labelSelector, Map<String, String> annotationSelector, TimeoutBudget budget, long checkInterval) throws InterruptedException {

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

            log.info("Have {} out of {} replicas. Expecting={}, ReadyRequired={}", actualReplicas, pods.size(), expectedReplicas, readyRequired);

            if (budget.timeoutExpired()) {
                // our time budged expired ... throw exception
                String message = String.format("Only '%s' out of '%s' in state 'Running' before timeout %s", actualReplicas, expectedReplicas,
                        pods.stream().map(pod -> pod.getMetadata().getName()).collect(Collectors.joining(",")));
                throw new RuntimeException(message);
            }

            // try again next cycle

            Thread.sleep(checkInterval);

        } while (actualReplicas != expectedReplicas);

        // finished successfully

    }

    public static void waitForNReplicas(int expectedReplicas, Map<String, String> labelSelector, Map<String, String> annotationSelector, TimeoutBudget budget, long checkInterval) throws InterruptedException {
        waitForNReplicas(expectedReplicas, true, labelSelector, annotationSelector, budget, checkInterval);
    }

    public static void waitForNReplicas(int expectedReplicas, Map<String, String> labelSelector, Map<String, String> annotationSelector, TimeoutBudget budget) throws InterruptedException {
        waitForNReplicas(expectedReplicas, labelSelector, annotationSelector, budget, 5000);
    }

    /**
     * Check ready status of all pods in list
     *
     * @param pods list of pods
     * @return
     */
    private static int numReady(List<Pod> pods) {
        return (int) pods.stream().filter(pod -> isPodReady(pod, true)).count();
    }

    public static boolean isPodReady(final Pod pod, final boolean doLog) {

        if (!"Running".equals(pod.getStatus().getPhase())) {
            if (doLog) {
                log.info("POD {} in status : {}", pod.getMetadata().getName(), pod.getStatus().getPhase());
            }
            return false;
        }

        var nonReadyContainers = pod.getStatus().getContainerStatuses().stream()
                .filter(cs -> !Boolean.TRUE.equals(cs.getReady()))
                .map(ContainerStatus::getName)
                .collect(Collectors.toList());

        if (!nonReadyContainers.isEmpty()) {
            if (doLog) {
                log.info("POD {} non-ready containers: [{}]", pod.getMetadata().getName(), String.join(", ", nonReadyContainers));
            }
            return false;
        }

        return true;
    }

    /**
     * Wait for expected count of pods within AddressSpace
     *
     * @param client       client for manipulation with kubernetes cluster
     * @param addressSpace
     * @param numExpected  count of expected pods
     * @param budget       timeout budget - this method throws Exception when timeout is reached
     * @throws InterruptedException
     */
    public static void waitForExpectedReadyPods(Kubernetes client, AddressSpace addressSpace, int numExpected, TimeoutBudget budget) throws Exception {
        List<Pod> pods = listRunningPods(client, addressSpace);
        while (budget.timeLeft() >= 0 && pods.size() != numExpected) {
            Thread.sleep(2000);
            pods = listRunningPods(client, addressSpace);
            log.info("Got {} pods, expected: {}", pods.size(), numExpected);
        }
        if (pods.size() != numExpected) {
            throw new IllegalStateException("Unable to find " + numExpected + " pods. Found : " + printPods(pods));
        }
    }

    /**
     * Wait for expected count of pods within AddressSpace
     *
     * @param client      client for manipulation with kubernetes cluster
     * @param numExpected count of expected pods
     * @param budget      timeout budget - this method throws Exception when timeout is reached
     * @throws InterruptedException
     */
    public static void waitForExpectedReadyPods(Kubernetes client, String namespace, int numExpected, TimeoutBudget budget) throws InterruptedException {
        log.info("Waiting for expected ready pods: {}", numExpected);
        List<Pod> pods = listReadyPods(client, namespace);
        while (budget.timeLeft() >= 0 && pods.size() != numExpected) {
            Thread.sleep(2000);
            pods = listReadyPods(client, namespace);
            log.info("Got {} pods, expected: {}", pods.size(), numExpected);
        }
        if (pods.size() != numExpected) {
            Set<Pod> ready = new HashSet<>(pods);
            Set<Pod> unready = client.listPods(namespace).stream().filter(pod -> !ready.contains(pod)).collect(Collectors.toSet());
            throw new IllegalStateException(String.format("Unable to find %d ready pods. Ready : %s, Unready : %s", numExpected, printPods(ready), printPods(unready)));
        }
        for (Pod pod : pods) {
            client.waitUntilPodIsReady(pod);
        }
    }

    /**
     * Print name of all pods in list
     *
     * @param pods list of pods that should be printed
     * @return
     */
    public static String printPods(Collection<Pod> pods) {
        return pods.stream()
                .map(pod -> "{" + pod.getMetadata().getName() + ", " + pod.getStatus().getPhase() + "}")
                .collect(Collectors.joining(","));
    }

    /**
     * Get list of all running pods from specific AddressSpace
     *
     * @param kubernetes   client for manipulation with kubernetes cluster
     * @param addressSpace
     * @return
     */
    public static List<Pod> listRunningPods(Kubernetes kubernetes, AddressSpace addressSpace) throws Exception {
        return kubernetes.listPods(Collections.singletonMap("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace))).stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .collect(Collectors.toList());
    }

    /**
     * Get list of all running pods from specific AddressSpace
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @return
     */
    public static List<Pod> listRunningPods(Kubernetes kubernetes) {
        return kubernetes.listPods().stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .collect(Collectors.toList());
    }

    /**
     * Get list of all ready pods
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @return
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
     * @return
     */
    public static List<Pod> listReadyPods(Kubernetes kubernetes, String namespace) {
        return kubernetes.listPods(namespace).stream()
                .filter(pod -> pod.getStatus().getContainerStatuses().stream().allMatch(ContainerStatus::getReady))
                .collect(Collectors.toList());
    }

    /**
     * Get list of all non-ready pods
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @return
     */
    public static Stream<Pod> streamNonReadyPods(Kubernetes kubernetes, String namespace) {
        return kubernetes.listPods(namespace).stream()
                .filter(pod -> !isPodReady(pod, false));
    }

    public static List<Pod> listBrokerPods(Kubernetes kubernetes) {
        return kubernetes.listPods(Collections.singletonMap("role", "broker"));
    }

    public static List<Pod> listBrokerPods(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("role", "broker");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPods(labels);
    }

    public static List<Pod> listRouterPods(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("capability", "router");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPods(labels);
    }

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

    public static List<PersistentVolumeClaim> listPersistentVolumeClaims(Kubernetes kubernetes, AddressSpace addressSpace) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        return kubernetes.listPersistentVolumeClaims(labels);
    }

    /**
     * Generate message body with prefix
     */
    public static List<String> generateMessages(String prefix, int numMessages) {
        return IntStream.range(0, numMessages).mapToObj(i -> prefix + i).collect(Collectors.toList());
    }

    /**
     * Generate message body with "testmessage" content and without prefix
     */
    public static List<String> generateMessages(int numMessages) {
        return generateMessages("testmessage", numMessages);
    }

    /**
     * Check if endpoint is accessible
     */
    public static boolean resolvable(Endpoint endpoint) {
        for (int i = 0; i < 10; i++) {
            try {
                InetAddress[] addresses = Inet4Address.getAllByName(endpoint.getHost());
                return addresses.length > 0;
            } catch (UnknownHostException ignore) {
            }
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }
        return false;
    }

    /**
     * Wait until Namespace will be removed
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @param namespace  project/namespace to remove
     */
    public static void waitForNamespaceDeleted(Kubernetes kubernetes, String namespace) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && kubernetes.listNamespaces().contains(namespace)) {
            Thread.sleep(1000);
        }
        if (kubernetes.listNamespaces().contains(namespace)) {
            throw new TimeoutException("Timed out waiting for namespace " + namespace + " to disappear");
        }
    }

    /**
     * Repeat request n-times in a row
     *
     * @param retry count of remaining retries
     * @param fn    request function
     * @return
     */
    public static <T> T doRequestNTimes(int retry, Callable<T> fn, Optional<Runnable> reconnect) throws Exception {
        try {
            return fn.call();
        } catch (Exception ex) {
            if (ex.getCause() instanceof VertxException && ex.getCause().getMessage().contains("Connection was closed")) {
                if (reconnect.isPresent()) {
                    log.warn("connection was closed, trying to reconnect...");
                    reconnect.get().run();
                }
            }
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    log.info("{} remaining iterations", retry);
                    return doRequestNTimes(retry - 1, fn, reconnect);
                } catch (Exception ex2) {
                    throw ex2;
                }
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
     * @param retry count of remaining retries
     * @param fn    request function
     * @return The value from the first successful call to the callable
     */
    public static <T> T runUntilPass(int retry, Callable<T> fn) throws InterruptedException {
        for (int i = 0; i < retry; i++) {
            try {
                log.info("Running command, attempt: {}", i);
                return fn.call();
            } catch (Exception ex) {
                log.info("Command failed", ex);
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException(String.format("Command wasn't pass in %s attempts", retry));
    }

    /**
     * Repeat command n-times.
     *
     * @param retries Number of retries.
     * @param callable Code to execute.
     */
    public static void runUntilPass(int retries, ThrowingCallable callable) throws InterruptedException {
        runUntilPass(retries, () -> {
            callable.call();
            return null;
        });
    }

    /**
     * Replace address plan in ConfigMap of already existing address
     *
     * @param kubernetes client for manipulation with kubernetes cluster
     * @param addrSpace  address space which contains ConfigMap
     * @param dest       destination which will be modified
     * @param plan       definition of AddressPlan
     */
    public static void replaceAddressConfig(Kubernetes kubernetes, AddressSpace addrSpace, Address dest, AddressPlan plan) {
        String mapKey = "config.json";
        ConfigMap destConfigMap = kubernetes.getConfigMap(addrSpace.getMetadata().getNamespace(), dest.getSpec().getAddress());

        JsonObject data = new JsonObject(destConfigMap.getData().get(mapKey));
        log.info(data.toString());
        data.getJsonObject("spec").remove("plan");
        data.getJsonObject("spec").put("plan", plan.getMetadata().getName());

        Map<String, String> modifiedData = new LinkedHashMap<>();
        modifiedData.put(mapKey, data.toString());
        destConfigMap.setData(modifiedData);
        kubernetes.replaceConfigMap(addrSpace.getMetadata().getNamespace(), destConfigMap);
    }

    public static void deleteAddressSpaceCreatedBySC(Kubernetes kubernetes, AddressSpace addressSpace, GlobalLogCollector logCollector) throws Exception {
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

    public static RemoteWebDriver getFirefoxDriver() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.getFirefoxSeleniumAppEndpoint(Kubernetes.getInstance());
        return getRemoteDriver(endpoint.getHost(), endpoint.getPort(), new FirefoxOptions());
    }

    public static RemoteWebDriver getChromeDriver() throws Exception {
        Endpoint endpoint = SystemtestsKubernetesApps.getChromeSeleniumAppEndpoint(Kubernetes.getInstance());
        return getRemoteDriver(endpoint.getHost(), endpoint.getPort(), new ChromeOptions());
    }

    private static RemoteWebDriver getRemoteDriver(String host, int port, Capabilities options) throws Exception {
        int attempts = 60;
        URL hubUrl = new URL(String.format("http://%s:%s/wd/hub", host, port));
        for (int i = 0; i < attempts; i++) {
            if (isReachable(hubUrl)) {
                return new RemoteWebDriver(hubUrl, options);
            }
            Thread.sleep(2000);
        }
        throw new IllegalStateException("Selenium webdriver cannot connect to selenium container");
    }

    public static boolean isReachable(URL url) {
        log.info("Trying to connect to {}", url.toString());
        try {
            url.openConnection();
            url.getContent();
            log.info("Client is able to connect to the selenium hub");
            return true;
        } catch (Exception ex) {
            log.warn("Cannot connect to hub: {}", ex.getMessage());
            return false;
        }
    }

    public static void waitUntilCondition(Callable<String> fn, String expected, TimeoutBudget budget) throws Exception {
        String actual = "Too small time out budget!!";
        while (!budget.timeoutExpired()) {
            actual = fn.call();
            log.debug(actual);
            if (actual.contains(expected)) {
                return;
            }
            log.debug("next iteration, remaining time: {}", budget.timeLeft());
            Thread.sleep(2000);
        }
        throw new IllegalStateException(String.format("Expected: '%s' in content, but was: '%s'", expected, actual));
    }

    public static void waitUntilCondition(final String forWhat, final Predicate<WaitPhase> condition, final TimeoutBudget budget) throws Exception {
        Objects.requireNonNull(condition);
        Objects.requireNonNull(budget);

        log.info("Waiting {} ms for - {}", budget.timeLeft(), forWhat);

        while (!budget.timeoutExpired()) {
            if (condition.test(WaitPhase.LOOP)) {
                return;
            }
            log.debug("next iteration, remaining time: {}", budget.timeLeft());
            Thread.sleep(5000);
        }

        if (condition.test(WaitPhase.LAST_TRY)) {
            return;
        }

        throw new IllegalStateException("Failed to wait for: " + forWhat);
    }

    public static void waitForChangedResourceVersion(final TimeoutBudget budget, final String namespace, final String name, final String currentResourceVersion) throws Exception {
        waitForChangedResourceVersion(budget, currentResourceVersion, () -> Kubernetes.getInstance().getAddressSpaceClient(namespace).withName(name).get().getMetadata().getResourceVersion());
    }

    public static void waitForChangedResourceVersion(final TimeoutBudget budget, final String currentResourceVersion, final ThrowingSupplier<String> provideNewResourceVersion)
            throws Exception {
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

    public static String getGlobalConsoleRoute() throws Exception {
        return Kubernetes.getInstance().getConsoleServiceClient().withName("console").get().getStatus().getUrl();
    }

    public static CompletableFuture<Void> runAsync(ThrowingCallable callable) {
        return CompletableFuture.runAsync(() -> {
            try {
                callable.call();
            } catch (Exception e) {
                log.error("Error running async test", e);
                throw new RuntimeException(e);
            }
        }, e -> new Thread(e).start());
    }

    @FunctionalInterface
    public static interface ThrowingCallable {
        void call() throws Exception;
    }

    /**
     * Return a number of random characters in the range of {@code 0-9a-f}.
     * @param length the number of characters to return
     * @return The random string, never {@code null}.
     */
    public static String randomCharacters(int length) {
        var b = new byte[(int) Math.ceil(length/2.0)];
        R.nextBytes(b);
        return BaseEncoding.base16().encode(b).substring(length%2);
    }

    public static void waitUntilDeployed(String namespace) throws Exception {
        TestUtils.waitUntilCondition("All pods and container is ready", waitPhase -> {
            List<Pod> pods = Kubernetes.getInstance().listPods(namespace);
            for (Pod pod : pods) {
                List<ContainerStatus> initContainers = pod.getStatus().getInitContainerStatuses();
                for (ContainerStatus s : initContainers) {
                    if (!s.getReady())
                        return false;
                }
                List<ContainerStatus> containers = pod.getStatus().getContainerStatuses();
                for (ContainerStatus s : containers) {
                    if (!s.getReady())
                        return false;
                }
            }
            return true;
        }, new TimeoutBudget(10, TimeUnit.MINUTES));
    }

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

        brInfraConfigClient.list().getItems().forEach(cr -> brInfraConfigClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        stInfraConfigClient.list().getItems().forEach(cr -> stInfraConfigClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addressSpaceClient.list().getItems().forEach(cr -> addressSpaceClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addressClient.list().getItems().forEach(cr -> addressClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addrSpacePlanClient.list().getItems().forEach(cr -> addrSpacePlanClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        addPlanClient.list().getItems().forEach(cr -> addPlanClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        authServiceClient.list().getItems().forEach(cr -> authServiceClient.withName(cr.getMetadata().getName()).cascading(true).delete());
        consoleClient.list().getItems().forEach(cr -> consoleClient.withName(cr.getMetadata().getName()).cascading(true).delete());
    }
}
