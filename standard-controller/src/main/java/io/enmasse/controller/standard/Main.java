/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.controller.standard;

import io.enmasse.k8s.api.EventLogger;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;


/**
 * The standard controller is responsible for watching address spaces of type standard, creating
 * infrastructure required and propagating relevant status information.
 */

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();

        TemplateOptions templateOptions = new TemplateOptions(env);
        File templateDir = getEnv(env, "TEMPLATE_DIR")
                .map(File::new)
                .orElse(null);

        String certDir = getEnvOrThrow(env, "CERT_DIR");

        Kubernetes kubernetes = new KubernetesHelper(new DefaultOpenShiftClient(), templateDir);
        AddressClusterGenerator clusterGenerator = new TemplateAddressClusterGenerator(kubernetes, templateOptions);

        EventLogger eventLogger = kubernetes.createEventLogger(Clock.systemUTC(), "standard-controller");

        String addressSpace = getEnvOrThrow(env, "ADDRESS_SPACE");

        AddressController addressController = new AddressController(
                addressSpace,
                kubernetes.createAddressApi(),
                kubernetes,
                clusterGenerator,
                certDir,
                eventLogger);

        log.info("Deploying address space controller for " + addressSpace);
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(addressController, result -> {
            if (result.succeeded()) {
                log.info("Address space controller for {} deployed", addressSpace);
            } else {
                log.warn("Unable to deploy address controller for {}", addressSpace);
            }
        });
        vertx.createHttpServer()
                .requestHandler(request -> request.response().setStatusCode(200).end()).listen(8889);
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }
}
