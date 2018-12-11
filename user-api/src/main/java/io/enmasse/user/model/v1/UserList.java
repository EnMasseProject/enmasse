/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.common.api.model.AbstractList;
import io.enmasse.common.api.model.ApiVersion;
import io.enmasse.common.api.model.CustomResource;

@ApiVersion("v1alpha1")
@CustomResource(group = "user.enmasse.io", kind="MessagingUserList")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserList extends AbstractList<User>{
}
