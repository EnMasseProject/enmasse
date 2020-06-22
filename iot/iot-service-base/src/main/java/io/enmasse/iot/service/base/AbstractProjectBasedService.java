/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import io.enmasse.common.model.CustomResources;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.model.v1.IoTProjectBuilder;
import io.enmasse.iot.model.v1.IoTProjectList;
import io.enmasse.iot.utils.ConfigBase;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.vertx.core.Future;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Optional.ofNullable;

public abstract class AbstractProjectBasedService extends AbstractKubernetesBasedService {

    private static final Duration DEFAULT_RESYNC_PERIOD = Duration.ofMinutes(5);

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractProjectBasedService.class);

    private final Map<String, IoTProject> projects = new ConcurrentHashMap<>();

    private SharedInformerFactory factory;

    /**
     * The resync period of the IoTProject informer.
     */
    private Duration resyncPeriod = DEFAULT_RESYNC_PERIOD;

    @ConfigProperty(name = ConfigBase.CONFIG_BASE + "kubernetes.informer.resyncPeriod")
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

        // CustomResourceDefinitions.registerAll();

        // create a new informer factory
        this.factory = getClient().inAnyNamespace().informers();

        // setup informer
        var informer = this.factory.sharedIndexInformerForCustomResource(
                CustomResources.toContext(IoTCrd.project()),
                IoTProject.class, IoTProjectList.class, this.resyncPeriod.toMillis());

        informer.addEventHandler(new ResourceEventHandler<IoTProject>() {

                    @Override
                    public void onUpdate(final IoTProject oldObj, final IoTProject newObj) {
                        try {
                            AbstractProjectBasedService.this.onUpdate(newObj);
                        } catch (Exception e) {
                            log.warn("Failed to run onUpdate()", e);
                        }
                    }

                    @Override
                    public void onDelete(final IoTProject obj, final boolean deletedFinalStateUnknown) {
                        try {
                            AbstractProjectBasedService.this.onDelete(obj);
                        } catch (Exception e) {
                            log.warn("Failed to run onDelete()", e);
                        }
                    }

                    @Override
                    public void onAdd(final IoTProject obj) {
                        try {
                            AbstractProjectBasedService.this.onAdd(obj);
                        } catch (Exception e) {
                            log.warn("Failed to run onAdd()", e);
                        }
                    }
                });

        // this only starts informers created by this factory instance
        this.factory.startAllRegisteredInformers();
    }

    protected void stopWatcher() {
        if (this.factory != null) {
            this.factory.stopAllRegisteredInformers();
            this.factory = null;
        }
    }

    /**
     * Lookup a project asynchronously.
     * <p>
     * The method will return a copy of the project, which can be freely edited
     * without modifying the internal copy.
     *
     * @param tenantName The name of the tenant to look up.
     * @return A future reporting the result.
     */
    protected Future<Optional<IoTProject>> getProject(final String tenantName) {
        return succeededFuture(
                ofNullable(this.projects.get(tenantName))
                        .map(project -> new IoTProjectBuilder(project).build()));
    }

    protected void onAdd(final IoTProject project) {
        final String key = key(project);
        log.info("Added project: {}", key);
        this.projects.put(key, project);
    }

    protected void onUpdate(final IoTProject project) {
        final String key = key(project);
        log.info("Modified project: {}", key);
        this.projects.put(key, project);
    }

    protected void onDelete(final IoTProject project) {
        final String key = key(project);
        log.info("Removed project: {}", key);
        this.projects.remove(key);
    }

}
