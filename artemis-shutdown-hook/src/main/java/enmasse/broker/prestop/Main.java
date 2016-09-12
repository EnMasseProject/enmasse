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

package enmasse.broker.prestop;

import com.openshift.internal.restclient.model.Pod;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IContainer;
import io.vertx.core.impl.FileResolver;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.util.Optional;

/**
 * @author Ulf Lilleengen
 */
public class Main {
    public static void main(String [] args) throws Exception {
        System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x");

        boolean debug = System.getenv("PRESTOP_DEBUG") != null;
        String address = System.getenv("QUEUE_NAME");

        String messagingHost = System.getenv("MESSAGING_SERVICE_HOST");
        int messagingPort = Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT"));

        String localHost = "127.0.0.1"; //Inet4Address.getLocalHost().getHostAddress();
        Endpoint from = new Endpoint(localHost, 5673);
        Endpoint to = new Endpoint(messagingHost, messagingPort);
        Endpoint mgmtEndpoint = new Endpoint(localHost, 61616);

        Optional<Runnable> debugFn = Optional.empty();
        if (debug) {
            String kubeHost = System.getenv("KUBERNETES_SERVICE_HOST");
            String kubePort = System.getenv("KUBERNETES_SERVICE_PORT");
            String kubeUrl = String.format("https://%s:%s", kubeHost, kubePort);
            IClient client = new ClientBuilder(kubeUrl).authorizationStrategy(new TokenAuthorizationStrategy(openshiftToken())).build();
            String namespace = openshiftNamespace();
            debugFn = Optional.of(() -> signalSuccess(client, namespace, address));
        }
        DrainClient client = new DrainClient(mgmtEndpoint, from, debugFn);

        // Only works for queues at the moment
        if (address != null) {
            client.drainMessages(to, address);
        } else {
            client.shutdownBroker();
        }
    }

    private static void signalSuccess(IClient osClient, String namespace, String address) {
        try {
            Pod pod = osClient.getResourceFactory().create("v1", ResourceKind.POD);
            pod.setName("mypod-" + address);
            pod.addContainer("containerfoo");
            IContainer cont = pod.getContainers().iterator().next();
            cont.setImage(new DockerImageURI("lulf/artemis:latest"));
            cont.addEnvVar("QUEUE_NAME", "myqueue");

            osClient.create(pod, namespace);
            Thread.sleep(10000);
        } catch (InterruptedException e) {}
    }

    private static final String serviceAccountPath = "/var/run/secrets/kubernetes.io/serviceaccount";
    private static String openshiftNamespace() throws IOException {
        return readFile(new File(serviceAccountPath, "namespace"));
    }

    private static String openshiftToken() throws IOException {
        return readFile(new File(serviceAccountPath, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
