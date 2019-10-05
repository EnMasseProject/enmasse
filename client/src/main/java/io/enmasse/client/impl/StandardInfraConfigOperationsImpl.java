/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigList;
import io.enmasse.admin.model.v1.DoneableStandardInfraConfig;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class StandardInfraConfigOperationsImpl extends EnmasseReadyOperationsImpl<StandardInfraConfig, StandardInfraConfigList, DoneableStandardInfraConfig, Resource<StandardInfraConfig, DoneableStandardInfraConfig>>
        implements Resource<StandardInfraConfig, DoneableStandardInfraConfig> {

    public StandardInfraConfigOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public StandardInfraConfigOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(AdminCrd.GROUP)
                .withApiGroupVersion(AdminCrd.VERSION_V1BETA1));
        this.apiGroupName = AdminCrd.GROUP;
        this.apiVersion = AdminCrd.API_VERSION_V1BETA1;
        this.type = StandardInfraConfig.class;
        this.listType = StandardInfraConfigList.class;
        this.doneableType = DoneableStandardInfraConfig.class;
        this.cascading(true);
    }

    @Override
    public StandardInfraConfigOperationsImpl newInstance(OperationContext context) {
        return new StandardInfraConfigOperationsImpl(context);
    }

    @Override
    protected boolean isReady(StandardInfraConfig resource) {
        return resource != null;
    }

}
