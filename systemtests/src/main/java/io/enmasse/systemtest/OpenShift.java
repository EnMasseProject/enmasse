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
package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles interaction with openshift cluster
 */
public class OpenShift extends Kubernetes {

    public OpenShift(Environment environment, String globalNamespace) {
        super(environment, new DefaultOpenShiftClient(new ConfigBuilder().withMasterUrl(environment.openShiftUrl())
                .withOauthToken(environment.openShiftToken())
                .withUsername(environment.openShiftUser()).build()), globalNamespace);
    }

    public Endpoint getRestEndpoint() {
        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        Route route = openShift.routes().inNamespace(globalNamespace).withName("restapi").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(globalNamespace, "address-controller", "https");
        }
    }

    public Endpoint getKeycloakEndpoint() {
        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        Route route = openShift.routes().inNamespace(globalNamespace).withName("keycloak").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        Logging.log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(globalNamespace, "standard-authservice", "https");
        }
    }

    @Override
    public Endpoint getExternalEndpoint(String namespace, String endpointName) {
        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        Route route = openShift.routes().inNamespace(namespace).withName(endpointName).get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        Logging.log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            Logging.log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint(namespace, endpointName, "https");
        }
    }
}
