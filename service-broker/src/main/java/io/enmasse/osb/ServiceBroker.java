/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.osb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.CoreCrd;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.CachingSchemaProvider;
import io.enmasse.k8s.api.KubeAddressSpaceApi;
import io.enmasse.k8s.api.KubeSchemaApi;
import io.enmasse.k8s.api.SchemaApi;
import io.enmasse.osb.api.provision.ConsoleProxy;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserCrd;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class ServiceBroker extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServiceBroker.class.getName());
    private final NamespacedKubernetesClient client;
    private final ServiceBrokerOptions options;

    static {
        try {
            CoreCrd.registerCustomCrds();
            AdminCrd.registerCustomCrds();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw new ExceptionInInitializerError(t);
        }
    }

    private ServiceBroker(ServiceBrokerOptions options) {
        this.client = new DefaultKubernetesClient();
        this.options = options;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        SchemaApi schemaApi = KubeSchemaApi.create(client, client.getNamespace(), options.getVersion(), true, false);
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        ensureRouteExists(client.adapt(OpenShiftClient.class), options);
        ensureCredentialsExist(client, options);

        AddressSpaceApi addressSpaceApi = KubeAddressSpaceApi.create(client, null, options.getVersion());
        AuthApi authApi = new KubeAuthApi(client, client.getConfiguration().getOauthToken());

        UserApi userApi = createUserApi();

        ConsoleProxy consoleProxy = addressSpace -> {
            Route route = client.adapt(OpenShiftClient.class).routes().inNamespace(client.getNamespace()).withName(options.getConsoleProxyRouteName()).get();
            if (route == null) {
                return null;
            }
            return String.format("https://%s/console/%s/%s", route.getSpec().getHost(), addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
        };

        vertx.deployVerticle(new HTTPServer(addressSpaceApi, schemaProvider, authApi, options.getCertDir(), options.getEnableRbac(), userApi, options.getListenPort(), consoleProxy),
                result -> {
                    if (result.succeeded()) {
                        log.info("EnMasse Service Broker started");
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }

    private void ensureCredentialsExist(NamespacedKubernetesClient client, ServiceBrokerOptions options) {
        Secret secret = client.secrets().withName(options.getServiceCatalogCredentialsSecretName()).get();
        if (secret == null) {
            client.secrets().createNew()
                    .editOrNewMetadata()
                    .withName(options.getServiceCatalogCredentialsSecretName())
                    .addToLabels("app", "enmasse")
                    .endMetadata()
                    .addToData("token", Base64.getEncoder().encodeToString(client.getConfiguration().getOauthToken().getBytes(StandardCharsets.UTF_8)))
                    .done();
        }
    }

    private UserApi createUserApi() {
        var userClient = client.customResources(UserCrd.messagingUser(), User.class, UserList.class, DoneableUser.class);
        return new UserApi() {
            @Override
            public void createOrReplace(User user) {
                userClient.inNamespace(user.getMetadata().getNamespace()).create(user);
            }

            @Override
            public boolean deleteUser(String namespace, String name) {
                return userClient.inNamespace(namespace).withName(name).delete();
            }
        };
    }

    private static void ensureRouteExists(OpenShiftClient client, ServiceBrokerOptions serviceBrokerOptions) throws IOException {
        Route proxyRoute = client.routes().inNamespace(client.getNamespace()).withName(serviceBrokerOptions.getConsoleProxyRouteName()).get();
        if (proxyRoute == null) {
            String caCertificate = new String(Files.readAllBytes(new File(serviceBrokerOptions.getCertDir(), "tls.crt").toPath()), StandardCharsets.UTF_8);
            client.routes().createNew()
                    .editOrNewMetadata()
                    .withName(serviceBrokerOptions.getConsoleProxyRouteName())
                    .addToLabels("app", "enmasse")
                    .endMetadata()
                    .editOrNewSpec()
                    .editOrNewPort()
                    .withNewTargetPort("https")
                    .endPort()
                    .editOrNewTo()
                    .withKind("Service")
                    .withName("service-broker")
                    .endTo()
                    .editOrNewTls()
                    .withTermination("reencrypt")
                    .withCaCertificate(caCertificate)
                    .endTls()
                    .endSpec()
                    .done();
        }
    }

    public static void main(String args[]) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new ServiceBroker(ServiceBrokerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting service broker: " + e.getMessage());
            System.exit(1);
        }
    }
}
