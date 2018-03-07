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
import io.enmasse.k8s.api.cache.*;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.RequestConfigBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConfigMapSchemaApi implements SchemaApi, ListerWatcher<ConfigMap, ConfigMapList> {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapSchemaApi.class);
    private final NamespacedOpenShiftClient client;
    private final String namespace;

    private static final ObjectMapper mapper = CodecV1.getMapper();

    public ConfigMapSchemaApi(NamespacedOpenShiftClient client, String namespace) {
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

    private <T> List<T> getResources(Class<T> type, String labelType, List<ConfigMap> maps) {
        List<T> items = new ArrayList<>();

        for (ConfigMap config : maps) {
            if (config.getMetadata().getLabels().get(LabelKeys.TYPE).equals(labelType)) {
                items.add(getResourceFromConfig(type, config));
            }
        }
        return items;
    }

    private List<AddressPlan> getAddressPlans(List<ConfigMap> maps) {
        return getResources(AddressPlan.class, "address-plan", maps);
    }

    private List<AddressSpacePlan> getAddressSpacePlans(List<ConfigMap> maps) {
        return getResources(AddressSpacePlan.class, "address-space-plan", maps);
    }

    private List<ResourceDefinition> getResourceDefinitions(List<ConfigMap> maps) {
        return getResources(ResourceDefinition.class, "resource-definition", maps);
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

    private void validateAddressSpacePlan(AddressSpacePlan addressSpacePlan, List<AddressPlan> addressPlans, List<ResourceDefinition> resourceDefinitions) {
        Set<String> resourceDefinitionNames = resourceDefinitions.stream().map(ResourceDefinition::getName).collect(Collectors.toSet());
        String definedBy = addressSpacePlan.getAnnotations().get(AnnotationKeys.DEFINED_BY);
        if (!resourceDefinitionNames.contains(definedBy)) {
            String error = "Error validating address space plan " + addressSpacePlan.getName() + ": missing resource definition " + definedBy + ", found: " + resourceDefinitionNames;
            log.warn(error);
            throw new SchemaValidationException(error);
        }

        Set<String> addressPlanNames = addressPlans.stream().map(AddressPlan::getName).collect(Collectors.toSet());
        if (!addressPlanNames.containsAll(addressSpacePlan.getAddressPlans())) {
            Set<String> missing = new HashSet<>(addressSpacePlan.getAddressPlans());
            missing.removeAll(addressPlanNames);
            String error = "Error validating address space plan " + addressSpacePlan.getName() + ": missing " + missing;
            log.warn(error);
            throw new SchemaValidationException(error);
        }
    }

    private void validateAddressPlan(AddressPlan addressPlan, List<ResourceDefinition> resourceDefinitions) {
        Set<String> resourceDefinitionNames = resourceDefinitions.stream().map(ResourceDefinition::getName).collect(Collectors.toSet());
        Set<String> resourcesUsed = addressPlan.getRequiredResources().stream().map(ResourceRequest::getResourceName).collect(Collectors.toSet());

        if (!resourceDefinitionNames.containsAll(resourcesUsed)) {
            Set<String> missing = new HashSet<>(resourcesUsed);
            missing.removeAll(resourceDefinitionNames);
            String error = "Error validating address plan " + addressPlan.getName() + ": missing resources " + missing;
            log.warn(error);
            throw new SchemaValidationException(error);
        }
    }

    @Override
    public Schema getSchema() {
        List<ConfigMap> maps = list(new ListOptions()).getItems();
        return assembleSchema(maps);
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

    @Override
    public Watch watchSchema(Watcher<Schema> watcher, Duration resyncInterval) {
        WorkQueue<ConfigMap> queue = new FifoQueue<>(config -> config.getMetadata().getName());
        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        config.setClock(Clock.systemUTC());
        config.setExpectedType(ConfigMap.class);
        config.setListerWatcher(this);
        config.setResyncInterval(resyncInterval);
        config.setWorkQueue(queue);
        config.setProcessor(map -> {
            if (queue.hasSynced()) {
                watcher.onUpdate(
                        Collections.singleton(assembleSchema(queue.list())));
            }
        });

        Reflector<ConfigMap, ConfigMapList> reflector = new Reflector<>(config);
        Controller controller = new Controller(reflector);
        controller.start();
        return controller;
    }

    @Override
    public io.fabric8.kubernetes.client.Watch watch(io.fabric8.kubernetes.client.Watcher<ConfigMap> watcher, ListOptions listOptions) {
        RequestConfig requestConfig = new RequestConfigBuilder()
                .withRequestTimeout(listOptions.getTimeoutSeconds())
                .build();
        return client.withRequestConfig(requestConfig).call(c ->
                c.configMaps()
                        .inNamespace(namespace)
                        .withLabelIn(LabelKeys.TYPE,"address-space-plan", "address-plan", "resource-definition")
                        .withResourceVersion(listOptions.getResourceVersion())
                        .watch(watcher));
    }

    @Override
    public ConfigMapList list(ListOptions listOptions) {
        return client.configMaps()
                .inNamespace(namespace)
                .withLabelIn(LabelKeys.TYPE, "address-space-plan", "address-plan", "resource-definition")
                .list();
    }

    private Schema assembleSchema(List<ConfigMap> maps) {
        List<AddressSpacePlan> addressSpacePlans = getAddressSpacePlans(maps);
        List<AddressPlan> addressPlans = getAddressPlans(maps);
        List<ResourceDefinition> resourceDefinitions = getResourceDefinitions(maps);

        for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
            validateAddressSpacePlan(addressSpacePlan, addressPlans, resourceDefinitions);
        }

        for (AddressPlan addressPlan : addressPlans) {
            validateAddressPlan(addressPlan, resourceDefinitions);
        }

        List<AddressSpaceType> types = new ArrayList<>();
        types.add(createBrokeredType(addressSpacePlans, addressPlans));
        types.add(createStandardType(addressSpacePlans, addressPlans));
        return new Schema.Builder()
                .setAddressSpaceTypes(types)
                .setResourceDefinitions(resourceDefinitions)
                .build();
    }

}
