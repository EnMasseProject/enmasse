package enmasse.controller.instance;

import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.common.TemplateParameter;
import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InstanceFactoryImpl implements InstanceFactory {
    private static final Logger log = LoggerFactory.getLogger(InstanceFactoryImpl.class.getName());
    private final Kubernetes kubernetes;
    private final String instanceTemplateName;
    private final boolean isMultitenant;

    public InstanceFactoryImpl(Kubernetes kubernetes, String instanceTemplateName, boolean isMultitenant) {
        this.kubernetes = kubernetes;
        this.instanceTemplateName = instanceTemplateName;
        this.isMultitenant = isMultitenant;
    }

    @Override
    public void create(Instance instance) {
        Kubernetes instanceClient = kubernetes.withInstance(instance.id());
        if (!instanceClient.getRoutes(instance.id()).isEmpty()) {
            log.info("Instance " + instance.id() + " already created, ignoring");
        }
        if (isMultitenant) {
            kubernetes.createNamespace(instance.id());
            kubernetes.addDefaultViewPolicy(instance.id());
        }

        String secretName = instance.certSecret().orElse(kubernetes.createInstanceSecret(instance.id()));
        Instance.Builder builder = new Instance.Builder(instance);
        builder.certSecret(Optional.ofNullable(secretName));
        instance = builder.build();

        instanceClient.create(createResourceList(instance, secretName));
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

    private Kubernetes getClient(Instance instance) {
        return kubernetes.withInstance(instance.id());
    }

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

    @Override
    public void delete(Instance instance) {
        if (kubernetes.withInstance(instance.id()).listClusters().isEmpty()) {
            kubernetes.deleteNamespace(instance.id().getNamespace());
        } else {
            throw new IllegalArgumentException("Instance " + instance.id() + " still has active destinations");
        }
    }
}
