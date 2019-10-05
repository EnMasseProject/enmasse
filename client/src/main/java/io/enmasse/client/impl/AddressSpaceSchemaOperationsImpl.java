/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.address.model.AddressSpaceSchema;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.address.model.DoneableAddressSpaceSchema;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class AddressSpaceSchemaOperationsImpl extends EnmasseReadyOperationsImpl<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema, Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>>
        implements Resource<AddressSpaceSchema, DoneableAddressSpaceSchema> {

    public AddressSpaceSchemaOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public AddressSpaceSchemaOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(CoreCrd.GROUP)
                .withApiGroupVersion(CoreCrd.VERSION));
        this.apiGroupName = CoreCrd.GROUP;
        this.apiVersion = CoreCrd.API_VERSION ;
        this.type = AddressSpaceSchema.class;
        this.listType = AddressSpaceSchemaList.class;
        this.doneableType = DoneableAddressSpaceSchema.class;
        this.cascading(true);
    }

    @Override
    public AddressSpaceSchemaOperationsImpl newInstance(OperationContext context) {
        return new AddressSpaceSchemaOperationsImpl(context);
    }

    @Override
    protected boolean isReady(AddressSpaceSchema resource) {
        return resource != null;
    }

}
