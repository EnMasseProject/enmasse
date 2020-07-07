/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.systemtest.platform.Kubernetes;

public final class DeviceManagementApi {

    public static final String CLUSTER_ROLE_NAME = "test.iot.enmasse.io:device-manager";
    public static final String SERVICE_ACCOUNT_NAME = "iot-device-management";

    private DeviceManagementApi() {}

    /**
     * Get the token for managing devices.
     *
     * @return The token, which can be used as a "Bearer Token".
     */
    public static String getManagementToken() {
        return Kubernetes.getServiceAccountToken(Kubernetes.getInstance().getInfraNamespace(), SERVICE_ACCOUNT_NAME);
    }

    public static void createManagementServiceAccount(final String namespace) throws Exception {

        var client = Kubernetes.getClient();

        // create a service account for working with devices

        client
                .serviceAccounts()
                .inNamespace(namespace)

                .createOrReplaceWithNew()

                .withNewMetadata()
                .withNamespace(namespace)
                .withName(SERVICE_ACCOUNT_NAME)
                .addToLabels("app", "enmasse")
                .addToLabels("component", "iot")
                .endMetadata()

                .done();

        // create a cluster wide role

        client
                .rbac().clusterRoles()

                .createOrReplaceWithNew()

                .withNewMetadata()
                .withName(CLUSTER_ROLE_NAME)
                .addToLabels("app", "enmasse")
                .addToLabels("component", "iot")
                .endMetadata()

                .addNewRule()
                .withApiGroups(IoTCrd.GROUP)
                .withResources(IoTCrd.project().getSpec().getNames().getPlural().toLowerCase())
                .withVerbs("create", "get", "list", "patch", "update", "delete")
                .endRule()

                .done();

        // map the role to the service account

        client
                .rbac().clusterRoleBindings()
                .createOrReplaceWithNew()

                .withNewMetadata()
                .withName(CLUSTER_ROLE_NAME + "-" + SERVICE_ACCOUNT_NAME)
                .addToLabels("app", "enmasse")
                .addToLabels("component", "iot")
                .endMetadata()

                .withNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(CLUSTER_ROLE_NAME)
                .endRoleRef()

                .addNewSubject()
                .withKind("ServiceAccount")
                .withNamespace(namespace)
                .withName(SERVICE_ACCOUNT_NAME)
                .endSubject()

                .done();
    }
}
