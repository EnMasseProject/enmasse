package enmasse.controller.instance;

import enmasse.config.AnnotationKeys;
import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.common.TemplateParameter;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class InstanceManagerImpl implements InstanceManager {
    private static final Logger log = LoggerFactory.getLogger(InstanceManagerImpl.class.getName());
    private final Kubernetes kubernetes;
    private final String instanceTemplateName;
    private final boolean isMultitenant;
    private final String namespace;

    public InstanceManagerImpl(Kubernetes kubernetes, String instanceTemplateName, boolean isMultitenant, String namespace) {
        this.kubernetes = kubernetes;
        this.instanceTemplateName = instanceTemplateName;
        this.isMultitenant = isMultitenant;
        this.namespace = namespace;
    }

    @Override
    public void create(Instance instance) {
        Kubernetes instanceClient = kubernetes.withInstance(instance.id());
        if (instanceClient.hasService("messaging")) {
            return;
        }
        log.info("Creating instance {}", instance);
        if (isMultitenant) {
            kubernetes.createNamespace(instance.id());
            kubernetes.addDefaultViewPolicy(instance.id());
        }

        kubernetes.createInstanceSecret(instance.certSecret(), instance.id());

        instanceClient.create(createResourceList(instance));
    }

    private KubernetesList createResourceList(Instance instance) {
        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new ParameterValue(TemplateParameter.INSTANCE, instance.id().getId()));
        instance.messagingHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.MESSAGING_HOSTNAME, h)));
        instance.mqttHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.MQTT_HOSTNAME, h)));
        instance.consoleHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.CONSOLE_HOSTNAME, h)));
        parameterValues.add(new ParameterValue(TemplateParameter.ROUTER_SECRET, instance.certSecret()));
        parameterValues.add(new ParameterValue(TemplateParameter.MQTT_SECRET, instance.certSecret()));
        parameterValues.add(new ParameterValue(TemplateParameter.ADDRESS_SPACE_SERVICE_HOST, getApiServer()));

        KubernetesList items = kubernetes.processTemplate(instanceTemplateName, parameterValues.toArray(new ParameterValue[0]));

        instance.uuid().ifPresent(uuid -> Kubernetes.addObjectLabel(items, LabelKeys.UUID, uuid));
        return items;
    }

    private String getApiServer() {
        return "address-controller." +
                namespace +
                ".svc.cluster.local";
    }

    @Override
    public boolean isReady(Instance instance) {
        Set<String> readyDeployments = kubernetes.withInstance(instance.id()).getReadyDeployments().stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());

        Set<String> requiredDeployments = createResourceList(instance).getItems().stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        return readyDeployments.containsAll(requiredDeployments);
    }

    @Override
    public void retainInstances(Set<InstanceId> desiredInstances) {
        if (isMultitenant) {
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put(LabelKeys.APP, "enmasse");
            labels.put(LabelKeys.TYPE, "instance");
            for (Namespace namespace : kubernetes.listNamespaces(labels)) {
                String id = namespace.getMetadata().getAnnotations().get(AnnotationKeys.INSTANCE);
                InstanceId instanceId = InstanceId.withIdAndNamespace(id, namespace.getMetadata().getName());
                if (!desiredInstances.contains(instanceId)) {
                    try {
                        delete(instanceId);
                    } catch(KubernetesClientException e){
                        log.info("Exception when deleting namespace (may already be in progress): " + e.getMessage());
                    }
                }
            }
        }
    }

    private void delete(InstanceId instanceId) {
        if (kubernetes.withInstance(instanceId).listClusters().isEmpty()) {
            kubernetes.deleteNamespace(instanceId.getNamespace());
        } else {
            log.warn("Instance {} still has active destinations, not deleting", instanceId);
        }
    }
}
