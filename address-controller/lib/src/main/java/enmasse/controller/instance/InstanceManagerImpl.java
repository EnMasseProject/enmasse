package enmasse.controller.instance;

import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.common.Route;
import enmasse.controller.common.TemplateParameter;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.ParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InstanceManagerImpl implements InstanceManager {
    private final Kubernetes kubernetes;
    private final String instanceTemplateName;
    private final boolean isMultitenant;

    public InstanceManagerImpl(Kubernetes kubernetes, String instanceTemplateName, boolean isMultitenant) {
        this.kubernetes = kubernetes;
        this.instanceTemplateName = instanceTemplateName;
        this.isMultitenant = isMultitenant;
    }

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        if (isMultitenant) {
            Map<String, String> labelMap = new LinkedHashMap<>();
            labelMap.put(LabelKeys.INSTANCE, instanceId.getId());
            labelMap.put("app", "enmasse");
            labelMap.put("type", "instance");
            return list(labelMap).stream().findAny();
        } else {
            return Optional.of(buildInstance(instanceId));
        }
    }

    @Override
    public Optional<Instance> get(String uuid) {
        if (isMultitenant) {
            Map<String, String> labelMap = new LinkedHashMap<>();
            labelMap.put(LabelKeys.UUID, uuid);
            labelMap.put("app", "enmasse");
            labelMap.put("type", "instance");
            return list(labelMap).stream().findAny();
        } else {
            return Optional.empty();
        }
    }

    private Instance buildInstance(InstanceId instanceId) {
        List<Route> routes = kubernetes.getRoutes(instanceId);
        return new Instance.Builder(instanceId)
                .messagingHost(getRouteHost(routes, "messaging"))
                .mqttHost(getRouteHost(routes, "mqtt"))
                .consoleHost(getRouteHost(routes, "console"))
                .build();
    }

    private Set<Instance> list(Map<String, String> labelMap) {
        if (isMultitenant) {
            return kubernetes.listNamespaces(labelMap).stream()
                    .map(namespace -> InstanceId.withIdAndNamespace(namespace.getMetadata().getLabels().get("instance"), namespace.getMetadata().getName()))
                    .map(this::buildInstance)
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private Optional<String> getRouteHost(List<Route> routes, String routeName) {
        for (Route route : routes) {
            if (route.getName().equals(routeName)) {
                return Optional.ofNullable(route.getHostName());
            }
        }
        return Optional.empty();
    }

    @Override
    public void create(Instance instance) throws Exception {
        if (isMultitenant) {
            kubernetes.createNamespace(instance.id());
            kubernetes.addDefaultViewPolicy(instance.id());
        }

        String secretName = instance.certSecret().orElse(kubernetes.createInstanceSecret(instance.id()));
        Instance.Builder builder = new Instance.Builder(instance);
        builder.certSecret(Optional.ofNullable(secretName));
        instance = builder.build();

        KubernetesList items = createResourceList(instance, secretName);

        Kubernetes instanceClient = getClient(instance);
        instanceClient.create(items);

        kubernetes.create(kubernetes.createInstanceConfig(instance));
    }

    private KubernetesList createResourceList(Instance instance, String secretName) {
        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new ParameterValue(TemplateParameter.INSTANCE, Kubernetes.sanitizeName(instance.id().getId())));
        instance.messagingHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.MESSAGING_HOSTNAME, h)));
        instance.mqttHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.MQTT_HOSTNAME, h)));
        instance.consoleHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.CONSOLE_HOSTNAME, h)));
        parameterValues.add(new ParameterValue(TemplateParameter.ROUTER_SECRET, secretName));
        parameterValues.add(new ParameterValue(TemplateParameter.MQTT_SECRET, secretName));

        KubernetesList items = kubernetes.processTemplate(instanceTemplateName, parameterValues.toArray(new ParameterValue[0]));

        instance.uuid().ifPresent(uuid -> Kubernetes.addObjectLabel(items, LabelKeys.UUID, uuid));
        return items;
    }

    @Override
    public void delete(Instance instance) {
        if (getClient(instance).listClusters().isEmpty()) {
            kubernetes.deleteNamespace(instance.id().getNamespace());
            kubernetes.deleteInstanceConfig(instance);
        } else {
            throw new IllegalArgumentException("Instance " + instance.id() + " still has active destinations");
        }
    }

    @Override
    public Set<Instance> list() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("app", "enmasse");
        labelMap.put("type", "instance");
        return list(labelMap);
    }

    @Override
    public boolean isReady(Instance instance) throws IOException, InterruptedException {
        Set<String> readyDeployments = getClient(instance).getReadyDeployments().stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());

        Set<String> requiredDeployments = createResourceList(instance, "dummy-secret").getItems().stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        return readyDeployments.containsAll(requiredDeployments);
    }

    private Kubernetes getClient(Instance instance) {
        return kubernetes.mutateClient(instance.id());
    }
}
