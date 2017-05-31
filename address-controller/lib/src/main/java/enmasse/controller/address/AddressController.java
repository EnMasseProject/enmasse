package enmasse.controller.address;

import enmasse.controller.common.*;
import enmasse.controller.flavor.FlavorRepository;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Watches addresses
 */
public class AddressController extends AbstractVerticle implements Watcher<Instance> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final Kubernetes kubernetes;
    private final InstanceApi instanceApi;
    private final OpenShiftClient client;
    private final FlavorRepository flavorRepository;
    private final Map<Instance, String> addressSpaceMap = new HashMap<>();
    private Watch watch;

    public AddressController(InstanceApi instanceApi, Kubernetes kubernetes, OpenShiftClient client, FlavorRepository flavorRepository) {
        this.instanceApi = instanceApi;
        this.kubernetes = kubernetes;
        this.client = client;
        this.flavorRepository = flavorRepository;
    }

    @Override
    public void start() throws Exception {
        this.watch = instanceApi.watchInstances(this);
    }

    @Override
    public void stop() throws Exception {
        if (watch != null) {
            watch.close();
        }
    }

    @Override
    public synchronized void resourcesUpdated(Set<Instance> instances) throws Exception {
        log.debug("Check instances in address controller: " + instances);
        for (Instance instance : instances) {
            if (!addressSpaceMap.containsKey(instance)) {
                DestinationClusterGenerator clusterGenerator = new TemplateDestinationClusterGenerator(instance, kubernetes, flavorRepository);
                AddressSpaceController addressSpaceController = new AddressSpaceController(
                        instanceApi.withInstance(instance.id()),
                        kubernetes.withInstance(instance.id()),
                        client,
                        clusterGenerator);
                log.info("Deploying address space controller for " + instance.id());
                vertx.deployVerticle(addressSpaceController, result -> {
                    if (result.succeeded()) {
                        addressSpaceMap.put(instance, result.result());
                    } else {
                        log.warn("Unable to deploy address controller for " + instance.id());
                    }
                });
            }
        }

        Iterator<Map.Entry<Instance, String>> it = addressSpaceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Instance, String> entry = it.next();
            if (!instances.contains(entry.getKey())) {
                vertx.undeploy(entry.getValue());
                it.remove();
            }
        }
    }
}
