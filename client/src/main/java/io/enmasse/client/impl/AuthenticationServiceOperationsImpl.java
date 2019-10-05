/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.admin.model.v1.DoneableAuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceList;
import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class AuthenticationServiceOperationsImpl extends EnmasseReadyOperationsImpl<AuthenticationService, AuthenticationServiceList, DoneableAuthenticationService, Resource<AuthenticationService, DoneableAuthenticationService>>
        implements Resource<AuthenticationService, DoneableAuthenticationService> {

    public AuthenticationServiceOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public AuthenticationServiceOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(AdminCrd.GROUP)
                .withApiGroupVersion(AdminCrd.VERSION_V1BETA1));
        this.apiGroupName = AdminCrd.GROUP;
        this.apiVersion = AdminCrd.API_VERSION_V1BETA1;
        this.type = AuthenticationService.class;
        this.listType = AuthenticationServiceList.class;
        this.doneableType = DoneableAuthenticationService.class;
        this.cascading(true);
    }

    @Override
    public AuthenticationServiceOperationsImpl newInstance(OperationContext context) {
        return new AuthenticationServiceOperationsImpl(context);
    }

    @Override
    protected boolean isReady(AuthenticationService resource) {
        return resource != null
                && resource.getStatus() != null;
    }

}
