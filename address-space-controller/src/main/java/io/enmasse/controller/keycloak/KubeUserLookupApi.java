/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.User;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubeUserLookupApi implements UserLookupApi {
    private static final Logger log = LoggerFactory.getLogger(KubeUserLookupApi.class);
    private final NamespacedOpenShiftClient client;
    private final boolean isOpenShift;

    public KubeUserLookupApi(NamespacedOpenShiftClient client, boolean isOpenShift) {
        this.client = client;
        this.isOpenShift = isOpenShift;
    }

    @Override
    public String findUserId(String userName) {
        if (isOpenShift) {
            if (userName == null || userName.isEmpty() || userName.contains(":")) {
                return "";
            }
            try {
                User user = client.users().withName(userName).get();
                if (user == null) {
                    return "";
                }
                log.debug("Found user {} with id {}", user.getMetadata().getName(), user.getMetadata().getUid());
                return user.getMetadata().getUid();
            } catch (KubernetesClientException e) {
                log.warn("Exception looking up user id, returning empty", e);
                return "";
            }
        } else {
            return "";
        }
    }
}
