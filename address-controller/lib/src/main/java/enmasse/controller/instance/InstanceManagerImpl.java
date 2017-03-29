package enmasse.controller.instance;

import enmasse.controller.common.OpenShift;
import enmasse.controller.common.TemplateParameter;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;
import java.util.stream.Collectors;

public class InstanceManagerImpl implements InstanceManager {
    private final OpenShift openShift;
    private final String instanceTemplateName;
    private final boolean isMultitenant;

    public InstanceManagerImpl(OpenShift openShift, String instanceTemplateName, boolean isMultitenant) {
        this.openShift = openShift;
        this.instanceTemplateName = instanceTemplateName;
        this.isMultitenant = isMultitenant;
    }

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        if (isMultitenant) {
            Map<String, String> labelMap = new LinkedHashMap<>();
            labelMap.put("instance", instanceId.getId());
            labelMap.put("app", "enmasse");
            labelMap.put("type", "instance");
            return list(labelMap).stream().findAny();
        } else {
            return Optional.of(buildInstance(instanceId));
        }
    }

    private Instance buildInstance(InstanceId instanceId) {
        List<Route> routes = openShift.getRoutes(instanceId);
        return new Instance.Builder(instanceId)
                .messagingHost(getRouteHost(routes, "messaging"))
                .mqttHost(getRouteHost(routes, "mqtt"))
                .consoleHost(getRouteHost(routes, "console"))
                .build();
    }

    private Set<Instance> list(Map<String, String> labelMap) {
        if (isMultitenant) {
            return openShift.listNamespaces(labelMap).stream()
                    .map(namespace -> InstanceId.withIdAndNamespace(namespace.getMetadata().getLabels().get("instance"), namespace.getMetadata().getName()))
                    .map(this::buildInstance)
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private Optional<String> getRouteHost(List<Route> routes, String routeName) {
        for (Route route : routes) {
            if (route.getMetadata().getName().equals(routeName)) {
                return Optional.of(route.getSpec().getHost());
            }
        }
        return Optional.empty();
    }

    @Override
    public void create(Instance instance) {
        if (isMultitenant) {
            openShift.createNamespace(instance.id());
            openShift.addDefaultViewPolicy(instance.id());
        }

        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new ParameterValue(TemplateParameter.INSTANCE, OpenShift.sanitizeName(instance.id().getId())));
        instance.messagingHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.MESSAGING_HOSTNAME, h)));
        instance.mqttHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.MQTT_HOSTNAME, h)));
        instance.consoleHost().ifPresent(h -> parameterValues.add(new ParameterValue(TemplateParameter.CONSOLE_HOSTNAME, h)));

        KubernetesList items = openShift.processTemplate(instanceTemplateName, parameterValues.toArray(new ParameterValue[0]));

        OpenShift instanceClient = openShift.mutateClient(instance.id());
        instanceClient.create(items);
    }

    @Override
    public void delete(Instance instance) {
        if (openShift.mutateClient(instance.id()).listClusters().isEmpty()) {
            openShift.deleteNamespace(instance.id().getNamespace());
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
}
