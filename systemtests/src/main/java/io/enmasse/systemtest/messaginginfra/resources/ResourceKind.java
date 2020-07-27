/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

public interface ResourceKind {
    String MESSAGING_ADDRESS = "MessagingAddress";
    String MESSAGING_INFRASTRUCTURE = "MessagingInfrastructure";
    String MESSAGING_PROJECT = "MessagingProject";
    String MESSAGING_ENDPOINT = "MessagingEndpoint";
}
