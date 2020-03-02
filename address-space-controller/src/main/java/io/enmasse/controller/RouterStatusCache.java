/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.amqp.RouterEntity;
import io.enmasse.amqp.RouterManagement;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.RouterCheckFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

public class RouterStatusCache implements Runnable, Controller {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusCache.class);

    private final EventLogger eventLogger;

    private volatile boolean running = false;
    private Thread thread;
    private final Duration checkInterval;
    private final Object monitor = new Object();
    private boolean needCheck = false;

    private final NamespacedKubernetesClient client;
    private final String namespace;
    private final Vertx vertx = Vertx.vertx();
    private final Duration connectTimeout;
    private final Duration queryTimeout;

    private volatile List<AddressSpace> currentAddressSpaces = new ArrayList<>();
    private final ConcurrentHashMap<AddressSpace, List<RouterStatus>> routerStatusMap = new ConcurrentHashMap<>();

    RouterStatusCache(EventLogger eventLogger, Duration checkInterval, NamespacedKubernetesClient client, String namespace, Duration connectTimeout, Duration queryTimeout)
    {
        this.eventLogger = eventLogger;
        this.checkInterval = checkInterval;
        this.client = client;
        this.namespace = namespace;
        this.connectTimeout = connectTimeout;
        this.queryTimeout = queryTimeout;
    }

    List<RouterStatus> getLatestResult(AddressSpace addressSpace) {
        return routerStatusMap.get(addressSpace);
    }

    @Override
    public void reconcileAll(List<AddressSpace> addressSpaces) {
        // Is this thread safe?

        routerStatusMap.entrySet().removeIf(e -> !addressSpaces.contains(e.getKey()));
        this.currentAddressSpaces = addressSpaces;
        log.info("RouterStatusCache Address Spaces: {}", currentAddressSpaces.stream().map(a -> a.getMetadata().getName()).collect(Collectors.toList()));
        wakeup();
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.setName("router-status-cache");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        try {
            running = false;
            thread.interrupt();
            thread.join();
        } catch (InterruptedException ignored) {
            log.warn("Interrupted while stopping", ignored);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                checkRouterStatus();
                synchronized (monitor) {
                    if (!needCheck) {
                        monitor.wait(checkInterval.toMillis());
                    }
                    needCheck = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Exception in collector task", e);
            }
        }
    }

    public void wakeup() {
        synchronized (monitor) {
            needCheck = true;
            monitor.notifyAll();
        }
    }

    void checkRouterStatus() {
        for (AddressSpace addressSpace : currentAddressSpaces) {
            if ("standard".equals(addressSpace.getSpec().getType())) {
                List<RouterStatus> routerStatusList = checkRouterStatus(addressSpace);
                if (routerStatusList != null) {
                    routerStatusMap.put(addressSpace, routerStatusList);
                }
            }
        }
    }


    private static final RouterEntity connection = new RouterEntity( "org.apache.qpid.dispatch.connection", "operStatus", "opened", "host");
    private static final RouterEntity node = new RouterEntity("org.apache.qpid.dispatch.router.node", "id", "nextHop");
    private static final RouterEntity link = new RouterEntity("org.apache.qpid.dispatch.router.link", "linkType", "undeliveredCount");
    private static final RouterEntity[] entities = new RouterEntity[]{connection, node, link};

    private List<RouterStatus> checkRouterStatus(AddressSpace addressSpace) {
        String addressSpaceCaSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace);
        Secret addressSpaceCa = client.secrets().inNamespace(namespace).withName(addressSpaceCaSecretName).get();
        if (addressSpaceCa == null) {
            log.warn("Unable to check router status, missing address space CA secret for {}!", addressSpace);
            return null;
        }

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] key = decoder.decode(addressSpaceCa.getData().get("tls.key"));
        byte[] cert = decoder.decode(addressSpaceCa.getData().get("tls.crt"));
        if (key == null) {
            log.warn("Unable to check router status, missing address space CA key for {}!", addressSpace);
            return null;
        }

        if (cert == null) {
            log.warn("Unable to check router status, missing address space CA cert for {}!", addressSpace);
            return null;
        }

        RouterManagement routerManagement = RouterManagement.withCerts(vertx, "address-space-controller", connectTimeout, queryTimeout, cert, cert, key);

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);

        List<Pod> routerPods = client.pods().withLabel(LabelKeys.CAPABILITY, "router").withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems().stream()
                .filter(Readiness::isPodReady)
                .collect(Collectors.toList());

        ExecutorCompletionService<RouterStatus> service = new ExecutorCompletionService<>(ForkJoinPool.commonPool());
        for (Pod router : routerPods) {
            service.submit(() -> collectStatus(routerManagement, router));
        }

        List<RouterStatus> routerStatusList = new ArrayList<>();
        for (int i = 0; i < routerPods.size(); i++) {
            try {
                RouterStatus status = service.take().get();
                if (status != null) {
                    routerStatusList.add(status);
                }
            } catch (Exception e) {
                log.info("Error requesting router status. Ignoring", e);
                eventLogger.log(RouterCheckFailed, e.getMessage(), Warning, ControllerKind.AddressSpace, addressSpace.getMetadata().getName());
            }
        }
        return routerStatusList;
    }

    private RouterStatus collectStatus(RouterManagement routerManagement, Pod router) {
        try {

            int port = 0;
            for (Container container : router.getSpec().getContainers()) {
                if (container.getName().equals("router")) {
                    for (ContainerPort containerPort : container.getPorts()) {
                        if (containerPort.getName().equals("amqps-normal")) {
                            port = containerPort.getContainerPort();
                        }
                    }
                }
            }

            if (port != 0) {
                // Until the connector entity allows querying for the status, we have to list
                // all connections and match with the connector host.
                Map<RouterEntity, List<List<?>>> response = routerManagement.query(router.getStatus().getPodIP(), port, entities);

                RouterConnections connections = null;
                if (response.containsKey(connection)) {
                    connections = collectConnectionInfo(response.get(connection));
                }

                List<String> neighbors = null;
                if (response.containsKey(node)) {
                    // Remove this router from the neighbour list
                    neighbors = filterOnAttribute(String.class, 0, response.get(node)).stream()
                            .filter(n -> !n.equals(router.getMetadata().getName()))
                            .collect(Collectors.toList());
                }

                long undeliveredTotal = 0;
                if (response.containsKey(link)) {

                    List<String> linkTypes = filterOnAttribute(String.class, 0, response.get(link));
                    List<UnsignedLong> undelivered = filterOnAttribute(UnsignedLong.class, 1, response.get(link));
                    for (int i = 0; i < linkTypes.size(); i++) {
                        if ("inter-router".equals(linkTypes.get(i))) {
                            undeliveredTotal += undelivered.get(i) != null ? undelivered.get(i).longValue() : 0;
                        }
                    }
                }
                return new RouterStatus(router.getMetadata().getName(), connections, neighbors, undeliveredTotal);
            }
        } catch (Exception e) {
            log.info("Error requesting registered topics from {}. Ignoring", router.getMetadata().getName(), e);
        }
        return null;
    }

    /*
     * Until the connector entity allows querying for the status, we have to go through all connections and
     * see if we can find our connector host in there.
     */
    private RouterConnections collectConnectionInfo(List<List<?>> response) {
        int hostIdx = connection.getAttributeIndex("host");
        int openedIdx = connection.getAttributeIndex("opened");
        int operStatusIdx = connection.getAttributeIndex("operStatus");

        List<String> hosts = filterOnAttribute(String.class, hostIdx, response);
        List<Boolean> opened = filterOnAttribute(Boolean.class, openedIdx, response);
        List<String> operStatus = filterOnAttribute(String.class, operStatusIdx, response);

        return new RouterConnections(hosts, opened, operStatus);
    }

    private static <T> List<T> filterOnAttribute(Class<T> type, int attrNum, List<List<?>> list) {
        List<T> filtered = new ArrayList<>();
        for (List<?> entry : list) {
            T filteredValue = type.cast(entry.get(attrNum));
            if (filteredValue != null) {
                filtered.add(filteredValue);
            }
        }
        return filtered;
    }

    @Override
    public String toString() {
        return "RouterStatusCache";
    }
}
