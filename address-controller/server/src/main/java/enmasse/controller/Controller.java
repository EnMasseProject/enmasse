/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.controller;

import enmasse.controller.address.AddressController;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.flavor.FlavorController;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.instance.InstanceController;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.instance.InstanceManagerImpl;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.instance.api.InstanceApiImpl;
import enmasse.controller.instance.cert.CertManager;
import enmasse.controller.instance.cert.SelfSignedCertManager;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class Controller extends AbstractVerticle {
    private final AMQPServer server;
    private final HTTPServer restServer;
    private final FlavorController flavorController;
    private final InstanceController instanceController;
    private final AddressController addressController;


    public Controller(ControllerOptions options) throws Exception {
        OpenShiftClient controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.masterUrl())
                .withOauthToken(options.token())
                .withNamespace(options.namespace())
                .build());

        Kubernetes kubernetes = new KubernetesHelper(InstanceId.withIdAndNamespace(options.namespace(), options.namespace()), controllerClient, options.templateDir());
        String templateName = "enmasse-instance-infra";

        FlavorManager flavorManager = new FlavorManager();
        InstanceManager instanceManager = new InstanceManagerImpl(kubernetes, templateName, options.isMultiinstance());
        InstanceApi instanceApi = new InstanceApiImpl(controllerClient);

        if (!options.isMultiinstance() && !kubernetes.hasService("messaging")) {
            Instance.Builder builder = new Instance.Builder(kubernetes.getInstanceId());
            builder.messagingHost(options.messagingHost());
            builder.mqttHost(options.mqttHost());
            builder.consoleHost(options.consoleHost());
            options.certSecret().ifPresent(builder::certSecret);
            instanceApi.createInstance(builder.build());
        }

        this.addressController = new AddressController(instanceApi, kubernetes, controllerClient, flavorManager);
        this.server = new AMQPServer(kubernetes.getInstanceId(), instanceApi, flavorManager, options.port());
        this.restServer = new HTTPServer(kubernetes.getInstanceId(), instanceApi, flavorManager);
        this.flavorController = new FlavorController(controllerClient, flavorManager);

        CertManager certManager = SelfSignedCertManager.create(controllerClient);
        this.instanceController = new InstanceController(instanceManager, controllerClient, instanceApi, certManager);
    }

    @Override
    public void start() {
        vertx.deployVerticle(flavorController);
        vertx.deployVerticle(server);
        vertx.deployVerticle(restServer, new DeploymentOptions().setWorker(true));
        vertx.deployVerticle(instanceController);
        vertx.deployVerticle(addressController);
    }

    public static void main(String args[]) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new Controller(ControllerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
