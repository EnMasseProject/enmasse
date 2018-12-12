/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.enmasse.common.api.model.AbstractList;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@JsonPropertyOrder({"apiVersion", "kind", "metadata"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserList extends AbstractList<User> {

    private static final long serialVersionUID = 1L;

    public static final String KIND = "MessagingUserList";
    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "user.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    public UserList() {
        super(KIND, API_VERSION);
    }
}
