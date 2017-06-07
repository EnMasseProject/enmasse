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
import enmasse.controller.auth.AuthController;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.common.KubernetesHelper;
import enmasse.controller.flavor.FlavorController;
import enmasse.controller.flavor.FlavorManager;
import enmasse.controller.instance.InstanceController;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.instance.InstanceManagerImpl;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.instance.api.ConfigMapInstanceApi;
import enmasse.controller.auth.CertManager;
import enmasse.controller.auth.SelfSignedCertManager;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.*;

import java.util.ArrayList;
import java.util.List;

public class Controller extends AbstractVerticle {
    private final OpenShiftClient controllerClient;
    private final ControllerOptions options;
    private final Kubernetes kubernetes;

    public Controller(ControllerOptions options) throws Exception {
        this.controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.masterUrl())
                .withOauthToken(options.token())
                .withNamespace(options.namespace())
                .build());
        this.options = options;
        this.kubernetes = new KubernetesHelper(InstanceId.withIdAndNamespace(options.namespace(), options.namespace()), controllerClient, options.templateDir());
    }

    @Override
    public void start(Future<Void> startPromise) {
        InstanceApi instanceApi = new ConfigMapInstanceApi(vertx, controllerClient);

        if (!options.isMultiinstance() && !kubernetes.hasService("messaging")) {
            Instance.Builder builder = new Instance.Builder(kubernetes.getInstanceId());
            builder.messagingHost(options.messagingHost());
            builder.mqttHost(options.mqttHost());
            builder.consoleHost(options.consoleHost());
            options.certSecret().ifPresent(builder::certSecret);
            instanceApi.createInstance(builder.build());
        }

        FlavorManager flavorManager = new FlavorManager();
        CertManager certManager = SelfSignedCertManager.create(controllerClient);

        String templateName = "enmasse-instance-infra";
        InstanceManager instanceManager = new InstanceManagerImpl(kubernetes, templateName, options.isMultiinstance());

        deployVerticles(startPromise, new Deployment(new AddressController(instanceApi, kubernetes, controllerClient, flavorManager)),
                new Deployment(new AuthController(certManager, instanceApi)),
                new Deployment(new InstanceController(instanceManager, controllerClient, instanceApi)),
                new Deployment(new FlavorController(controllerClient, flavorManager)),
                new Deployment(new AMQPServer(kubernetes.getInstanceId(), instanceApi, flavorManager, options.port())),
                new Deployment(new HTTPServer(kubernetes.getInstanceId(), instanceApi, flavorManager, options.certDir()), new DeploymentOptions().setWorker(true)));
    }

    private void deployVerticles(Future<Void> startPromise, Deployment ... deployments) {
        List<Future> futures = new ArrayList<>();
        for (Deployment deployment : deployments) {
            Future<Void> promise = Future.future();
            futures.add(promise);
            vertx.deployVerticle(deployment.verticle, deployment.options, result -> {
                if (result.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(result.cause());
                }
            });
        }

        CompositeFuture.all(futures).setHandler(result -> {
            if (result.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    private static class Deployment {
        final Verticle verticle;
        final DeploymentOptions options;

        private Deployment(Verticle verticle) {
            this(verticle, new DeploymentOptions());
        }

        private Deployment(Verticle verticle, DeploymentOptions options) {
            this.verticle = verticle;
            this.options = options;
        }
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
