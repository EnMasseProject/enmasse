/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanList;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.DoneableAddressPlan;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class AddressPlanOperationsImpl extends EnmasseReadyOperationsImpl<AddressPlan, AddressPlanList, DoneableAddressPlan, Resource<AddressPlan, DoneableAddressPlan>>
        implements Resource<AddressPlan, DoneableAddressPlan> {

    public AddressPlanOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public AddressPlanOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(AdminCrd.GROUP)
                .withApiGroupVersion(AdminCrd.VERSION_V1BETA1));
        this.apiGroupName = AdminCrd.GROUP;
        this.apiVersion = AdminCrd.VERSION_V1BETA1;
        this.type = AddressPlan.class;
        this.listType = AddressPlanList.class;
        this.doneableType = DoneableAddressPlan.class;
        this.cascading(true);
    }

    @Override
    public AddressPlanOperationsImpl newInstance(OperationContext context) {
        return new AddressPlanOperationsImpl(context);
    }

    @Override
    protected boolean isReady(AddressPlan resource) {
        return resource != null;
    }

}
