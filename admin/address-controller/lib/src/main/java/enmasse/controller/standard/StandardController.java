package enmasse.controller.standard;

import enmasse.config.AnnotationKeys;
import enmasse.controller.common.*;
import enmasse.controller.k8s.api.AddressSpaceApi;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class StandardController extends AbstractVerticle implements Watcher<AddressSpace> {
    private static final Logger log = LoggerFactory.getLogger(StandardController.class.getName());
    private final OpenShiftClient client;

    private final StandardHelper helper;
    private final AddressSpaceApi addressSpaceApi;
    private Watch watch;

    private final Map<AddressSpace, String> addressControllerMap = new HashMap<>();
    private final Kubernetes kubernetes;

    public StandardController(StandardHelper helper, OpenShiftClient client, AddressSpaceApi addressSpaceApi, Kubernetes kubernetes) {
        this.helper = helper;
        this.client = client;
        this.addressSpaceApi = addressSpaceApi;
        this.kubernetes = kubernetes;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(addressSpaceApi.watchAddressSpaces(this));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                this.watch = result.result();
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        vertx.executeBlocking(promise -> {
            try {
                if (watch != null) {
                    watch.close();
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                stopFuture.complete();
            } else {
                stopFuture.fail(result.cause());
            }
        });
    }

    @Override
    public synchronized void resourcesUpdated(Set<AddressSpace> instances) throws Exception {
        log.debug("Check standard address spaces: " + instances);
        createAddressSpaces(instances);
        retainAddressSpaces(instances);

        for (AddressSpace instance : addressSpaceApi.listAddressSpaces()) {
            AddressSpace.Builder mutableAddressSpace = new AddressSpace.Builder(instance);
            updateReadiness(mutableAddressSpace);
            updateEndpoints(mutableAddressSpace);
            addressSpaceApi.replaceAddressSpace(mutableAddressSpace.build());
        }
        
        createAddressControllers(instances);
        deleteAddressControllers(instances);
    }

    private void createAddressControllers(Set<AddressSpace> addressSpaces) {
        for (AddressSpace addressSpace : addressSpaces) {
            if (!addressControllerMap.containsKey(addressSpace)) {
                AddressClusterGenerator clusterGenerator = new TemplateAddressClusterGenerator(addressSpace, kubernetes);
                AddressController addressController = new AddressController(
                        addressSpaceApi.withAddressSpace(addressSpace),
                        kubernetes.withNamespace(addressSpace.getNamespace()),
                        clusterGenerator);
                log.info("Deploying address space controller for " + addressSpace.getName());
                vertx.deployVerticle(addressController, result -> {
                    if (result.succeeded()) {
                        addressControllerMap.put(addressSpace, result.result());
                    } else {
                        log.warn("Unable to deploy address controller for " + addressSpace.getName());
                    }
                });
            }
        }
    }

    private void deleteAddressControllers(Set<AddressSpace> addressSpaces) {
        Iterator<Map.Entry<AddressSpace, String>> it = addressControllerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AddressSpace, String> entry = it.next();
            if (!addressSpaces.contains(entry.getKey())) {
                vertx.undeploy(entry.getValue());
                it.remove();
            }
        }
    }

    private void retainAddressSpaces(Set<AddressSpace> desiredAddressSpaces) {
        helper.retainAddressSpaces(desiredAddressSpaces.stream().map(AddressSpace::getName).collect(Collectors.toSet()));
    }

    private void createAddressSpaces(Set<AddressSpace> instances) {
        for (AddressSpace instance : instances) {
            // TODO: Ugh, find a way to do this as part of the main controller loop
            AddressSpace modified = helper.create(instance);
            if (modified != null) {
                addressSpaceApi.replaceAddressSpace(modified);
            }
        }
    }

    private void updateEndpoints(AddressSpace.Builder builder) throws IOException {

        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.ADDRESS_SPACE, builder.getName());

        /* Watch for routes and ingress */
        if (client.isAdaptable(OpenShiftClient.class)) {
            client.routes().inNamespace(builder.getNamespace()).list().getItems().stream()
                    .filter(route -> isPartOfAddressSpace(builder.getName(), route))
                    .forEach(route -> updateRoute(builder, route.getMetadata().getName(), route.getSpec().getHost()));
        } else {
            client.extensions().ingresses().inNamespace(builder.getNamespace()).list().getItems().stream()
                    .filter(ingress -> isPartOfAddressSpace(builder.getName(), ingress))
                    .forEach(ingress -> updateRoute(builder, ingress.getMetadata().getName(), ingress.getSpec().getRules().get(0).getHost()));
        }
    }

    private static boolean isPartOfAddressSpace(String id, HasMetadata resource) {
        return resource.getMetadata().getAnnotations() != null && id.equals(resource.getMetadata().getAnnotations().get(AnnotationKeys.ADDRESS_SPACE));
    }

    private void updateRoute(AddressSpace.Builder builder, String name, String host) {
        log.debug("Updating routes for " + name + " to " + host);
        List<Endpoint> updated = new ArrayList<>();
        for (Endpoint endpoint : builder.getEndpoints()) {
            if (endpoint.getName().equals(name)) {
                updated.add(new Endpoint.Builder(endpoint)
                        .setHost(host)
                        .build());
            } else {
                updated.add(endpoint);
            }
        }
    }

    private void updateReadiness(AddressSpace.Builder mutableAddressSpace) {
        AddressSpace instance = mutableAddressSpace.build();
        boolean isReady = helper.isReady(instance);
        if (mutableAddressSpace.getStatus().isReady() != isReady) {
            mutableAddressSpace.getStatus().setReady(isReady);
        }
    }
}
