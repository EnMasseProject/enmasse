/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import io.enmasse.iot.model.v1.IoTProject;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.vertx.core.Future;

public abstract class AbstractProjectBasedService extends AbstractKubernetesBasedService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractProjectBasedService.class);

    private final Map<String, IoTProject> projects = new ConcurrentHashMap<>();

    private Watch watcher;

    /**
     * Generates a tenant name from an IoT project.
     *
     * @param resource The project to use.
     * @return The tenant name.
     */
    protected static String key(final IoTProject resource) {
        return resource.getMetadata().getNamespace() + "." + resource.getMetadata().getName();
    }

    @Override
    protected Future<?> doStart() {
        return super.doStart()
                .compose(x -> runBlocking(this::startWatcher));
    }

    @Override
    protected Future<?> doStop() {
        return super.doStop()
                .compose(x -> runBlocking(this::stopWatcher));
    }

    protected void startWatcher() {

        log.info("Starting project watcher");

        this.watcher = IoTProjects.clientForProject(getClient())

                .watch(new Watcher<>() {

                    @Override
                    public void eventReceived(final Action action, final IoTProject project) {
                        log.debug("Watcher event - action: {}, project: {}", action, project);
                        switch (action) {
                            case ADDED: {
                                final String key = key(project);
                                log.info("Added project: {}", key);
                                projects.put(key, project);
                                break;
                            }
                            case DELETED: {
                                final String key = key(project);
                                log.info("Removed project: {}", key);
                                projects.remove(key);
                                break;
                            }
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onClose(final KubernetesClientException cause) {
                        log.info("Watcher closed", cause);
                    }

                });

    }

    protected void stopWatcher() {
        if (this.watcher == null) {
            return;
        }

        try {
            this.watcher.close();
        } finally {
            this.watcher = null;
        }
    }

    /**
     * Lookup a project asynchronously.
     *
     * @param tenantName The name of the tenant to look up.
     * @return A future reporting the result.
     */
    protected CompletableFuture<Optional<IoTProject>> getProject(final String tenantName) {
        return completedFuture(ofNullable(this.projects.get(tenantName)));
    }

}
