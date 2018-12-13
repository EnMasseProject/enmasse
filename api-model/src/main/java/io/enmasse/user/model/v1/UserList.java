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
    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "user.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    public UserList() {
        super(KIND, API_VERSION);
    }
}
