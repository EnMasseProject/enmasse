package enmasse.address.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import enmasse.address.controller.model.*;
import enmasse.address.controller.openshift.DestinationCluster;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Wraps the OpenShift client and adds some helper methods.
 */
public class OpenShiftHelper {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(OpenShiftHelper.class.getName());
    private final OpenShiftClient client;

    public OpenShiftHelper(OpenShiftClient client) {
        this.client = client;
    }

    public OpenShiftClient getClient() {
        return client;
    }

    public List<DestinationCluster> listClusters(FlavorRepository flavorRepository) {
        Map<Destination, List<HasMetadata>> resourceMap = new HashMap<>();

        // Add other resources part of a destination cluster
        List<HasMetadata> objects = new ArrayList<>();
        objects.addAll(client.deploymentConfigs().list().getItems());
        objects.addAll(client.persistentVolumeClaims().list().getItems());
        objects.addAll(client.configMaps().list().getItems());
        objects.addAll(client.replicationControllers().list().getItems());

        for (HasMetadata config : objects) {
            Map<String, String> labels = config.getMetadata().getLabels();
            Map<String, String> annotations = config.getMetadata().getAnnotations();

            if (labels != null && labels.containsKey(LabelKeys.STORE_AND_FORWARD) &&  labels.containsKey(LabelKeys.MULTICAST) &&
                    annotations != null && annotations.containsKey(LabelKeys.ADDRESS_LIST)) {
                log.info("Parsing resource " + config);

                Optional<String> flavorName = Optional.ofNullable(labels.get(LabelKeys.FLAVOR));
                Set<String> addresses = parseAddressAnnotation(annotations.get(LabelKeys.ADDRESS_LIST));
                boolean storeAndForward = Boolean.parseBoolean(labels.get(LabelKeys.STORE_AND_FORWARD));
                boolean multicast = Boolean.parseBoolean(labels.get(LabelKeys.MULTICAST));

                Destination destination = new Destination(addresses, storeAndForward, multicast, flavorName);

                if (!resourceMap.containsKey(destination)) {
                    resourceMap.put(destination, new ArrayList<>());
                }
                resourceMap.get(destination).add(config);
            }
        }

        return resourceMap.entrySet().stream()
                .map(entry -> {
                    KubernetesList list = new KubernetesList();
                    list.setItems(entry.getValue());
                    Flavor flavor = getFlavor(flavorRepository, entry.getKey());
                    return new DestinationCluster(client, entry.getKey(), list, flavor.isShared());
                }).collect(Collectors.toList());
    }

    private Flavor getFlavor(FlavorRepository flavorRepository, Destination dest) {
        return dest.flavor()
                .map(f -> flavorRepository.getFlavor(f, TimeUnit.SECONDS.toMillis(60)))
                .orElse(new Flavor.Builder("direct", "direct").build());
    }

    public static Set<String> parseAddressAnnotation(String jsonAddresses) {
        try {
            ArrayNode array = (ArrayNode) mapper.readTree(jsonAddresses);
            Set<String> lst = new HashSet<>();
            for (int i = 0; i < array.size(); i++) {
                lst.add(array.get(i).asText());
            }
            return lst;
        } catch (Exception e) {
            log.error("Unable to parse address annotation '" + jsonAddresses + "'", e);
            return Collections.emptySet();
        }
    }
}
