/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.ProtonRequestClient;
import io.enmasse.amqp.ProtonRequestClientOptions;
import io.enmasse.amqp.SyncRequestClient;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.engine.SslDomain;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class RouterStatusCollector {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusCollector.class);
    private final String certDir;

    public RouterStatusCollector(String certDir) {
        this.certDir = certDir;
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
            log.debug("Checking router status of router " + router.getStatus().getPodIP());
            SslDomain sslDomain = SslDomain.Factory.create();
            sslDomain.init(SslDomain.Mode.CLIENT);
            sslDomain.setTrustedCaDb(new File(certDir, "ca.crt").getAbsolutePath());
            sslDomain.setPeerAuthentication(SslDomain.VerifyMode.VERIFY_PEER);
            sslDomain.setCredentials(new File(certDir, "tls.crt").getAbsolutePath(), new File(certDir, "tls.key").getAbsolutePath(), null);
            ProtonRequestClientOptions clientOptions = new ProtonRequestClientOptions()
                    .setSaslEnabled(true)
                    .setSaslMechanisms(new String[]{"EXTERNAL"})
                    .setContainerId("router-status-checker")
                    .setSslEnabled(true)
                    .setSslDomain(sslDomain);
            try (ProtonRequestClient client = new ProtonRequestClient()) {
                CompletableFuture<Void> promise = new CompletableFuture<>();
                client.connect(router.getStatus().getPodIP(), port, clientOptions, "$management", promise);

                promise.get(10, TimeUnit.SECONDS);


                List<String> addresses = filterOnAttribute(collectRouter(client, "org.apache.qpid.dispatch.router.config.address",
                        Arrays.asList("prefix")), 0);

                List<List<String>> autoLinks = collectRouter(client, "org.apache.qpid.dispatch.router.config.autoLink",
                        Arrays.asList("addr", "containerId", "dir", "operStatus"));
                List<List<String>> linkRoutes = collectRouter(client, "org.apache.qpid.dispatch.router.config.linkRoute",
                        Arrays.asList("prefix", "containerId", "dir", "operStatus"));
                List<String> connections = filterOnAttribute(collectRouter(client, "org.apache.qpid.dispatch.connection",
                        Arrays.asList("container")), 0);


                String routerId = router.getMetadata().getName();
                return new RouterStatus(routerId, addresses, autoLinks, linkRoutes, connections);
            }
        } else {
            log.info("Unable to find appropriate router port, skipping address check");
            return null;
        }
    }

    private static List<String> filterOnAttribute(List<List<String>> list, int attrNum) {
        List<String> filtered = new ArrayList<>();
        for (List<String> entry : list) {
            filtered.add(entry.get(attrNum));
        }
        return filtered;
    }

    private static List<List<String>> collectRouter(SyncRequestClient client, String entityType, List<String> attributeNames) throws Exception {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("operation", "QUERY");
        properties.put("entityType", entityType);
        Map body = new LinkedHashMap<>();

        body.put("attributeNames", attributeNames);

        Message message = Proton.message();
        message.setApplicationProperties(new ApplicationProperties(properties));
        message.setBody(new AmqpValue(body));

        Message response = client.request(message, 10, TimeUnit.SECONDS);
        AmqpValue value = (AmqpValue) response.getBody();
        Map values = (Map) value.getValue();
        List<List<String>> results = (List<List<String>>) values.get("results");
        return results;
    }
}
