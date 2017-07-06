package enmasse.controller.standard;

import enmasse.config.AnnotationKeys;
import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.common.TemplateParameter;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CertProvider;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.SecretCertProvider;
import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.types.TemplateConfig;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for managing a standard address space.
 */
public class StandardHelper {
    private static final Logger log = LoggerFactory.getLogger(StandardHelper.class.getName());
    private final Kubernetes kubernetes;
    private final boolean isMultitenant;
    private final String namespace;

    public StandardHelper(Kubernetes kubernetes, boolean isMultitenant, String namespace) {
        this.kubernetes = kubernetes;
        this.isMultitenant = isMultitenant;
        this.namespace = namespace;
    }

    public void create(AddressSpace addressSpace) {
        Kubernetes instanceClient = kubernetes.withNamespace(addressSpace.getNamespace());
        if (instanceClient.hasService("messaging")) {
            return;
        }
        log.info("Creating address space {}", addressSpace);
        if (isMultitenant) {
            kubernetes.createNamespace(addressSpace.getName(), addressSpace.getNamespace());
            kubernetes.addDefaultViewPolicy(addressSpace.getNamespace());
        }


        StandardResources resourceList = createResourceList(addressSpace);

        // Step 5: Create routes
        for (Endpoint endpoint : resourceList.routeEndpoints) {
            kubernetes.createRoute(endpoint.getName(), endpoint.getService(), endpoint.getHost().orElse(null), addressSpace.getNamespace());
        }

        // Step 4: Create secrets for endpoints if they don't already exist
        for (CertProvider certProvider : resourceList.serviceCerts.values()) {
            kubernetes.createSecretWithDefaultPermissions(certProvider.getSecretName(), addressSpace.getNamespace());
        }

        kubernetes.create(resourceList.resourceList);
    }

    private static class StandardResources {
        public AddressSpace addressSpace;
        public KubernetesList resourceList;
        public List<Endpoint> routeEndpoints;
        public Map<String, CertProvider> serviceCerts;
    }

    private StandardResources createResourceList(AddressSpace addressSpace) {
        Plan plan = addressSpace.getPlan();
        StandardResources returnVal = new StandardResources();
        returnVal.addressSpace = addressSpace;
        returnVal.resourceList = new KubernetesList();
        returnVal.serviceCerts = new HashMap<>();
        returnVal.routeEndpoints = new ArrayList<>();


        if (plan.getTemplateConfig().isPresent()) {
            List<ParameterValue> parameterValues = new ArrayList<>();

            parameterValues.add(new ParameterValue(TemplateParameter.ADDRESS_SPACE, addressSpace.getName()));
            parameterValues.add(new ParameterValue(TemplateParameter.ADDRESS_SPACE_SERVICE_HOST, getApiServer()));

            // Step 1: Validate endpoints and remove unknown
            Set<String> requiredServices = new HashSet<>(Arrays.asList("messaging", "mqtt", "console"));
            Set<String> discoveredServices = new HashSet<>();
            List<Endpoint> endpoints = new ArrayList<>(addressSpace.getEndpoints());
            Iterator<Endpoint> it = endpoints.iterator();
            Map<String, CertProvider> serviceCertProviders = new HashMap<>();

            while (it.hasNext()) {
                Endpoint endpoint = it.next();
                if (!requiredServices.contains(endpoint.getService())) {
                    log.info("Unknown service {} for endpoint {}, removing", endpoint.getService(), endpoint.getName());
                    it.remove();
                } else {
                    discoveredServices.add(endpoint.getService());
                    endpoint.getCertProvider().ifPresent(certProvider -> serviceCertProviders.put(endpoint.getService(), certProvider));
                }
            }
            returnVal.routeEndpoints.addAll(endpoints);

            // Step 2: Create endpoint objects for those not found
            Set<String> secretsToGenerate = new HashSet<>();
            Set<String> missingServices = new HashSet<>(requiredServices);
            missingServices.removeAll(discoveredServices);

            for (String service : missingServices) {
                String secretName = "certs-" + service;
                if (!serviceCertProviders.containsKey(service)) {
                    serviceCertProviders.put(service, new SecretCertProvider(secretName));
                    secretsToGenerate.add(secretName);
                }
                endpoints.add(new Endpoint.Builder()
                        .setCertProvider(serviceCertProviders.get(secretName))
                        .setName(service)
                        .setService(service)
                        .build());
            }

            // Step 3: Ensure all endpoints have their certProviders set
            List<Endpoint> allEndpoints = endpoints.stream()
                    .map(endpoint -> {
                        if (!endpoint.getCertProvider().isPresent()) {
                            Endpoint newEndpoint = new Endpoint.Builder(endpoint)
                                    .setCertProvider(serviceCertProviders.get(endpoint.getService()))
                                    .build();
                            return newEndpoint;
                        } else {
                            return endpoint;
                        }
                    }).collect(Collectors.toList());

            returnVal.addressSpace = new AddressSpace.Builder(addressSpace)
                    .setEndpointList(allEndpoints)
                    .build();

            returnVal.serviceCerts = serviceCertProviders;

            parameterValues.add(new ParameterValue(TemplateParameter.ROUTER_SECRET, serviceCertProviders.get("messaging").getSecretName()));
            parameterValues.add(new ParameterValue(TemplateParameter.MQTT_SECRET, serviceCertProviders.get("mqtt").getSecretName()));

            // Step 5: Create infrastructure
            TemplateConfig templateConfig = plan.getTemplateConfig().get();
            returnVal.resourceList = kubernetes.processTemplate(templateConfig.getName(), parameterValues.toArray(new ParameterValue[0]));
        }
        return returnVal;
    }

    private String getApiServer() {
        return "address-controller." + namespace + ".svc";
    }

    public boolean isReady(AddressSpace addressSpace) {
        Set<String> readyDeployments = kubernetes.withNamespace(addressSpace.getNamespace()).getReadyDeployments().stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());

        Set<String> requiredDeployments = createResourceList(addressSpace).resourceList.getItems().stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        return readyDeployments.containsAll(requiredDeployments);
    }

    public void retainAddressSpaces(Set<String> desiredAddressSpaces) {
        if (isMultitenant) {
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put(LabelKeys.APP, "enmasse");
            labels.put(LabelKeys.TYPE, "address-space");
            for (Namespace namespace : kubernetes.listNamespaces(labels)) {
                String id = namespace.getMetadata().getAnnotations().get(AnnotationKeys.ADDRESS_SPACE);
                if (!desiredAddressSpaces.contains(id)) {
                    try {
                        kubernetes.deleteNamespace(namespace.getMetadata().getName());
                    } catch(KubernetesClientException e){
                        log.info("Exception when deleting namespace (may already be in progress): " + e.getMessage());
                    }
                }
            }
        }
    }
}
