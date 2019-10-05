/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class AddressOperationsImpl extends EnmasseReadyOperationsImpl<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>>
        implements Resource<Address, DoneableAddress> {

    public AddressOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public AddressOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(CoreCrd.GROUP)
                .withApiGroupVersion(CoreCrd.VERSION));
        this.apiGroupName = CoreCrd.GROUP;
        this.apiVersion = CoreCrd.API_VERSION;
        this.type = Address.class;
        this.listType = AddressList.class;
        this.doneableType = DoneableAddress.class;
        this.cascading(true);
    }

    @Override
    public AddressOperationsImpl newInstance(OperationContext context) {
        return new AddressOperationsImpl(context);
    }

    @Override
    protected boolean isReady(Address resource) {
        return resource != null
                && resource.getStatus() != null
                && resource.getStatus().isReady();
    }

}
