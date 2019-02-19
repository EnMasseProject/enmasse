/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class UserList extends AbstractList<User> {

    public static final String KIND = "MessagingUserList";

    public UserList() {
        super(KIND, UserCrd.API_VERSION);
    }
}
