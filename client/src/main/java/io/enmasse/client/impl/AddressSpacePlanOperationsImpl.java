/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanList;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.DoneableAddressSpacePlan;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class AddressSpacePlanOperationsImpl extends EnmasseReadyOperationsImpl<AddressSpacePlan, AddressSpacePlanList, DoneableAddressSpacePlan, Resource<AddressSpacePlan, DoneableAddressSpacePlan>>
        implements Resource<AddressSpacePlan, DoneableAddressSpacePlan> {

    public AddressSpacePlanOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public AddressSpacePlanOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(AdminCrd.GROUP)
                .withApiGroupVersion(AdminCrd.VERSION_V1BETA2));
        this.apiGroupName = AdminCrd.GROUP;
        this.apiVersion = AdminCrd.API_VERSION_V1BETA2;
        this.type = AddressSpacePlan.class;
        this.listType = AddressSpacePlanList.class;
        this.doneableType = DoneableAddressSpacePlan.class;
        this.cascading(true);
    }

    @Override
    public AddressSpacePlanOperationsImpl newInstance(OperationContext context) {
        return new AddressSpacePlanOperationsImpl(context);
    }

    @Override
    protected boolean isReady(AddressSpacePlan resource) {
        return resource != null;
    }

}
