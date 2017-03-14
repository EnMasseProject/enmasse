/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package enmasse.address.controller.admin;

import enmasse.address.controller.generator.DestinationClusterGenerator;
import enmasse.address.controller.generator.TemplateDestinationClusterGenerator;
import enmasse.address.controller.generator.TemplateParameter;
import enmasse.address.controller.model.TenantId;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.DoneableTemplate;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.ParameterValue;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages namespaces and infrastructure for a single tenant.
 */
public class AddressManagerFactoryImpl implements AddressManagerFactory {
    private final OpenShiftClient openShiftClient;
    private final TenantClientFactory clientFactory;
    private final FlavorRepository flavorRepository;
    private final boolean isMultitenant;
    private final String tenantTemplateName;

    public AddressManagerFactoryImpl(OpenShiftClient openShiftClient, TenantClientFactory clientFactory, FlavorRepository flavorRepository, boolean isMultitenant, boolean useTLS) {
        this.openShiftClient = openShiftClient;
        this.clientFactory = clientFactory;
        this.flavorRepository = flavorRepository;
        this.isMultitenant = isMultitenant;
        this.tenantTemplateName = useTLS ? "tls-enmasse-tenant-infra" : "enmasse-tenant-infra";
    }

    @Override
    public Optional<AddressManager> getAddressManager(TenantId tenant) {
        return hasTenant(tenant) ? Optional.of(createManager(tenant)) : Optional.empty();
    }

    private AddressManager createManager(TenantId tenant) {
        OpenShiftClient tenantClient = createTenantClient(tenant);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(tenant, tenantClient, flavorRepository);
        return new AddressManagerImpl(new OpenShiftHelper(tenant, tenantClient), generator);
    }

    private OpenShiftClient createTenantClient(TenantId tenant) {
        if (isMultitenant) {
            return clientFactory.createClient(tenant);
        } else {
            return openShiftClient;
        }
    }

    @Override
    public AddressManager getOrCreateAddressManager(TenantId tenant) {
        if (hasTenant(tenant)) {
            return createManager(tenant);
        } else {
            return deployTenant(tenant);
        }
    }

    private void createNamespace(TenantId tenant) {
        if (isMultitenant) {
            openShiftClient.namespaces().createNew()
                    .editMetadata()
                    .withName("enmasse-" + tenant.toString())
                    .addToLabels("app", "enmasse")
                    .addToLabels("tenant", tenant.toString())
                    .endMetadata()
                    .done();
        }
    }

    private boolean hasTenant(TenantId tenant) {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("app", "enmasse");
        labelMap.put("tenant", tenant.toString());
        if (isMultitenant) {
            return !openShiftClient.namespaces().withLabels(labelMap).list().getItems().isEmpty();
        } else {
            return !openShiftClient.services().withLabels(labelMap).list().getItems().isEmpty();
        }
    }

    private AddressManager deployTenant(TenantId tenant) {
        createNamespace(tenant);

        ClientTemplateResource<Template, KubernetesList, DoneableTemplate> templateProcessor = openShiftClient.templates().withName(tenantTemplateName);

        Map<String, String> paramMap = new LinkedHashMap<>();

        // If the flavor is shared, there is only one instance of it, so give it the name of the flavor
        paramMap.put(TemplateParameter.TENANT, OpenShiftHelper.nameSanitizer(tenant.toString()));

        ParameterValue parameters[] = new ParameterValue[1];
        parameters[0] = new ParameterValue(TemplateParameter.TENANT, OpenShiftHelper.nameSanitizer(tenant.toString()));
        KubernetesList items = templateProcessor.process(parameters);

        OpenShiftClient tenantClient = createTenantClient(tenant);
        tenantClient.lists().create(items);

        DestinationClusterGenerator generator = new TemplateDestinationClusterGenerator(tenant, tenantClient, flavorRepository);
        return new AddressManagerImpl(new OpenShiftHelper(tenant, tenantClient), generator);
    }
}
