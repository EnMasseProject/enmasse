/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.amqp.SyncRequestClient;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.enmasse.controller.standard.ControllerKind.AddressSpace;
import static io.enmasse.controller.standard.ControllerReason.RouterCheckFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

class RouterStatus {
    private final String routerId;
    private final List<String> addresses;
    private final List<List<String>> autoLinks;
    private final List<List<String>> linkRoutes;
    private final List<String> connections;

    RouterStatus(String routerId, List<String> addresses, List<List<String>> autoLinks, List<List<String>> linkRoutes, List<String> connections) {
        this.routerId = routerId;
        this.addresses = addresses;
        this.autoLinks = autoLinks;
        this.linkRoutes = linkRoutes;
        this.connections = connections;
    }

    public String getRouterId() {
        return routerId;
    }

    public StatusCheckResult checkAddress(Address address) {
        if (addresses.contains(address.getAddress())) {
            return new StatusCheckResult(true);
        } else {
            return new StatusCheckResult(false, "Address " + address.getAddress() + " not found on " + routerId);
        }
    }

    public List<StatusCheckResult> checkAutoLinks(Address address) {

        List<StatusCheckResult> statusCheckResults = new ArrayList<>();
        for (List<String> autoLink : autoLinks) {
            String addr = autoLink.get(0);

            if (addr.equals(address.getAddress())) {
                statusCheckResults.add(new StatusCheckResult(true));
            }
        }

        if (statusCheckResults.size() < 2) {
            statusCheckResults.add(new StatusCheckResult(false, "Address " + address.getAddress() + " is missing autoLinks on " + routerId));
        }

        return statusCheckResults;
    }

    public List<StatusCheckResult> checkLinkRoutes(Address address) {
        List<StatusCheckResult> statusCheckResults = new ArrayList<>();

        for (List<String> linkRoute : linkRoutes) {
            String prefix = linkRoute.get(0);

            // Pooled topics have active link routes
            if (prefix.equals(address.getAddress())) {
                statusCheckResults.add(new StatusCheckResult(true));
            }
        }

        if (statusCheckResults.size() < 2) {
            statusCheckResults.add(new StatusCheckResult(false, "Address " + address.getAddress() + " is missing linkRoutes on " + routerId));
        }

        return statusCheckResults;
    }

    public static StatusCheckResult checkActiveAutoLink(Address address, List<RouterStatus> routerStatusList) {
        Set<String> active = new HashSet<>();

        for (RouterStatus routerStatus : routerStatusList) {

            for (List<String> autoLink : routerStatus.autoLinks) {
                String addr = autoLink.get(0);
                String dir = autoLink.get(2);
                String operStatus = autoLink.get(3);

                if (addr.equals(address.getAddress()) && operStatus.equals("active")) {
                    active.add(dir);
                }
            }
        }

        if (active.size() < 2) {
            return new StatusCheckResult(false, "Address " + address.getAddress() + " is missing active autoLink (active in dirs: " + active + ")");
        } else {
            return new StatusCheckResult(true);
        }
    }

    public static StatusCheckResult checkActiveLinkRoute(Address address, List<RouterStatus> routerStatusList) {
        int ok = 0;
        Set<String> active = new HashSet<>();
        String brokerId = address.getAnnotations().get(AnnotationKeys.BROKER_ID);

        for (RouterStatus routerStatus : routerStatusList) {

            for (List<String> linkRoute : routerStatus.linkRoutes) {
                String addr = linkRoute.get(0);
                String containerId = linkRoute.get(1);
                String dir = linkRoute.get(2);
                String operStatus = linkRoute.get(3);

                if (addr.equals(address.getAddress()) && brokerId != null && brokerId.equals(containerId) && operStatus.equals("active")) {
                    active.add(dir);
                }
            }
        }

        if (active.size() < 2) {
            return new StatusCheckResult(false, "Address " + address.getAddress() + " is missing active linkRoute (active in dirs: " + active + ")");
        } else {
            return new StatusCheckResult(true);
        }
    }

    public static StatusCheckResult checkConnection(Address address, List<RouterStatus> routerStatusList) {
        for (RouterStatus routerStatus : routerStatusList) {
            for (String containerId : routerStatus.connections) {
                if (containerId.startsWith(address.getName())) {
                    return new StatusCheckResult(true);
                }
            }
        }

        return new StatusCheckResult(false, "Address " + address.getAddress() + " is missing connection from broker");
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{routerId=").append(routerId).append(",")
                .append("addresses=").append(addresses).append(",")
                .append("autoLinks=").append(autoLinks).append(",")
                .append("linkRoutes=").append(linkRoutes).append(",")
                .append("connections=").append(connections).append("}")
                .toString();
    }
}
