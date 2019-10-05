/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client.impl;

import io.enmasse.client.EnmasseReadyOperationsImpl;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class UserOperationsImpl extends EnmasseReadyOperationsImpl<User, UserList, DoneableUser, Resource<User, DoneableUser>>
        implements Resource<User, DoneableUser> {

    public UserOperationsImpl(OkHttpClient client, Config config) {
        this((new OperationContext()).withOkhttpClient(client).withConfig(config));
    }

    public UserOperationsImpl(OperationContext context) {
        super(context.withApiGroupName(UserCrd.GROUP)
                .withApiGroupVersion(UserCrd.VERSION));
        this.apiGroupName = UserCrd.GROUP;
        this.apiVersion = UserCrd.API_VERSION;
        this.type = User.class;
        this.listType = UserList.class;
        this.doneableType = DoneableUser.class;
        this.cascading(true);
    }

    @Override
    public UserOperationsImpl newInstance(OperationContext context) {
        return new UserOperationsImpl(context);
    }

    @Override
    protected boolean isReady(User resource) {
        return resource != null;
    }

}
