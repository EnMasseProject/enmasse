/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MessagingUserFinalizerController extends AbstractFinalizerController {
    private static final Logger log = LoggerFactory.getLogger(AddressFinalizerController.class);
    public static final String FINALIZER_MESSAGING_USERS = "enmasse.io/messaging-users";

    private final MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> userClient;

    public MessagingUserFinalizerController(NamespacedKubernetesClient client) {
        this(client.customResources(UserCrd.messagingUser(), User.class, UserList.class, DoneableUser.class));
    }

    MessagingUserFinalizerController(MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> userClient) {
        super(FINALIZER_MESSAGING_USERS);
        this.userClient = userClient;
    }

    @Override
    public String toString() {
        return "MessagingUserFinalizerController";
    }

    @Override
    protected Result processFinalizer(AddressSpace addressSpace) {
        log.info("Processing messaging user finalizer for {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());

        try {
            final List<User> users = userClient.inNamespace(addressSpace.getMetadata().getNamespace()).list().getItems().stream()
                    .filter(user -> user.getMetadata().getName().startsWith(addressSpace.getMetadata().getName() + "."))
                    .collect(Collectors.toList());
            for (User user : users) {
                userClient.inNamespace(user.getMetadata().getNamespace()).withName(user.getMetadata().getName()).cascading(true).delete();
            }
            return Result.completed(addressSpace);
        } catch (KubernetesClientException e) {
            // If not found, the address CRD does not exist so we drop the finalizer
            if (e.getCode() == 404) {
                log.warn("Got 404 when listing users for {}/{}. Marking as finalized.", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), e);
                return Result.completed(addressSpace);
            } else {
                log.warn("Error finalizing {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), e);
                return Result.waiting(addressSpace);
            }
        }
    }
}
