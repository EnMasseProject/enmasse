/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.k8s.api.SchemaProvider;

public class MigrationController implements Controller {
    private final SchemaProvider schemaProvider;
    private final String version;

    public MigrationController(SchemaProvider schemaProvider, String version) {
        this.schemaProvider = schemaProvider;
        this.version = version;
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
        if (version.startsWith("0.24")) {
            return migratePlanName(addressSpace);
        } else {
            return addressSpace;
        }
    }

    private AddressSpace migratePlanName(AddressSpace addressSpace) {
        AddressSpaceResolver resolver = new AddressSpaceResolver(schemaProvider.getSchema());
        AddressSpacePlan plan = resolver.getPlan(addressSpace.getType(), addressSpace.getPlan())
                .orElse(null);

        if (plan == null) {
            AddressSpace.Builder builder = new AddressSpace.Builder(addressSpace);
            if ("unlimited-brokered".equals(addressSpace.getPlan())) {
                builder.setPlan("brokered-single-broker");
            } else if ("unlimited-standard".equals(addressSpace.getPlan())) {
                builder.setPlan("standard-unlimited-with-mqtt");
            } else if ("unlimited-standard-without-mqtt".equals(addressSpace.getPlan())) {
                builder.setPlan("standard-unlimited");
            }
            return builder.build();
        } else {
            return addressSpace;
        }
    }

    @Override
    public String toString() {
        return "MigrationController";
    }
}
