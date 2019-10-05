/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.DoneableConsoleService;
import io.enmasse.admin.model.v1.ConsoleService;
import io.enmasse.admin.model.v1.ConsoleServiceList;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class ConsoleServiceOperationsImpl extends EnmasseReadyOperationsImpl<ConsoleService, ConsoleServiceList, DoneableConsoleService, Resource<ConsoleService, DoneableConsoleService>>
        implements Resource<ConsoleService, DoneableConsoleService> {

    public ConsoleServiceOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public ConsoleServiceOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(AdminCrd.GROUP)
                .withApiGroupVersion(AdminCrd.VERSION_V1BETA1));
        this.apiGroupName = AdminCrd.GROUP;
        this.apiVersion = AdminCrd.API_VERSION_V1BETA1;
        this.type = ConsoleService.class;
        this.listType = ConsoleServiceList.class;
        this.doneableType = DoneableConsoleService.class;
        this.cascading(true);
    }

    @Override
    public ConsoleServiceOperationsImpl newInstance(OperationContext context) {
        return new ConsoleServiceOperationsImpl(context);
    }

    @Override
    protected boolean isReady(ConsoleService resource) {
        return resource != null
                && resource.getStatus() != null
                && resource.getStatus().getUrl() != null;
    }

}
