/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ExportsController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(ExportsController.class.getName());

    private final KubernetesClient client;

    public ExportsController(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public AddressSpace reconcile(AddressSpace addressSpace) {

        if ( Controller.isDeleted(addressSpace)) {
            return addressSpace;
        }

        try {
            Map<String, List<ExportSpec>> exportsMap = new HashMap<>();
            for (EndpointSpec endpointSpec : addressSpace.getSpec().getEndpoints()) {
                if (endpointSpec.getExports() != null) {
                    exportsMap.put(endpointSpec.getName(), endpointSpec.getExports());
                }
            }

            for (EndpointStatus endpointStatus : addressSpace.getStatus().getEndpointStatuses()) {
                List<ExportSpec> exports = exportsMap.get(endpointStatus.getName());
                if (exports != null) {
                    for (ExportSpec export : exports) {
                        switch (export.getKind()) {
                            case Secret:
                                exportAsSecret(export.getName(), endpointStatus, addressSpace);
                                break;
                            case ConfigMap:
                                exportAsConfigMap(export.getName(), endpointStatus, addressSpace);
                                break;
                            case Service:
                                exportAsService(export.getName(), endpointStatus, addressSpace);
                                break;
                            default:
                                log.info("Unknown export kind {} for address space {}, ignoring", export.getKind(), addressSpace.getMetadata().getName());
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error exporting endpoints for address space {}", addressSpace.getMetadata().getName(), e);
        }
        return addressSpace;
    }

    private void exportAsSecret(String name, EndpointStatus endpointStatus, AddressSpace addressSpace) {
        Map<String, String> exportMap = buildExportMap(addressSpace.getStatus(), endpointStatus);
        Secret secret = new SecretBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .addToOwnerReferences(new OwnerReferenceBuilder()
                        .withBlockOwnerDeletion(false)
                        .withApiVersion(addressSpace.getApiVersion())
                        .withController(true)
                        .withKind(addressSpace.getKind())
                        .withName(addressSpace.getMetadata().getName())
                        .withUid(addressSpace.getMetadata().getUid())
                        .build())
                .endMetadata()
                .addToStringData(exportMap)
                .build();

        Secret existing = client.secrets().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).get();
        if (existing != null) {
            Map<String, String> decodedExportMap = decodeExportMap(existing.getData());
            if (!decodedExportMap.equals(exportMap)) {
                client.secrets().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).replace(secret);
            }
        } else {
            client.secrets().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).createOrReplace(secret);
        }
    }

    private void exportAsConfigMap(String name, EndpointStatus endpointStatus, AddressSpace addressSpace) {
        Map<String, String> exportMap = buildExportMap(addressSpace.getStatus(), endpointStatus);
        ConfigMap configMap = new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .addToOwnerReferences(new OwnerReferenceBuilder()
                        .withBlockOwnerDeletion(false)
                        .withApiVersion(addressSpace.getApiVersion())
                        .withController(true)
                        .withKind(addressSpace.getKind())
                        .withName(addressSpace.getMetadata().getName())
                        .withUid(addressSpace.getMetadata().getUid())
                        .build())
                .endMetadata()
                .addToData(exportMap)
                .build();

        ConfigMap existing = client.configMaps().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).get();
        if (existing != null && !exportMap.equals(existing.getData())) {
            client.configMaps().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).replace(configMap);
        } else {
            client.configMaps().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).createOrReplace(configMap);
        }
    }

    private void exportAsService(String name, EndpointStatus endpointStatus, AddressSpace addressSpace) {
        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .addToOwnerReferences(new OwnerReferenceBuilder()
                        .withBlockOwnerDeletion(false)
                        .withApiVersion(addressSpace.getApiVersion())
                        .withController(true)
                        .withKind(addressSpace.getKind())
                        .withName(addressSpace.getMetadata().getName())
                        .withUid(addressSpace.getMetadata().getUid())
                        .build())
                .endMetadata()
                .editOrNewSpec()
                .withType("ExternalName")
                .withExternalName(endpointStatus.getServiceHost() + ".cluster.local")
                .withPorts(ServiceHelper.toServicePortList(endpointStatus.getServicePorts()))
                .endSpec()
                .build();

        Service existing = client.services().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).get();
        if (existing != null &&
                (!endpointStatus.getServiceHost().equals(existing.getSpec().getExternalName())
                        || !endpointStatus.getServicePorts().equals(ServiceHelper.fromServicePortList(existing.getSpec().getPorts())))) {
            client.services().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).replace(service);
        } else {
            client.services().inNamespace(addressSpace.getMetadata().getNamespace()).withName(name).createOrReplace(service);
        }
    }

    private static Map<String, String> buildExportMap(AddressSpaceStatus addressSpaceStatus, EndpointStatus endpointStatus) {
        Map<String, String> map = new HashMap<>();
        map.put("service.host", endpointStatus.getServiceHost());
        for (Map.Entry<String, Integer> portEntry : endpointStatus.getServicePorts().entrySet()) {
            map.put("service.port." + portEntry.getKey(), String.valueOf(portEntry.getValue()));
        }
        if (endpointStatus.getExternalHost() != null) {
            map.put("external.host", endpointStatus.getExternalHost());
        }
        for (Map.Entry<String, Integer> portEntry : endpointStatus.getExternalPorts().entrySet()) {
            map.put("external.port." + portEntry.getKey(), String.valueOf(portEntry.getValue()));
        }

        if (addressSpaceStatus.getCaCert() != null) {
            map.put("ca.crt", addressSpaceStatus.getCaCert());
        }
        return map;
    }


    private static Map<String, String> decodeExportMap(Map<String, String> data) {
        Map<String, String> exportData = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            exportData.put(entry.getKey(), new String(Base64.getDecoder().decode(entry.getValue()), StandardCharsets.UTF_8));
        }
        return exportData;
    }

    @Override
    public String toString() {
        return "ExportsController";
    }
}
