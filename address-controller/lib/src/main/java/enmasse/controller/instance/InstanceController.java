package enmasse.controller.instance;

import enmasse.config.LabelKeys;
import enmasse.controller.common.ConfigWatcher;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.instance.cert.CertManager;
import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The instance controller is responsible for watching instance that should be created, as well as
 * propagating any relevant status for a given instance back to the instance resource.
 */

public class InstanceController extends ConfigWatcher<Instance> {
    private static final Logger log = LoggerFactory.getLogger(InstanceController.class.getName());
    private final OpenShiftClient client;

    private final CertManager certManager;
    private final InstanceManager instanceManager;
    private final InstanceApi instanceApi;

    public InstanceController(InstanceManager instanceManager, OpenShiftClient client, InstanceApi instanceApi, CertManager certManager) {
        super(Collections.singletonMap(LabelKeys.TYPE, "instance-config"), client.getNamespace(), client);
        this.instanceManager = instanceManager;
        this.client = client;
        this.instanceApi = instanceApi;
        this.certManager = certManager;
    }

    @Override
    protected synchronized void checkConfigs(Set<Instance> instances) throws Exception {
        log.debug("Check instances in instance controller: " + instances);
        createInstances(instances);
        retainInstances(instances);

        for (Instance instance : instanceApi.listInstances()) {
            certManager.updateCerts(instance);
            Instance.Builder mutableInstance = new Instance.Builder(instance);
            updateReadiness(mutableInstance);
            updateRoutes(mutableInstance);
            instanceApi.replaceInstance(mutableInstance.build());
        }
    }

    @Override
    protected Set<Instance> listConfigs() throws Exception {
        return instanceApi.listInstances();
    }

    private void retainInstances(Set<Instance> desiredInstances) {
        String[] ids = desiredInstances.stream()
                .map(instance -> instance.id().getId())
                .collect(Collectors.toList()).toArray(new String[0]);


        try {
            client.namespaces().withLabelNotIn(LabelKeys.INSTANCE, ids).withLabel(LabelKeys.APP, "enmasse").withLabel(LabelKeys.TYPE, "instance").delete();
        } catch (KubernetesClientException e) {
            log.info("Exception when deleting namespace (may already be in progress): " + e.getMessage());
        }
    }

    private void createInstances(Set<Instance> instances) {
        for (Instance instance : instances) {
            instanceManager.create(instance);
        }
    }

    private void updateRoutes(Instance.Builder instance) throws IOException {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(LabelKeys.INSTANCE, instance.id().getId());

        /* Watch for routes and ingress */
        if (client.isAdaptable(OpenShiftClient.class)) {
            updateRoutes(instance, client.routes().inNamespace(instance.id().getNamespace()).withLabels(labelMap).list());
        } else {
            updateIngresses(instance, client.extensions().ingresses().inNamespace(instance.id().getNamespace()).withLabels(labelMap).list());
        }
    }

    private void updateIngresses(Instance.Builder instance, IngressList list) {
        for (Ingress ingress : list.getItems()) {
            updateRoute(instance, ingress.getMetadata().getName(), ingress.getSpec().getRules().get(0).getHost());
        }
    }

    private void updateRoutes(Instance.Builder instance, RouteList list) throws IOException {
        for (Route route : list.getItems()) {
            updateRoute(instance, route.getMetadata().getName(), route.getSpec().getHost());
        }
    }

    private void updateRoute(Instance.Builder builder, String name, String host) {
        log.debug("Updating routes for " + name + " to " + host);
        if ("messaging".equals(name)) {
            builder.messagingHost(Optional.of(host));
        } else if ("mqtt".equals(name)) {
            builder.mqttHost(Optional.of(host));
        } else if ("console".equals(name)) {
            builder.consoleHost(Optional.of(host));
        }
    }

    private void updateReadiness(Instance.Builder mutableInstance) {
        Instance instance = mutableInstance.build();
        boolean isReady = instanceManager.isReady(instance);
        if (instance.status().isReady() != isReady) {
            mutableInstance.status(new Instance.Status(isReady));
        }
    }
}
