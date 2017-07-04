package enmasse.controller.address.api;

import enmasse.config.LabelKeys;
import enmasse.config.AnnotationKeys;
import enmasse.controller.common.*;
import enmasse.controller.model.AddressSpaceId;
import io.enmasse.address.model.*;
import io.enmasse.address.model.impl.k8s.v1.address.DecodeContext;
import io.enmasse.address.model.impl.k8s.v1.address.AddressCodec;
import io.enmasse.address.model.impl.types.standard.StandardAddressDecodeContext;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConfigMapAddressApi implements AddressApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapAddressApi.class);
    private final Vertx vertx;
    private final OpenShiftClient client;
    private final AddressSpaceId addressSpaceId;
    private final AddressCodec addressCodec = new AddressCodec();

    // TODO: Parameterizing this could potentially make this class generic
    private final DecodeContext decodeContext = new StandardAddressDecodeContext();

    public ConfigMapAddressApi(Vertx vertx, OpenShiftClient client, AddressSpaceId addressSpaceId) {
        this.vertx = vertx;
        this.client = client;
        this.addressSpaceId = addressSpaceId;
    }

    @Override
    public Optional<Address> getAddressWithName(String address) {
        ConfigMap map = client.configMaps().inNamespace(addressSpaceId.getNamespace()).withName(Kubernetes.sanitizeName("address-config-" + address)).get();
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressFromConfig(map));
        }
    }

    @Override
    public Optional<Address> getAddressWithUuid(String uuid) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        labels.put(LabelKeys.UUID, uuid);

        ConfigMapList list = client.configMaps().inNamespace(addressSpaceId.getNamespace()).withLabels(labels).list();
        if (list.getItems().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(getAddressFromConfig(list.getItems().get(0)));
        }
    }

    @SuppressWarnings("unchecked")
    private Address getAddressFromConfig(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();

        try {
            return addressCodec.decodeAddress(decodeContext, data.get("json").getBytes("UTF-8"));
        } catch (Exception e) {
            log.warn("Unable to decode address", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Address> listAddresses() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");

        Set<Address> addresses = new LinkedHashSet<>();
        ConfigMapList list = client.configMaps().inNamespace(addressSpaceId.getNamespace()).withLabels(labels).list();
        for (ConfigMap config : list.getItems()) {
            addresses.add(getAddressFromConfig(config));
        }
        return addresses;
    }

    @Override
    public void createAddress(Address destination) {
        createOrReplace(destination);
    }

    @Override
    public void replaceAddress(Address address) {
        String name = Kubernetes.sanitizeName("address-config-" + address.getAddress());
        ConfigMap previous = client.configMaps().inNamespace(addressSpaceId.getNamespace()).withName(name).get();
        if (previous == null) {
            return;
        }
        createOrReplace(address);
    }

    private void createOrReplace(Address address) {
        String name = Kubernetes.sanitizeName("address-config-" + address.getAddress());
        DoneableConfigMap builder = client.configMaps().inNamespace(addressSpaceId.getNamespace()).withName(name).createOrReplaceWithNew()
                .withNewMetadata()
                .withName(name)
                .addToLabels(LabelKeys.TYPE, "address-config")
                // TODO: Support other ways of doing this
                .addToAnnotations(AnnotationKeys.CLUSTER_ID, name)
                .addToAnnotations(AnnotationKeys.INSTANCE, address.getAddressSpace())
                .endMetadata();

        try {
            builder.addToData("json", new String(addressCodec.encodeAddress(address), Charset.forName("UTF-8")));
        } catch (Exception e) {
            log.info("Error serializing address for {}", address.getAddress(), e);
        }
    }

    @Override
    public void deleteAddress(Address address) {
        String name = Kubernetes.sanitizeName("address-config-" + address.getAddress());
        client.configMaps().inNamespace(addressSpaceId.getNamespace()).withName(name).delete();
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LabelKeys.TYPE, "address-config");
        WatcherVerticle<Address> verticle = new WatcherVerticle<>(new Resource<Address>() {
            @Override
            public io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher w) {
                return client.configMaps().inNamespace(addressSpaceId.getNamespace()).withLabels(labels).watch(w);
            }

            @Override
            public Set<Address> listResources() {
                return listAddresses();
            }
        }, watcher);

        CompletableFuture<String> promise = new CompletableFuture<>();
        vertx.deployVerticle(verticle, result -> {
            if (result.succeeded()) {
                promise.complete(result.result());
            } else {
                log.error("Error deploying verticle: {}", result.cause());
                promise.completeExceptionally(result.cause());
            }
        });

        String id = promise.get(1, TimeUnit.MINUTES);
        return () -> vertx.undeploy(id);
    }
}
