/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.iot.service.base.IoTProjects;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class TestApplication {
    public static void main(String[] args) {

        Config config = Config.autoConfigure(null);

        DefaultKubernetesClient client = new DefaultKubernetesClient(config);

        var iot = IoTProjects.clientForProject(client);
        IoTProject project = iot.inNamespace("hono").withName("default").get();
        System.out.println(project);

    }
}
