/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.logs;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.bases.ThrowableRunner;
import io.enmasse.systemtest.condition.OpenShiftVersion;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class GlobalLogCollector {
    private final static Logger LOGGER = CustomLogger.getLogger();
    private static boolean verbose = true;
    private final Kubernetes kubernetes;
    private final Path logDir;
    private final String namespace;
    private boolean appendNamespaceToLogDir;

    public GlobalLogCollector(Kubernetes kubernetes, Path logDir) {
        this(kubernetes, logDir, kubernetes.getInfraNamespace());
    }

    public GlobalLogCollector(Kubernetes kubernetes, Path logDir, String namespace) {
        this(kubernetes, logDir, namespace, true);
    }

    public GlobalLogCollector(Kubernetes kubernetes, Path logDir, String namespace, boolean appendNamespaceToLogDir) {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
        this.namespace = namespace;
        this.appendNamespaceToLogDir = appendNamespaceToLogDir;
    }

    public void collectConfigMaps() {
        collectConfigMaps("global");
    }

    public static void globalDisableVerboseLogging() {
        GlobalLogCollector.verbose = false;
    }

    public void collectConfigMaps(String operation) {
        if (verbose) {
            LOGGER.info("Collecting configmaps for namespace {}", namespace);
        }
        kubernetes.getAllConfigMaps(namespace).getItems().forEach(configMap -> {
            try {
                Path confMapFile = resolveLogFile(configMap.getMetadata().getName() + "." + operation + ".configmap");
                if (verbose) {
                    LOGGER.info("config map '{}' will be archived with path: '{}'", configMap.getMetadata().getName(), confMapFile);
                }
                if (!Files.exists(confMapFile)) {
                    try (BufferedWriter bf = Files.newBufferedWriter(confMapFile)) {
                        bf.write(configMap.toString());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Collect logs from terminated pods in namespace
     */
    public void collectLogsTerminatedPods() {
        LOGGER.info("Store logs from all terminated pods in namespace '{}'", namespace);
        kubernetes.getLogsOfTerminatedPods(namespace).forEach((podName, podLogTerminated) -> {
            try {
                Path podLog = resolveLogFile(namespace + "." + podName + ".terminated.log");
                LOGGER.info("log of terminated '{}' pod will be archived with path: '{}'", podName, podLog);
                try (BufferedWriter bf = Files.newBufferedWriter(podLog)) {
                    bf.write(podLogTerminated);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void collectLogsOfPodsByLabels(String namespace, String discriminator, Map<String, String> labels) {
        if (verbose) {
            LOGGER.info("Store logs from all pods in namespace '{}' matching labels {}", namespace, labels);
        }
        kubernetes.getLogsByLables(namespace, labels).forEach((podName, podLogs) -> {
            try {
                String filename = discriminator == null ? String.format("%s.%s.log", namespace, podName) : String.format("%s.%s.%s.log", namespace, discriminator, podName);
                Path podLog = resolveLogFile(filename);
                if (verbose) {
                    LOGGER.info("log of '{}' pod will be archived with path: '{}'", podName, podLog);
                }
                try (BufferedWriter bf = Files.newBufferedWriter(podLog)) {
                    bf.write(podLogs);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void collectLogsOfPodsInNamespace(String namespace) {
        LOGGER.info("Store logs from all pods in namespace '{}'", namespace);
        kubernetes.getLogsOfAllPods(namespace).forEach((podName, podLogs) -> {
            try {
                String filename = String.format("%s.%s.log", namespace, podName);
                Path podLog = resolveLogFile(filename);
                LOGGER.info("log of '{}' pod will be archived with path: '{}'", podName, podLog);
                try (BufferedWriter bf = Files.newBufferedWriter(podLog)) {
                    bf.write(podLogs);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void collectEvents() {
        collectEvents(this.namespace);
    }

    public void collectEvents(String namespace) {
        long timestamp = System.currentTimeMillis();
        LOGGER.info("Collecting events in {}", namespace);
        ExecutionResultData result = KubeCMDClient.getEvents(namespace);
        try {
            Path eventLog = resolveLogFile(namespace + ".events." + timestamp);
            try (BufferedWriter writer = Files.newBufferedWriter(eventLog)) {
                writer.write(result.getStdOut());
            } catch (Exception e) {
                LOGGER.error("Error collecting events for {}", namespace, e);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void collectAllAdapterQdrProxyState() {
        collectHttpAdapterQdrProxyState();
        collectMqttAdapterQdrProxyState();
    }

    private void collectHttpAdapterQdrProxyState() {
        LOGGER.info("Collecting qdr-proxy router state in namespace {}", namespace);
        collectRouterState("httpAdapterQdrProxyState", System.currentTimeMillis(),
                kubernetes.listPods(Map.of("component", "iot", "name", "iot-http-adapter")).stream(),
                Optional.of("qdr-proxy"),
                this::qdrProxyCmd);
    }

    private void collectMqttAdapterQdrProxyState() {
        LOGGER.info("Collecting qdr-proxy router state in namespace {}", namespace);
        collectRouterState("mqttAdapterQdrProxyState", System.currentTimeMillis(),
                kubernetes.listPods(Map.of("component", "iot", "name", "iot-mqtt-adapter")).stream(),
                Optional.of("qdr-proxy"),
                this::qdrProxyCmd);
    }

    public void collectRouterState(String operation) {
        LOGGER.info("Collecting router state in namespace {}", namespace);
        collectRouterState(operation, System.currentTimeMillis(),
                kubernetes.listPods(Map.of("capability", "router")).stream(),
                Optional.of("router"),
                this::enmasseRouterCmd);
    }

    private void collectRouterState(String operation, long timestamp, Stream<Pod> podsStream, Optional<String> container,
                                    BiFunction<String, String[], String[]> saslMechanismArgsCmdProvider) {
        podsStream.filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .forEach(pod -> {
                    collectRouterInfo(pod, container, "." + operation + ".autolinks." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdmanage", new String[]{"QUERY", "--type=autoLink"}));
                    collectRouterInfo(pod, container, "." + operation + ".links." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdmanage", new String[]{"QUERY", "--type=link"}));
                    collectRouterInfo(pod, container, "." + operation + ".connections." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdmanage", new String[]{"QUERY", "--type=connection"}));
                    collectRouterInfo(pod, container, "." + operation + ".qdstat_a." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdstat", new String[]{"-a"}));
                    collectRouterInfo(pod, container, "." + operation + ".qdstat_l." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdstat", new String[]{"-l"}));
                    collectRouterInfo(pod, container, "." + operation + ".qdstat_n." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdstat", new String[]{"-n"}));
                    collectRouterInfo(pod, container, "." + operation + ".qdstat_c." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdstat", new String[]{"-c"}));
                    collectRouterInfo(pod, container, "." + operation + ".qdstat_linkroutes." + timestamp + ".log", saslMechanismArgsCmdProvider.apply("qdstat", new String[]{"--linkroutes"}));
                });
    }

    private String[] qdrProxyCmd(String cmd, String... extraArgs) {
        List<String> allArgs = new ArrayList<>();
        allArgs.add(cmd);
        allArgs.add("--sasl-mechanisms=ANONYMOUS");
        allArgs.add("-b");
        allArgs.add("127.0.0.1:5672");
        allArgs.addAll(Arrays.asList(extraArgs));
        return allArgs.toArray(String[]::new);
    }

    private String[] enmasseRouterCmd(String cmd, String... extraArgs) {
        List<String> allArgs = new ArrayList<>();
        allArgs.add(cmd);
        allArgs.add("-b");
        allArgs.add("127.0.0.1:7777");
        allArgs.addAll(Arrays.asList(extraArgs));
        return allArgs.toArray(String[]::new);
    }

    private void collectRouterInfo(Pod pod, Optional<String> container, String filesuffix, String[] command) {
        String output = KubeCMDClient.runOnPod(
                pod.getMetadata().getNamespace(),
                pod.getMetadata().getName(),
                container,
                command).getStdOut();
        try {
            Path routerAutoLinks = resolveLogFile(pod.getMetadata().getName() + filesuffix);
            if (verbose) {
                LOGGER.info("router info '{}' pod will be archived with path: '{}'", pod.getMetadata().getName(), routerAutoLinks);
            }
            Files.writeString(routerAutoLinks, output, UTF_8, CREATE_NEW);
        } catch (IOException e) {
            LOGGER.warn("Error collecting router state", e);
        }
    }

    /**
     * Create a new path inside the log directory, and ensure that the parent directory exists.
     *
     * @param other the path segment, relative to the log directory.
     * @return The full path.
     * @throws IOException In case of any IO error
     */
    private Path resolveLogFile(final String other) throws IOException {
        Path path = logDir;
        if (appendNamespaceToLogDir) {
            path = path.resolve(namespace);
        }
        return Files.createDirectories(path).resolve(other);
    }

    public static void saveInfraState(Path path) {
        Kubernetes kube = Kubernetes.getInstance();
        saveInfraState(path, kube.getInfraNamespace(),
                () -> {
                    if (TestInfo.getInstance().isClassIoT()) {
                        Files.writeString(path.resolve("iotconfig.yaml"), KubeCMDClient.getIoTConfig(kube.getInfraNamespace()).getStdOut());
                        Files.writeString(path.resolve("iotprojects.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "-A", "iotproject", "-o", "yaml").getStdOut());
                        GlobalLogCollector collectors = new GlobalLogCollector(kube, path, kube.getInfraNamespace());
                        collectors.collectAllAdapterQdrProxyState();
                    }
                },
                () -> {
                    if (TestInfo.getInstance().isOLMTest()) {
                        Files.writeString(path.resolve("describe_pods_olm.txt"), KubeCMDClient.describePods(kube.getOlmNamespace()).getStdOut());
                    }
                });
    }

    public static void saveInfraState(Path path, String infraNamespace, ThrowableRunner... extraCollectors) {
        try {
            LOGGER.info("Saving pod logs and info...");
            // check for marker file
            if (Files.exists(path.resolve("done.txt"))) {
                LOGGER.info("Information already saved in {}", path);
                return;
            }
            Kubernetes kube = Kubernetes.getInstance();
            Files.createDirectories(path);
            List<String> nsList = new ArrayList<>();
            nsList.add(infraNamespace);
            nsList.addAll(Arrays.asList(SystemtestsKubernetesApps.ST_NAMESPACES));
            nsList.add(Environment.getInstance().getMonitoringNamespace());
            // TMP: trying to understand sporadic problems with openshift 4 console authentication.
            if (Kubernetes.isOpenShiftCompatible(OpenShiftVersion.OCP4)) {
                nsList.add("openshift-authentication");
            }
            // TMP: check logs from openshift router.
            nsList.add("default");
            for (String ns : nsList) {
                if (kube.namespaceExists(ns)) {
                    List<Pod> pods = kube.listAllPods(ns);
                    for (Pod p : pods) {
                        try {
                            List<Container> containers = kube.getContainersFromPod(ns, p.getMetadata().getName());
                            for (Container c : containers) {
                                Path filePath = path.resolve(String.format("%s_%s.log", p.getMetadata().getName(), c.getName()));
                                try {
                                    Files.writeString(filePath, kube.getLog(ns, p.getMetadata().getName(), c.getName()));
                                } catch (IOException e) {
                                    LOGGER.warn("Cannot write file {}", filePath, e);
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.warn("Cannot access logs from pod {} ", p.getMetadata().getName(), ex);
                        }
                    }
                }
            }

            kube.getLogsOfTerminatedPods(infraNamespace).forEach((name, podLogTerminated) -> {
                Path filePath = path.resolve(String.format("%s.terminated.log", name));
                try {
                    Files.writeString(filePath, podLogTerminated);
                } catch (IOException e) {
                    LOGGER.warn("Cannot write file {}", filePath, e);
                }
            });

            //ns specific logs
            for (String ns : nsList) {
                if (kube.namespaceExists(ns)) {
                    Files.writeString(path.resolve(String.format("describe_pods.%s.txt", ns)), KubeCMDClient.describePods(ns).getStdOut());
                    Files.writeString(path.resolve(String.format("events.%s.txt", ns)), KubeCMDClient.getEvents(ns).getStdOut());
                    Files.writeString(path.resolve(String.format("configmaps.%s.yaml", ns)), KubeCMDClient.getConfigmaps(ns).getStdOut());
                    Files.writeString(path.resolve(String.format("secrets.%s.yaml", ns)), KubeCMDClient.getSecrets(ns).getStdOut());
                    Files.writeString(path.resolve(String.format("pvcs.%s.txt", ns)), KubeCMDClient.runOnClusterWithoutLogger("describe", "pvc", "-n", ns).getStdOut());
                    Files.writeString(path.resolve(String.format("deployments.%s.yml", ns)), KubeCMDClient.runOnClusterWithoutLogger("get", "deployments", "-o", "yaml", "-n", ns).getStdOut());
                    Files.writeString(path.resolve(String.format("statefulsets.%s.yml", ns)), KubeCMDClient.runOnClusterWithoutLogger("get", "statefulsets", "-o", "yaml", "-n", ns).getStdOut());
                }
            }

            //resource specific logs
            Files.writeString(path.resolve("describe_addressspaces.txt"), KubeCMDClient.runOnClusterWithoutLogger("describe", "addressspaces", "--all-namespaces").getStdOut());
            Files.writeString(path.resolve("describe_addresses.txt"), KubeCMDClient.runOnClusterWithoutLogger("describe", "addresses", "--all-namespaces").getStdOut());
            Files.writeString(path.resolve("addressspaces.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "addresses", "-o", "yaml", "--all-namespaces").getStdOut());
            Files.writeString(path.resolve("addresses.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "addresses", "-o", "yaml", "--all-namespaces").getStdOut());
            Files.writeString(path.resolve("users.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "messaginguser", "-o", "yaml", "--all-namespaces").getStdOut());

            //cluster wide logs
            Files.writeString(path.resolve("pvs.txt"), KubeCMDClient.runOnClusterWithoutLogger("describe", "pv").getStdOut());
            Files.writeString(path.resolve("storageclass.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "storageclass", "-o", "yaml").getStdOut());
            if (Kubernetes.isOpenShiftCompatible()) {
                Files.writeString(path.resolve("routes.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "-A", "routes", "-o", "yaml").getStdOut());
                Files.writeString(path.resolve("routes.yml"), KubeCMDClient.runOnClusterWithoutLogger("get", "-A", "routes", "-o", "yaml").getStdOut());

            }
            Files.writeString(path.resolve("events.txt"), KubeCMDClient.getAllEvents().getStdOut());
            Files.writeString(path.resolve("describe_nodes.txt"), KubeCMDClient.describeNodes().getStdOut());

            if (extraCollectors != null) {
                for (var c : extraCollectors) {
                    c.run();
                }
            }
            LOGGER.info("Pod logs and describe successfully stored into {}", path);
            // create marker file
            Files.createFile(path.resolve("done.txt"));
        } catch (Exception ex) {
            LOGGER.warn("Cannot save pod logs and info", ex);
        }
    }
}
