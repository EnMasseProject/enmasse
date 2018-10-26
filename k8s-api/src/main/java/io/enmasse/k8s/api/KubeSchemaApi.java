/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class KubeSchemaApi implements SchemaApi {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(KubeSchemaApi.class);
    private final AddressSpacePlanApi addressSpacePlanApi;
    private final AddressPlanApi addressPlanApi;
    private final BrokeredInfraConfigApi brokeredInfraConfigApi;
    private final StandardInfraConfigApi standardInfraConfigApi;
    private final Clock clock;
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));
    private final boolean isOpenShift;

    private volatile List<AddressSpacePlan> currentAddressSpacePlans;
    private volatile List<AddressPlan> currentAddressPlans;
    private volatile List<StandardInfraConfig> currentStandardInfraConfigs;
    private volatile List<BrokeredInfraConfig> currentBrokeredInfraConfigs;

    public KubeSchemaApi(AddressSpacePlanApi addressSpacePlanApi, AddressPlanApi addressPlanApi, BrokeredInfraConfigApi brokeredInfraConfigApi, StandardInfraConfigApi standardInfraConfigApi, Clock clock, boolean isOpenShift) {
        this.addressSpacePlanApi = addressSpacePlanApi;
        this.addressPlanApi = addressPlanApi;
        this.brokeredInfraConfigApi = brokeredInfraConfigApi;
        this.standardInfraConfigApi = standardInfraConfigApi;
        this.clock = clock;
        this.isOpenShift = isOpenShift;
    }

    public static KubeSchemaApi create(NamespacedOpenShiftClient openShiftClient, String namespace, boolean isOpenShift) {
        AddressSpacePlanApi addressSpacePlanApi = new KubeAddressSpacePlanApi(openShiftClient, namespace, AdminCrd.addressspaceplans());

        AddressPlanApi addressPlanApi = new KubeAddressPlanApi(openShiftClient, namespace, AdminCrd.addressplans());

        BrokeredInfraConfigApi brokeredInfraConfigApi = new KubeBrokeredInfraConfigApi(openShiftClient, namespace, AdminCrd.brokeredinfraconfigs());

        StandardInfraConfigApi standardInfraConfigApi = new KubeStandardInfraConfigApi(openShiftClient, namespace, AdminCrd.standardinfraconfigs());

        Clock clock = Clock.systemUTC();

        return new KubeSchemaApi(addressSpacePlanApi, addressPlanApi, brokeredInfraConfigApi, standardInfraConfigApi, clock, isOpenShift);
    }

    private void validateAddressSpacePlan(AddressSpacePlan addressSpacePlan, List<AddressPlan> addressPlans, List<String> infraTemplateNames) {
        String definedBy = addressSpacePlan.getMetadata().getAnnotations().get(AnnotationKeys.DEFINED_BY);
        if (!infraTemplateNames.contains(definedBy)) {
            String error = "Error validating address space plan " + addressSpacePlan.getMetadata().getName() + ": missing infra config definition " + definedBy + ", found: " + infraTemplateNames;
            log.warn(error);
            throw new SchemaValidationException(error);
        }

        Set<String> addressPlanNames = addressPlans.stream().map(p -> p.getMetadata().getName()).collect(Collectors.toSet());
        if (!addressPlanNames.containsAll(addressSpacePlan.getAddressPlans())) {
            Set<String> missing = new HashSet<>(addressSpacePlan.getAddressPlans());
            missing.removeAll(addressPlanNames);
            String error = "Error validating address space plan " + addressSpacePlan.getMetadata().getName() + ": missing " + missing;
            log.warn(error);
            throw new SchemaValidationException(error);
        }
    }

    private void validateAddressPlan(AddressPlan addressPlan) {
        Set<String> allowedResources = new HashSet<>(Arrays.asList("broker", "router"));
        Set<String> resourcesUsed = addressPlan.getRequiredResources().stream().map(ResourceRequest::getName).collect(Collectors.toSet());

        if (!allowedResources.containsAll(resourcesUsed)) {
            Set<String> missing = new HashSet<>(resourcesUsed);
            missing.removeAll(allowedResources);
            String error = "Error validating address plan " + addressPlan.getMetadata().getName() + ": missing resources " + missing;
            log.warn(error);
            throw new SchemaValidationException(error);
        }
    }

    private EndpointSpec createEndpointSpec(String name, String service, String port, ExposeSpec.TlsTermination tlsTermination) {
        if (isOpenShift) {
            return new EndpointSpec.Builder()
                    .setName(name)
                    .setService(service)
                    .setExposeSpec(new ExposeSpec.Builder()
                            .setType(ExposeSpec.ExposeType.route)
                            .setRouteTlsTermination(tlsTermination)
                            .setRouteServicePort(port)
                            .build())
                    .build();
        } else {
            return new EndpointSpec.Builder()
                    .setName(name)
                    .setService(service)
                    .setExposeSpec(new ExposeSpec.Builder()
                            .setType(ExposeSpec.ExposeType.loadbalancer)
                            .setLoadBalancerPorts(Collections.singletonList(port))
                            .build())
                    .build();
        }
    }

    private AddressSpaceType createStandardType(List<AddressSpacePlan> addressSpacePlans, List<AddressPlan> addressPlans, List<InfraConfig> standardInfraConfigs) {
        AddressSpaceType.Builder builder = new AddressSpaceType.Builder();
        builder.setName("standard");
        builder.setDescription("A standard address space consists of an AMQP router network in combination with " +
                "attachable 'storage units'. The implementation of a storage unit is hidden from the client " +
                        "and the routers with a well defined API.");

        builder.setAvailableEndpoints(Arrays.asList(
                createEndpointSpec("messaging", "messaging", "amqps", ExposeSpec.TlsTermination.passthrough),
                createEndpointSpec("amqp-wss", "messaging", "https", ExposeSpec.TlsTermination.reencrypt),
                createEndpointSpec("mqtt", "mqtt", "secure-mqtt", ExposeSpec.TlsTermination.passthrough),
                createEndpointSpec("console", "console", "https", ExposeSpec.TlsTermination.reencrypt)));

        List<AddressSpacePlan> filteredAddressSpaceplans = addressSpacePlans.stream()
                .filter(plan -> "standard".equals(plan.getAddressSpaceType()))
                .collect(Collectors.toList());
        builder.setAddressSpacePlans(filteredAddressSpaceplans);

        List<AddressPlan> filteredAddressPlans = addressPlans.stream()
                .filter(plan -> filteredAddressSpaceplans.stream()
                        .filter(aPlan -> aPlan.getAddressPlans().contains(plan.getMetadata().getName()))
                        .count() > 0)
                .collect(Collectors.toList());


        builder.setInfraConfigs(standardInfraConfigs);
        builder.setInfraConfigDeserializer(json -> mapper.readValue(json, StandardInfraConfig.class));

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
                                "in which case message ordering is no longer guaranteed.",
                        filteredAddressPlans),
                createAddressType(
                        "topic",
                        "A topic address for store-and-forward publish-subscribe messaging. Each message published " +
                                "to a topic address is forwarded to all subscribes on that address.",
                        filteredAddressPlans),
                createAddressType(
                        "subscription",
                        "A subscription on a topic",
                        filteredAddressPlans)));

        return builder.build();
    }

    private AddressSpaceType createBrokeredType(List<AddressSpacePlan> addressSpacePlans, List<AddressPlan> addressPlans, List<InfraConfig> brokeredInfraConfigs) {
        AddressSpaceType.Builder builder = new AddressSpaceType.Builder();
        builder.setName("brokered");
        builder.setDescription("A brokered address space consists of a broker combined with a console for managing addresses.");

        builder.setAvailableEndpoints(Arrays.asList(
                createEndpointSpec("messaging", "messaging", "amqps", ExposeSpec.TlsTermination.passthrough),
                createEndpointSpec("console", "console", "https", ExposeSpec.TlsTermination.reencrypt)));

        List<AddressSpacePlan> filteredAddressSpaceplans = addressSpacePlans.stream()
                .filter(plan -> "brokered".equals(plan.getAddressSpaceType()))
                .collect(Collectors.toList());
        builder.setAddressSpacePlans(filteredAddressSpaceplans);

        List<AddressPlan> filteredAddressPlans = addressPlans.stream()
                .filter(plan -> filteredAddressSpaceplans.stream()
                        .filter(aPlan -> aPlan.getAddressPlans().contains(plan.getMetadata().getName()))
                        .count() > 0)
                .collect(Collectors.toList());

        builder.setInfraConfigs(brokeredInfraConfigs);
        builder.setInfraConfigDeserializer(json -> mapper.readValue(json, BrokeredInfraConfig.class));

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
        List<Watch> watches = new ArrayList<>();
        watches.add(addressSpacePlanApi.watchAddressSpacePlans(items -> {
            currentAddressSpacePlans = items;
            updateSchema(watcher);
        }, resyncInterval));

        watches.add(addressPlanApi.watchAddressPlans(items -> {
            currentAddressPlans = items;
            updateSchema(watcher);
        }, resyncInterval));

        watches.add(brokeredInfraConfigApi.watchBrokeredInfraConfigs(items -> {
            currentBrokeredInfraConfigs = items;
            updateSchema(watcher);
        }, resyncInterval));

        watches.add(standardInfraConfigApi.watchStandardInfraConfigs(items -> {
            currentStandardInfraConfigs = items;
            updateSchema(watcher);
        }, resyncInterval));


        return () -> {
            Exception e = null;
            for (Watch watch : watches) {
                try {
                    watch.close();
                } catch (Exception ex) {
                    e = ex;
                }
            }
            if (e != null) {
                throw e;
            }
        };
    }

    private synchronized void updateSchema(Watcher<Schema> watcher) throws Exception {
        Schema schema = assembleSchema(currentAddressSpacePlans, currentAddressPlans, currentStandardInfraConfigs, currentBrokeredInfraConfigs);
        if (schema != null) {
            watcher.onUpdate(Collections.singletonList(schema));
        }
    }

    Schema assembleSchema(List<AddressSpacePlan> addressSpacePlans, List<AddressPlan> addressPlans, List<StandardInfraConfig> standardInfraConfigs, List<BrokeredInfraConfig> brokeredInfraConfigs) {
        if (addressSpacePlans == null || addressPlans == null || brokeredInfraConfigs == null || standardInfraConfigs == null) {
            return null;
        }

        for (AddressSpacePlan addressSpacePlan : addressSpacePlans) {
            if (addressSpacePlan.getAddressSpaceType().equals("brokered")) {
                validateAddressSpacePlan(addressSpacePlan, addressPlans, brokeredInfraConfigs.stream().map(t -> t.getMetadata().getName()).collect(Collectors.toList()));
            } else {
                validateAddressSpacePlan(addressSpacePlan, addressPlans, standardInfraConfigs.stream().map(t -> t.getMetadata().getName()).collect(Collectors.toList()));
            }
        }

        for (AddressPlan addressPlan : addressPlans) {
            validateAddressPlan(addressPlan);
        }

        List<AddressSpaceType> types = new ArrayList<>();
        types.add(createBrokeredType(addressSpacePlans, addressPlans, new ArrayList<>(brokeredInfraConfigs)));
        types.add(createStandardType(addressSpacePlans, addressPlans, new ArrayList<>(standardInfraConfigs)));
        return new Schema.Builder()
                .setAddressSpaceTypes(types)
                .setCreationTimestamp(formatter.format(clock.instant()))
                .build();
    }
}
