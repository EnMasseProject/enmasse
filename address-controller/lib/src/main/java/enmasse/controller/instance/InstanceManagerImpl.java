package enmasse.controller.instance;

import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.Route;
import enmasse.controller.common.TemplateParameter;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.ParameterValue;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InstanceManagerImpl implements InstanceManager {
    private final Kubernetes kubernetes;
    private final String instanceTemplateName;
    private final boolean isMultitenant;
    // TODO: Make truly random
    private final SecureRandom sr = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());

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

        String secretName = instance.certSecret().orElse(generateCertificates(instance.id()));

        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new ParameterValue(TemplateParameter.INSTANCE, Kubernetes.sanitizeName(instance.id().getId())));
        parameterValues.add(new ParameterValue(TemplateParameter.MESSAGING_HOSTNAME, instance.messagingHost().orElse("")));
        parameterValues.add(new ParameterValue(TemplateParameter.MQTT_HOSTNAME, instance.mqttHost().orElse("")));
        parameterValues.add(new ParameterValue(TemplateParameter.CONSOLE_HOSTNAME, instance.consoleHost().orElse("")));
        parameterValues.add(new ParameterValue(TemplateParameter.ROUTER_SECRET, secretName));
        parameterValues.add(new ParameterValue(TemplateParameter.MQTT_SECRET, secretName));
        parameterValues.add(new ParameterValue(TemplateParameter.KAFKA_BOOTSTRAP_SERVERS, ""));

        KubernetesList items = kubernetes.processTemplate(instanceTemplateName, parameterValues.toArray(new ParameterValue[0]));
        instance.uuid().ifPresent(uuid -> Kubernetes.addObjectLabel(items, LabelKeys.UUID, uuid));

        Kubernetes instanceClient = kubernetes.mutateClient(instance.id());
        instanceClient.create(items);
    }

    // TODO: Have this generate certificates from OpenShift CA
    private String generateCertificates(InstanceId instanceId) throws IOException, InterruptedException {
        File keyFile = new File("/tmp/server-key.pem");
        File certFile = new File("/tmp/server-cert.pem");
        Process keyGen = new ProcessBuilder("openssl", "req", "-new", "-x509", "-batch", "-nodes",
                "-out", certFile.getAbsolutePath(), "-keyout", keyFile.getAbsolutePath()).start();
        if (!keyGen.waitFor(1, TimeUnit.MINUTES)) {
            throw new RuntimeException("Key generation timed out");
        }
        return kubernetes.createCertSecret(instanceId, keyFile, certFile);
    }

    @Override
    public void delete(Instance instance) {
        if (kubernetes.mutateClient(instance.id()).listClusters().isEmpty()) {
            kubernetes.deleteNamespace(instance.id().getNamespace());
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
