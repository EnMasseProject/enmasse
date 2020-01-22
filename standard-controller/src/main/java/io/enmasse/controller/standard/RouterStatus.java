/*
 * Copyright 2016-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpecForwarder;
import io.enmasse.address.model.AddressStatusForwarder;
import io.enmasse.address.model.BrokerStatus;

class RouterStatus {
    private static final Logger log = LoggerFactory.getLogger(RouterStatus.class);

    private final String routerId;
    private final List<String> addresses;
    private final List<List<String>> autoLinks;
    private final List<List<String>> linkRoutes;
    private final List<String> connections;
    private final List<List<String>> links;

    RouterStatus(String routerId, List<String> addresses, List<List<String>> autoLinks, List<List<String>> linkRoutes, List<String> connections, List<List<String>> links) {
        this.routerId = routerId;
        this.addresses = addresses;
        this.autoLinks = autoLinks;
        this.linkRoutes = linkRoutes;
        this.connections = connections;
        this.links = links;
    }

    public String getRouterId() {
        return routerId;
    }

    public int checkAddress(Address address) {

        int ok = 0;
        final String addressName = address.getSpec().getAddress();

        boolean found = this.addresses.contains(addressName);
        if (!found) {
            var msg = "Address " + addressName + " not found on " + this.routerId;
            log.debug(msg);
            address.getStatus().setReady(false).appendMessage(msg);
        } else {
            ok++;
            log.debug("Address found for '{}'", addressName);
        }

        return ok;
    }

    public int checkAutoLinks(Address address) {

        int ok = 0;
        final String addressName = address.getSpec().getAddress();

        for (List<String> autoLink : autoLinks) {
            String addr = autoLink.get(0);

            if (addr.equals(addressName)) {
                ok++;
            }
        }

        if (ok < 2) {
            var msg = "Address " + addressName + " is missing autoLinks on " + routerId;
            log.debug(msg);
            address.getStatus().setReady(false).appendMessage(msg);
        } else {
            log.debug("Address {} has all required auto links: {}", addressName);
        }

        return ok;
    }

    public int checkLinkRoutes(Address address) {
        int ok = 0;
        final String addressName = address.getSpec().getAddress();

        for (List<String> linkRoute : linkRoutes) {
            if (linkRoute.size() > 0) {
                String prefix = linkRoute.get(0);

                // Pooled topics have active link routes
                if (prefix.equals(addressName)) {
                    ok++;
                }
            }
        }

        if (ok < 2) {
            var msg = "Address " + addressName + " is missing linkRoutes on " + this.routerId;
            log.debug(msg);
            address.getStatus().setReady(false).appendMessage(msg);
        } else {
            log.debug("Address {} has all required link routes: {}", addressName);
        }

        return ok;
    }

    public static int checkActiveAutoLink(Address address, List<RouterStatus> routerStatusList) {
        int ok = 0;
        final Set<String> active = new HashSet<>();
        final String addressName = address.getSpec().getAddress();

        for (RouterStatus routerStatus : routerStatusList) {

            log.debug("Router Auto Links: {}", routerStatus.autoLinks);

            for (List<String> autoLink : routerStatus.autoLinks) {
                if (autoLink.size() > 3) {
                    String addr = autoLink.get(0);
                    String dir = autoLink.get(2);
                    String operStatus = autoLink.get(3);

                    log.debug("Addr: {}, Dir: {}, operStatus: {}", addr, dir, operStatus);

                    if (addr.equals(addressName) && operStatus.equals("active")) {
                        active.add(dir);
                        log.debug("  Match!");
                    }
                }
            }
        }

        if (active.size() < 2) {
            var msg = "Address " + addressName + " is missing active autoLink (active in dirs: " + active + ")";
            log.debug(msg);
            address.getStatus().setReady(false).appendMessage(msg);
        } else {
            log.debug("Address {} has all required auto links: {}", addressName, active);
            ok++;
        }
        return ok;
    }

    public static int checkActiveLinkRoute(Address address, List<RouterStatus> routerStatusList) {
        int ok = 0;
        Set<String> active = new HashSet<>();
        final String addressName = address.getSpec().getAddress();

        for (RouterStatus routerStatus : routerStatusList) {

            for (List<String> linkRoute : routerStatus.linkRoutes) {
                if (linkRoute.size() > 3) {
                    String addr = linkRoute.get(0);
                    @SuppressWarnings("unused")
                    String containerId = linkRoute.get(1);
                    String dir = linkRoute.get(2);
                    String operStatus = linkRoute.get(3);

                    if (addr.equals(addressName) && operStatus.equals("active")) {
                        active.add(dir);
                    }
                }
            }
        }

        if (active.size() < 2) {
            var msg = "Address " + addressName + " is missing active linkRoute (active in dirs: " + active + ")";
            log.debug(msg);
            address.getStatus().setReady(false).appendMessage(msg);
        } else {
            log.debug("Address {} has all required link routes: {}", addressName, active);
            ok++;
        }
        return ok;
    }

    public static int checkConnection(Address address, List<RouterStatus> routerStatusList) {
        int ok = 0;
        for (RouterStatus routerStatus : routerStatusList) {
            for (String containerId : routerStatus.connections) {
                boolean found = false;
                for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
                    if (containerId.startsWith(brokerStatus.getClusterId())) {
                        ok++;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        final String addressName = address.getSpec().getAddress();
        if (ok == 0) {
            var msg = "Address " + addressName + " is missing connection from broker";
            log.debug(msg);
            address.getStatus().setReady(false).appendMessage(msg);
        } else {
            log.debug("Address {} has all required connections", addressName);
        }
        return ok;
    }

    public static int checkForwarderLinks(Address address, List<RouterStatus> routerStatusList) {
        if (address.getSpec().getForwarders() == null || address.getSpec().getForwarders().isEmpty()) {
            return 0;
        }

        final String addressName = address.getSpec().getAddress();

        int ok = 0;
        for (AddressSpecForwarder forwarder : address.getSpec().getForwarders()) {
            boolean found = false;
            boolean isUp = false;
            for (RouterStatus routerStatus : routerStatusList) {
                for (List<String> entry : routerStatus.links) {
                    String linkName = entry.get(0);
                    if (linkName.equals(address.getForwarderLinkName(forwarder))) {
                        found = true;
                        String operStatus = entry.get(1);

                        if ("up".equals(operStatus)) {
                            isUp = true;
                        }
                        break;
                    }
                }
            }

            final String forwarderName = forwarder.getName();
            if (!found) {
                updateForwarderStatus(addressName, forwarderName, false, "Unable to find link for forwarder '" + forwarderName + "'", address.getStatus().getForwarders());
            } else if (!isUp) {
                updateForwarderStatus(addressName, forwarderName, false, "Unable to find link in the up state for forwarder '" + forwarderName + "'", address.getStatus().getForwarders());
            } else {
                ok++;
                log.debug("Forwarder {} for address {} is ok", forwarder.getName(), address.getSpec().getAddress());
            }
        }
        return ok;
    }

    private static void updateForwarderStatus(String addressName, String forwarderName, boolean isReady, String message, List<AddressStatusForwarder> forwarderStatuses) {
        log.debug("Address: {} - {}", addressName, message);
        forwarderStatuses.stream()
                .filter(c -> c.getName().equals(forwarderName))
                .findFirst().ifPresent(s -> {
            s.setReady(isReady);
            s.appendMessage(message);
        });
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{routerId=").append(routerId).append(",")
                .append("addresses=").append(addresses).append(",")
                .append("autoLinks=").append(autoLinks).append(",")
                .append("linkRoutes=").append(linkRoutes).append(",")
                .append("connections=").append(connections).append(",")
                .append("links=").append(links).append("}")
                .toString();
    }
}
