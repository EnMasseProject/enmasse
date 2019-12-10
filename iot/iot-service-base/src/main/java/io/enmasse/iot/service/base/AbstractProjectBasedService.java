/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.enmasse.common.model.CustomResources;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.vertx.core.Future;

public abstract class AbstractProjectBasedService extends AbstractKubernetesBasedService {

    private static final Duration DEFAULT_RESYNC_PERIOD = Duration.ofMinutes(5);

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractProjectBasedService.class);

    private final Map<String, IoTProject> projects = new ConcurrentHashMap<>();

    private SharedInformerFactory factory;

    /**
     * The resync period of the IoTProject informer.
     */
    private Duration resyncPeriod = DEFAULT_RESYNC_PERIOD;

    @ConfigurationProperties(ServiceBase.CONFIG_BASE + "kubernetes.informer.resyncPeriod")
    public void setResyncPeriod(final Duration resyncPeriod) {
        this.resyncPeriod = resyncPeriod != null ? resyncPeriod : DEFAULT_RESYNC_PERIOD;
    }

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

        // create a new informer factory
        this.factory = getClient().inAnyNamespace().informers();

        // setup informer
        this.factory.sharedIndexInformerForCustomResource(
                CustomResources.toContext(IoTCrd.project()),
                IoTProject.class, IoTProjectList.class, this.resyncPeriod.toMillis())
                .addEventHandler(new ResourceEventHandler<IoTProject>() {

                    @Override
                    public void onUpdate(final IoTProject oldObj, final IoTProject newObj) {
                        final String key = key(newObj);
                        log.info("Modified project: {}", key);
                        projects.put(key, newObj);
                    }

                    @Override
                    public void onDelete(final IoTProject obj, final boolean deletedFinalStateUnknown) {
                        final String key = key(obj);
                        log.info("Removed project: {}", key);
                        projects.remove(key);
                    }

                    @Override
                    public void onAdd(final IoTProject obj) {
                        final String key = key(obj);
                        log.info("Added project: {}", key);
                        projects.put(key, obj);
                    }
                });

        // this only starts informers created by this factory instance
        this.factory.startAllRegisteredInformers();
    }

    protected void stopWatcher() {
        if (this.factory != null) {
            this.factory = null;
            this.factory.stopAllRegisteredInformers();
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
