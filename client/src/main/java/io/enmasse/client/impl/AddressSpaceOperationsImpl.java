/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class AddressSpaceOperationsImpl extends EnmasseReadyOperationsImpl<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>>
        implements Resource<AddressSpace, DoneableAddressSpace> {

    public AddressSpaceOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public AddressSpaceOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(CoreCrd.GROUP)
                .withApiGroupVersion(CoreCrd.VERSION));
        this.apiGroupName = CoreCrd.GROUP;
        this.apiVersion = CoreCrd.API_VERSION;
        this.type = AddressSpace.class;
        this.listType = AddressSpaceList.class;
        this.doneableType = DoneableAddressSpace.class;
        this.cascading(true);
    }

    @Override
    public AddressSpaceOperationsImpl newInstance(OperationContext context) {
        return new AddressSpaceOperationsImpl(context);
    }

    @Override
    protected boolean isReady(AddressSpace resource) {
        return resource != null
                && resource.getStatus() != null
                && resource.getStatus().isReady()
                && resource.getStatus().getEndpointStatuses() != null;
    }

}
