/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigList;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.DoneableBrokeredInfraConfig;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class BrokeredInfraConfigOperationsImpl extends EnmasseReadyOperationsImpl<BrokeredInfraConfig, BrokeredInfraConfigList, DoneableBrokeredInfraConfig, Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig>>
        implements Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig> {

    public BrokeredInfraConfigOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public BrokeredInfraConfigOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(AdminCrd.GROUP)
                .withApiGroupVersion(AdminCrd.VERSION_V1BETA1));
        this.apiGroupName = AdminCrd.GROUP;
        this.apiVersion = AdminCrd.API_VERSION_V1BETA1;
        this.type = BrokeredInfraConfig.class;
        this.listType = BrokeredInfraConfigList.class;
        this.doneableType = DoneableBrokeredInfraConfig.class;
        this.cascading(true);
    }

    @Override
    public BrokeredInfraConfigOperationsImpl newInstance(OperationContext context) {
        return new BrokeredInfraConfigOperationsImpl(context);
    }

    @Override
    protected boolean isReady(BrokeredInfraConfig resource) {
        return resource != null;
    }

}
