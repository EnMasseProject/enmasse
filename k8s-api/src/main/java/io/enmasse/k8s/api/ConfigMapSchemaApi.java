/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.*;
import io.enmasse.address.model.v1.CodecV1;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConfigMapSchemaApi implements SchemaApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapSchemaApi.class);
    private final KubernetesClient client;
    private final String namespace;

    private static final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapSchemaApi(KubernetesClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    private <T> T getResourceFromConfig(Class<T> type, ConfigMap configMap) {
        Map<String, String> data = configMap.getData();

        try {
            return mapper.readValue(data.get("definition"), type);
        } catch (Exception e) {
            log.warn("Unable to decode {}", type, e);
            throw new RuntimeException(e);
        }
    }

    private ConfigMapList listConfigMaps(String type) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, type);

        return client.configMaps().inNamespace(namespace).withLabels(labels).list();
    }

    private <T> List<T> listResources(Class<T> type, String labelType) {
        List<T> items = new ArrayList<>();

        for (ConfigMap config : listConfigMaps(labelType).getItems()) {
            items.add(getResourceFromConfig(type, config));
        }
        return items;
    }

    private List<AddressPlan> listAddressPlans() {
        return listResources(AddressPlan.class, "address-plan");
    }

    private List<AddressSpacePlan> listAddressSpacePlans() {
        return listResources(AddressSpacePlan.class, "address-space-plan");
    }

    private List<ResourceDefinition> listResourceDefinitions() {
        return listResources(ResourceDefinition.class, "resource-definition");
    }

    @Override
    public void copyIntoNamespace(AddressSpacePlan plan, String otherNamespace) {
        KubernetesListBuilder listBuilder = new KubernetesListBuilder();
        listBuilder.addAllToConfigMapItems(copyMaps(listConfigMaps("resource-definition").getItems(), m -> true, otherNamespace));
        listBuilder.addAllToConfigMapItems(copyMaps(
                listConfigMaps("address-space-plan").getItems(),
                m -> getResourceFromConfig(AddressSpacePlan.class, m).equals(plan),
                otherNamespace));
        listBuilder.addAllToConfigMapItems(copyMaps(
                listConfigMaps("address-plan").getItems(),
                m -> plan.getAddressPlans().contains(getResourceFromConfig(AddressPlan.class, m).getName()),
                otherNamespace));
        client.lists().inNamespace(otherNamespace).create(listBuilder.build());
    }

    private Collection<ConfigMap> copyMaps(List<ConfigMap> items, Predicate<ConfigMap> filter, String otherNamespace) {
        List<ConfigMap> list = new ArrayList<>();
        for (ConfigMap map : items) {
            if (filter.test(map)) {
                list.add(new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withName(map.getMetadata().getName())
                        .withNamespace(otherNamespace)
                        .addToLabels(map.getMetadata().getLabels())
                        .addToAnnotations(map.getMetadata().getAnnotations())
                        .endMetadata()
                        .addToData(map.getData())
                        .build());
            }
        }
        return list;
    }

    private void validateAddressSpacePlan(AddressSpacePlan addressSpacePlan) {
        Set<String> resourceDefinitions = listResourceDefinitions().stream().map(ResourceDefinition::getName).collect(Collectors.toSet());
        String definedBy = addressSpacePlan.getAnnotations().get(AnnotationKeys.DEFINED_BY);
        if (!resourceDefinitions.contains(definedBy)) {
            String error = "Error validating address space plan " + addressSpacePlan.getName() + ": missing resource definition " + definedBy + ", found: " + resourceDefinitions;
            log.warn(error);
            throw new SchemaValidationException(error);
        }

        Set<String> addressPlans = listAddressPlans().stream().map(AddressPlan::getName).collect(Collectors.toSet());
        if (!addressPlans.containsAll(addressSpacePlan.getAddressPlans())) {
            Set<String> missing = new HashSet<>(addressSpacePlan.getAddressPlans());
            missing.removeAll(addressPlans);
            String error = "Error validating address space plan " + addressSpacePlan.getName() + ": missing " + missing;
            log.warn(error);
            throw new SchemaValidationException(error);
        }
    }

    private void validateAddressPlan(AddressPlan addressPlan) {
        Set<String> resourceDefinitions = listResourceDefinitions().stream().map(ResourceDefinition::getName).collect(Collectors.toSet());
        Set<String> resourcesUsed = addressPlan.getRequiredResources().stream().map(ResourceRequest::getResourceName).collect(Collectors.toSet());

        if (!resourceDefinitions.containsAll(resourcesUsed)) {
            Set<String> missing = new HashSet<>(resourcesUsed);
            missing.removeAll(resourceDefinitions);
            String error = "Error validating address plan " + addressPlan.getName() + ": missing resources " + missing;
            log.warn(error);
            throw new SchemaValidationException(error);
        }
    }

    @Override
    public Schema getSchema() {
        List<AddressSpacePlan> addressSpacePlans = listAddressSpacePlans();
        List<AddressPlan> addressPlans = listAddressPlans();

        for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
            validateAddressSpacePlan(addressSpacePlan);
        }

        for (AddressPlan addressPlan : addressPlans) {
            validateAddressPlan(addressPlan);
        }

        List<AddressSpaceType> types = new ArrayList<>();
        types.add(createBrokeredType(addressSpacePlans, addressPlans));
        types.add(createStandardType(addressSpacePlans, addressPlans));
        return new Schema.Builder()
                .setAddressSpaceTypes(types)
                .setResourceDefinitions(listResourceDefinitions())
                .build();
    }

    private AddressSpaceType createStandardType(List<AddressSpacePlan> addressSpacePlans, List<AddressPlan> addressPlans) {
        AddressSpaceType.Builder builder = new AddressSpaceType.Builder();
        builder.setName("standard");
        builder.setDescription("A standard address space consists of an AMQP router network in combination with " +
                "attachable 'storage units'. The implementation of a storage unit is hidden from the client " +
                        "and the routers with a well defined API.");
        builder.setServiceNames(Arrays.asList("messaging", "mqtt", "console"));

        List<AddressSpacePlan> filteredAddressSpaceplans = addressSpacePlans.stream()
                .filter(plan -> "standard".equals(plan.getAddressSpaceType()))
                .collect(Collectors.toList());
        builder.setAddressSpacePlans(filteredAddressSpaceplans);

        List<AddressPlan> filteredAddressPlans = addressPlans.stream()
                .filter(plan -> filteredAddressSpaceplans.stream()
                        .filter(aPlan -> aPlan.getAddressPlans().contains(plan.getName()))
                        .count() > 0)
                .collect(Collectors.toList());

        builder.setAddressTypes(Arrays.asList(
                createAddressType(
                        "anycast",
                        "A direct messaging address type. Messages sent to an anycast address are not " +
                                "stored but forwarded directly to a consumer.",
                        filteredAddressPlans),
                createAddressType(
                        "multicast",
                        "A direct messaging address type. Messages sent to a multicast address are not " +
                                "stored but forwarded directly to multiple consumers.",
                        filteredAddressPlans),
                createAddressType(
                        "queue",
                        "A store-and-forward queue. A queue may be sharded across multiple storage units, " +
                                "in which case message ordering is no longer guaraneteed.",
                        filteredAddressPlans),
                createAddressType(
                        "topic",
                        "A topic address for store-and-forward publish-subscribe messaging. Each message published " +
                                "to a topic address is forwarded to all subscribes on that address.",
                        filteredAddressPlans)));

        return builder.build();
    }

    private AddressSpaceType createBrokeredType(List<AddressSpacePlan> addressSpacePlans, List<AddressPlan> addressPlans) {
        AddressSpaceType.Builder builder = new AddressSpaceType.Builder();
        builder.setName("brokered");
        builder.setDescription("A brokered address space consists of a broker combined with a console for managing addresses.");
        builder.setServiceNames(Arrays.asList("messaging", "console", "brokerconsole"));

        List<AddressSpacePlan> filteredAddressSpaceplans = addressSpacePlans.stream()
                .filter(plan -> "brokered".equals(plan.getAddressSpaceType()))
                .collect(Collectors.toList());
        builder.setAddressSpacePlans(filteredAddressSpaceplans);

        List<AddressPlan> filteredAddressPlans = addressPlans.stream()
                .filter(plan -> filteredAddressSpaceplans.stream()
                        .filter(aPlan -> aPlan.getAddressPlans().contains(plan.getName()))
                        .count() > 0)
                .collect(Collectors.toList());

        builder.setAddressTypes(Arrays.asList(
                createAddressType(
                        "queue",
                        "A queue that supports selectors, message grouping and transactions",
                        filteredAddressPlans),
                createAddressType(
                    "topic",
                    "A topic supports pub-sub semantics. Messages sent to a topic address is forwarded to all subscribes on that address.",
                    filteredAddressPlans)));

        return builder.build();
    }

    private AddressType createAddressType(String name, String description, List<AddressPlan> addressPlans) {
        AddressType.Builder builder = new AddressType.Builder();
        builder.setAddressPlans(addressPlans.stream()
                .filter(plan -> plan.getAddressType().equals(name))
                .collect(Collectors.toList()));
        builder.setName(name);
        builder.setDescription(description);
        return builder.build();
    }
}
