/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.enmasse.address.model.*;

class RouterStatus {
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
        boolean found = addresses.contains(address.getSpec().getAddress());
        if (!found) {
            address.getStatus().setReady(false).appendMessage("Address " + address.getSpec().getAddress() + " not found on " + routerId);
        } else {
            ok++;
        }
        return ok;
    }

    public int checkAutoLinks(Address address) {

        int ok = 0;
        for (List<String> autoLink : autoLinks) {
            String addr = autoLink.get(0);

            if (addr.equals(address.getSpec().getAddress())) {
                ok++;
            }
        }

        if (ok < 2) {
            address.getStatus().setReady(false).appendMessage("Address " + address.getSpec().getAddress() + " is missing autoLinks on " + routerId);
        }

        return ok;
    }

    public int checkLinkRoutes(Address address) {
        int ok = 0;

        for (List<String> linkRoute : linkRoutes) {
            if (linkRoute.size() > 0) {
                String prefix = linkRoute.get(0);

                // Pooled topics have active link routes
                if (prefix.equals(address.getSpec().getAddress())) {
                    ok++;
                }
            }
        }

        if (ok < 2) {
            address.getStatus().setReady(false).appendMessage("Address " + address.getSpec().getAddress() + " is missing linkRoutes on " + routerId);
        }

        return ok;
    }

    public static int checkActiveAutoLink(Address address, List<RouterStatus> routerStatusList) {
        int ok = 0;
        Set<String> active = new HashSet<>();

        for (RouterStatus routerStatus : routerStatusList) {

            for (List<String> autoLink : routerStatus.autoLinks) {
                if (autoLink.size() > 3) {
                    String addr = autoLink.get(0);
                    String dir = autoLink.get(2);
                    String operStatus = autoLink.get(3);

                    if (addr.equals(address.getSpec().getAddress()) && operStatus.equals("active")) {
                        active.add(dir);
                    }
                }
            }
        }

        if (active.size() < 2) {
            address.getStatus().setReady(false).appendMessage("Address " + address.getSpec().getAddress() + " is missing active autoLink (active in dirs: " + active + ")");
        } else {
            ok++;
        }
        return ok;
    }

    public static int checkActiveLinkRoute(Address address, List<RouterStatus> routerStatusList) {
        int ok = 0;
        Set<String> active = new HashSet<>();

        for (RouterStatus routerStatus : routerStatusList) {

            for (List<String> linkRoute : routerStatus.linkRoutes) {
                if (linkRoute.size() > 3) {
                    String addr = linkRoute.get(0);
                    @SuppressWarnings("unused")
                    String containerId = linkRoute.get(1);
                    String dir = linkRoute.get(2);
                    String operStatus = linkRoute.get(3);

                    if (addr.equals(address.getSpec().getAddress()) && operStatus.equals("active")) {
                        active.add(dir);
                    }
                }
            }
        }

        if (active.size() < 2) {
            address.getStatus().setReady(false).appendMessage("Address " + address.getSpec().getAddress() + " is missing active linkRoute (active in dirs: " + active + ")");
        } else {
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

        if (ok == 0) {
            address.getStatus().setReady(false).appendMessage("Address " + address.getSpec().getAddress() + " is missing connection from broker");
        }
        return ok;
    }

    public static int checkForwarderLinks(Address address, List<RouterStatus> routerStatusList) {
        if (address.getSpec().getForwarders() == null || address.getSpec().getForwarders().isEmpty()) {
            return 0;
        }

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

            if (!found) {
                updateForwarderStatus(forwarder.getName(), false, "Unable to find link for forwarder '" + forwarder.getName() + "'", address.getStatus().getForwarderStatuses());
            } else if (!isUp) {
                updateForwarderStatus(forwarder.getName(), false, "Unable to find link in the up state for forwarder '" + forwarder.getName() + "'", address.getStatus().getForwarderStatuses());
            } else {
                ok++;
            }
        }
        return ok;
    }

    private static void updateForwarderStatus(String name, boolean isReady, String message, List<AddressStatusForwarder> forwarderStatuses) {
        forwarderStatuses.stream()
                .filter(c -> c.getName().equals(name))
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
