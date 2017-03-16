package enmasse.address.controller.admin;

import enmasse.address.controller.generator.TemplateParameter;
import enmasse.address.controller.model.Instance;
import enmasse.address.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;
import java.util.stream.Collectors;

public class InstanceManagerImpl implements InstanceManager {
    private final OpenShift openShift;
    private final String instanceTemplateName;

    public InstanceManagerImpl(OpenShift openShift, String instanceTemplateName) {
        this.openShift = openShift;
        this.instanceTemplateName = instanceTemplateName;
    }

    @Override
    public Optional<Instance> get(String instanceId) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("instance", instanceId);
        labelMap.put("app", "enmasse");
        labelMap.put("type", "instance");
        return list(labelMap).stream().findAny();
    }

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        return get(instanceId.getId());
    }

    private Set<Instance> list(Map<String, String> labelMap) {
        return openShift.listNamespaces(labelMap).stream()
                .map(namespace -> InstanceId.withIdAndNamespace(namespace.getMetadata().getLabels().get("instance"), namespace.getMetadata().getName()))
                .map(id -> {
                    List<Route> routes = openShift.getRoutes(id);
                    Instance.Builder builder = new Instance.Builder(id);
                    builder.messagingHost(getRouteHost(routes, "messaging"));
                    builder.mqttHost(getRouteHost(routes, "mqtt"));
                    builder.consoleHost(getRouteHost(routes, "console"));
                    return builder.build();
                }).collect(Collectors.toSet());
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
        create(instance, true);
    }

    public void create(Instance instance, boolean createNamespace) {
        if (createNamespace) {
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
        openShift.deleteNamespace(instance.id().getNamespace());
    }

    @Override
    public Set<Instance> list() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("app", "enmasse");
        labelMap.put("type", "instance");
        return list(labelMap);
    }
}
