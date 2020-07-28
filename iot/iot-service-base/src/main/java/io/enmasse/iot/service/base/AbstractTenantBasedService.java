/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Optional.ofNullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.enmasse.iot.model.v1.IoTTenantBuilder;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.enmasse.common.model.CustomResources;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTTenant;
import io.enmasse.iot.model.v1.IoTTenantList;
import io.enmasse.iot.utils.ConfigBase;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.vertx.core.Future;

public abstract class AbstractTenantBasedService extends AbstractKubernetesBasedService {

    private static final Duration DEFAULT_RESYNC_PERIOD = Duration.ofMinutes(5);

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractTenantBasedService.class);

    private final Map<String, IoTTenant> tenants = new ConcurrentHashMap<>();

    private SharedInformerFactory factory;

    /**
     * The resync period of the IoTTenant informer.
     */
    private Duration resyncPeriod = DEFAULT_RESYNC_PERIOD;

    @ConfigurationProperties(ConfigBase.CONFIG_BASE + "kubernetes.informer.resyncPeriod")
    public void setResyncPeriod(final Duration resyncPeriod) {
        this.resyncPeriod = resyncPeriod != null ? resyncPeriod : DEFAULT_RESYNC_PERIOD;
    }

    /**
     * Generates a tenant name from an IoT project.
     *
     * @param resource The project to use.
     * @return The tenant name.
     */
    protected static String key(final IoTTenant resource) {
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
                CustomResources.toContext(IoTCrd.tenant()),
                IoTTenant.class, IoTTenantList.class, this.resyncPeriod.toMillis())
                .addEventHandler(new ResourceEventHandler<IoTTenant>() {

                    @Override
                    public void onUpdate(final IoTTenant oldObj, final IoTTenant newObj) {
                        try {
                            AbstractTenantBasedService.this.onUpdate(newObj);
                        } catch (Exception e) {
                            log.warn("Failed to run onUpdate()", e);
                        }
                    }

                    @Override
                    public void onDelete(final IoTTenant obj, final boolean deletedFinalStateUnknown) {
                        try {
                            AbstractTenantBasedService.this.onDelete(obj);
                        } catch (Exception e) {
                            log.warn("Failed to run onDelete()", e);
                        }
                    }

                    @Override
                    public void onAdd(final IoTTenant obj) {
                        try {
                            AbstractTenantBasedService.this.onAdd(obj);
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
    protected Future<Optional<IoTTenant>> getTenant(final String tenantName) {
        return succeededFuture(
                ofNullable(this.tenants.get(tenantName))
                        .map(project -> new IoTTenantBuilder(project).build()));
    }

    protected void onAdd(final IoTTenant project) {
        final String key = key(project);
        log.info("Added tenant: {}", key);
        this.tenants.put(key, project);
    }

    protected void onUpdate(final IoTTenant project) {
        final String key = key(project);
        log.info("Modified tenant: {}", key);
        this.tenants.put(key, project);
    }

    protected void onDelete(final IoTTenant project) {
        final String key = key(project);
        log.info("Removed tenant: {}", key);
        this.tenants.remove(key);
    }

}
