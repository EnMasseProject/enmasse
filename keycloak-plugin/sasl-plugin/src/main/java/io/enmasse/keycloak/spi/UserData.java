/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import java.util.Set;

public interface UserData {
    String getId();
    String getUsername();
    Set<String> getGroups();

}
