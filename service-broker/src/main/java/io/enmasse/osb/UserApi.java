/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.osb;

import io.enmasse.user.model.v1.User;

public interface UserApi {
    void createOrReplace(User user);
    boolean deleteUser(String namespace, String name);
}
