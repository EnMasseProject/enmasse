/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.RouterEntity;
import io.enmasse.amqp.RouterManagement;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RouterStatusCollector {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusCollector.class);
    private final RouterManagement routerManagement;

    public RouterStatusCollector(RouterManagement routerManagement) {
        this.routerManagement = routerManagement;
    }

    public RouterStatus collect(Pod router) throws Exception {
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
            return doCollectStatus(router, port);
        } else {
            log.info("Unable to find appropriate port for router {}, skipping address check", router.getMetadata().getName());
            return null;
        }
    }

    private static final RouterEntity address = new RouterEntity("org.apache.qpid.dispatch.router.config.address", "prefix");
    private static final RouterEntity autoLink = new RouterEntity("org.apache.qpid.dispatch.router.config.autoLink", "address", "containerId", "direction", "operStatus");

    private static final RouterEntity linkRoute = new RouterEntity("org.apache.qpid.dispatch.router.config.linkRoute", "prefix", "containerId", "direction", "operStatus");
    private static final RouterEntity connection = new RouterEntity("org.apache.qpid.dispatch.connection", "container");

    private static final RouterEntity[] entities = new RouterEntity[]{
            address,
            autoLink,
            linkRoute,
            connection
    };

    private RouterStatus doCollectStatus(Pod router, int port) throws Exception {
        String host = router.getStatus().getPodIP();
        log.debug("Checking router status of router : {}", router.getMetadata().getName());

        Map<RouterEntity, List<List>> results = routerManagement.query(host, port, entities);

        String routerId = router.getMetadata().getName();
        return new RouterStatus(routerId,
                filterOnAttribute(String.class, 0, results.get(address)),
                toTyped(String.class, results.get(autoLink)),
                toTyped(String.class, results.get(linkRoute)),
                filterOnAttribute(String.class, 0, results.get(connection)));
    }

    private static <T> List<List<T>> toTyped(Class<T> type, List<List> list) {
        List<List<T>> typed = new ArrayList<>();
        for (List<?> entry : list) {
            List<T> values = new ArrayList<>();
            for (Object value : entry) {
                values.add(type.cast(value));
            }
            typed.add(values);
        }
        return typed;
    }

    private static <T> List<T> filterOnAttribute(Class<T> type, int attrNum, List<List> list) {
        List<T> filtered = new ArrayList<>();
        for (List entry : list) {
            T filteredValue = type.cast(entry.get(attrNum));
            if (filteredValue != null) {
                filtered.add(filteredValue);
            }
        }
        return filtered;
    }
}
