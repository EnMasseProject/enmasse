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

import enmasse.controller.cert.SelfSignedController;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.flavor.FlavorController;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.instance.InstanceController;
import enmasse.controller.instance.InstanceFactoryImpl;
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
    private final AddressManager addressManager;
    private final InstanceFactoryImpl instanceManager;
    private final FlavorController flavorController;
    private final InstanceController instanceController;
    private final AbstractVerticle certController;


    public Controller(ControllerOptions options) throws Exception {
        OpenShiftClient controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.masterUrl())
                .withOauthToken(options.token())
                .withNamespace(options.namespace())
                .build());

        Kubernetes kubernetes = new KubernetesHelper(InstanceId.withIdAndNamespace(options.namespace(), options.namespace()), controllerClient, options.templateDir());
        String templateName = "enmasse-instance-infra";

        FlavorManager flavorManager = new FlavorManager();
        this.instanceManager = new InstanceFactoryImpl(kubernetes, templateName, options.isMultiinstance());
        if (!options.isMultiinstance() && !kubernetes.hasService("messaging")) {
            Instance.Builder builder = new Instance.Builder(kubernetes.getInstanceId());
            builder.messagingHost(options.messagingHost());
            builder.mqttHost(options.mqttHost());
            builder.consoleHost(options.consoleHost());
            builder.certSecret(options.certSecret());
            instanceManager.create(builder.build());
        }

        this.addressManager = new AddressManagerImpl(kubernetes, flavorManager);
        this.server = new AMQPServer(kubernetes.getInstanceId(), addressManager, instanceManager, flavorManager, options.port());
        this.restServer = new HTTPServer(kubernetes.getInstanceId(), addressManager, instanceManager, flavorManager);
        this.flavorController = new FlavorController(controllerClient, flavorManager);
        this.instanceController = new InstanceController(controllerClient, kubernetes);
        this.certController = SelfSignedController.create(controllerClient);
    }

    @Override
    public void start() {
        vertx.deployVerticle(flavorController);
        vertx.deployVerticle(server);
        vertx.deployVerticle(restServer, new DeploymentOptions().setWorker(true));
        vertx.deployVerticle(instanceController);
        vertx.deployVerticle(certController);
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
