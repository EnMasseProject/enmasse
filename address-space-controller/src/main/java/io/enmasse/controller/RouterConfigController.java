/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceSettings;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.RouterPolicySpec;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouter;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.router.config.*;
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RouterConfigController implements Controller {

    private final NamespacedKubernetesClient client;
    private final String namespace;
    private final AuthenticationServiceResolver authenticationServiceResolver;

    public RouterConfigController(NamespacedKubernetesClient client, String namespace, AuthenticationServiceResolver authenticationServiceResolver) {
        this.client = client;
        this.namespace = namespace;
        this.authenticationServiceResolver = authenticationServiceResolver;
    }

    public AddressSpace reconcile(AddressSpace addressSpace) throws Exception {

        InfraConfig infraConfig = InfraConfigs.parseCurrentInfraConfig(addressSpace);

        if (infraConfig instanceof StandardInfraConfig) {
            reconcileRouterConfig(addressSpace, (StandardInfraConfig) infraConfig);
        }
        return addressSpace;
    }

    private static String routerConfigName(String infraUuid) {
        return "qdrouterd-config." + infraUuid;
    }

    private static ConfigMapBuilder createNewConfigMap(String infraUuid) {
        // NOTE: Deletion of this configmap is handled by DeleteController based on these labels
        return new ConfigMapBuilder().editOrNewMetadata()
                .withName(routerConfigName(infraUuid))
                .addToLabels("app", "enmasse")
                .addToLabels("infraType", "standard")
                .addToLabels("infraUuid", infraUuid)
                .endMetadata();
    }

    private void reconcileRouterConfig(AddressSpace addressSpace, StandardInfraConfig infraConfig) throws IOException {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        ConfigMap config = client.configMaps().inNamespace(namespace).withName(routerConfigName(infraUuid)).get();
        RouterConfig current = null;
        if (config != null) {
            current = RouterConfig.fromMap(config.getData());
        }

        RouterConfig desired = generateConfig(infraUuid, authenticationServiceResolver.resolve(addressSpace), infraConfig);
        if (!desired.equals(current)) {
            Map<String, String> data = desired.toMap();
            if (config == null) {
                config = createNewConfigMap(infraUuid)
                        .withData(data)
                        .build();
                client.configMaps().inNamespace(namespace).withName(config.getMetadata().getName()).create(config);
            } else {
                config.setData(data);
                client.configMaps().inNamespace(namespace).withName(config.getMetadata().getName()).replace(config);
            }
        }
    }

    private static RouterConfig generateConfig(String infraUuid, AuthenticationServiceSettings authServiceSettings, StandardInfraConfig infraConfig) {
        Router router = new Router();
        StandardInfraConfigSpecRouter routerSpec = infraConfig.getSpec() != null ? infraConfig.getSpec().getRouter() : null;
        if (routerSpec != null && routerSpec.getWorkerThreads() != null) {
            router.setWorkerThreads(routerSpec.getWorkerThreads());
        }

        // SSL Profiles
        SslProfile authServiceSsl = new SslProfile();
        authServiceSsl.setName("auth_service_ssl");
        authServiceSsl.setCaCertFile("/etc/qpid-dispatch/authservice-ca/tls.crt");

        SslProfile sslDetails = new SslProfile();
        sslDetails.setName("ssl_details");
        sslDetails.setCertFile("/etc/qpid-dispatch/ssl/tls.crt");
        sslDetails.setPrivateKeyFile("/etc/qpid-dispatch/ssl/tls.key");

        SslProfile interRouterTls = new SslProfile();
        interRouterTls.setName("inter_router_tls");
        interRouterTls.setCaCertFile("/etc/enmasse-certs/ca.crt");
        interRouterTls.setCertFile("/etc/enmasse-certs/tls.crt");
        interRouterTls.setPrivateKeyFile("/etc/enmasse-certs/tls.key");

        // Authenticationservice plugin
        AuthServicePlugin authService = new AuthServicePlugin();
        authService.setName("auth_service");
        authService.setHost(authServiceSettings.getHost());
        authService.setPort(authServiceSettings.getPort());
        authService.setRealm(authServiceSettings.getRealm());
        authService.setSslProfile("auth_service_ssl");

        // Listeners
        Listener localBypass = new Listener();
        localBypass.setHost("127.0.0.1");
        localBypass.setPort(7777);
        localBypass.setAuthenticatePeer(false);

        Listener livenessProbe = new Listener();
        livenessProbe.setHost("127.0.0.1");
        livenessProbe.setPort(7770);
        livenessProbe.setAuthenticatePeer(false);
        livenessProbe.setHttp(true);
        livenessProbe.setMetrics(false);
        livenessProbe.setHealthz(true);
        livenessProbe.setWebsockets(false);
        livenessProbe.setHttpRootDir("invalid");

        Listener interRouter = new Listener();
        interRouter.setHost("0.0.0.0");
        interRouter.setPort(55672);
        interRouter.setRole(Role.inter_router);
        interRouter.setAuthenticatePeer(true);
        interRouter.setSslProfile("inter_router_tls");
        interRouter.setSaslMechanisms("EXTERNAL");
        if (routerSpec != null) {
            if (routerSpec.getIdleTimeout() != null) {
                interRouter.setIdleTimeoutSeconds(routerSpec.getIdleTimeout());
            }

            if (routerSpec.getLinkCapacity() != null) {
                interRouter.setLinkCapacity(routerSpec.getLinkCapacity());
            }
        }

        Listener httpsPublic = new Listener();
        httpsPublic.setHost("0.0.0.0");
        httpsPublic.setPort(8443);
        httpsPublic.setSaslPlugin("auth_service");
        httpsPublic.setSslProfile("ssl_details");
        httpsPublic.setHttp(true);
        httpsPublic.setAuthenticatePeer(true);
        if (routerSpec != null && routerSpec.getPolicy() != null) {
            httpsPublic.setPolicyVhost("public");
        }

        if (routerSpec != null) {
            if (routerSpec.getIdleTimeout() != null) {
                httpsPublic.setIdleTimeoutSeconds(routerSpec.getIdleTimeout());
            }

            if (routerSpec.getLinkCapacity() != null) {
                httpsPublic.setLinkCapacity(routerSpec.getLinkCapacity());
            }
        }

        Listener amqpPublic = new Listener();
        amqpPublic.setHost("0.0.0.0");
        amqpPublic.setPort(5672);
        amqpPublic.setSaslPlugin("auth_service");
        amqpPublic.setAuthenticatePeer(true);

        if (routerSpec != null && routerSpec.getPolicy() != null) {
            amqpPublic.setPolicyVhost("public");
        }

        if (routerSpec != null) {
            if (routerSpec.getIdleTimeout() != null) {
                amqpPublic.setIdleTimeoutSeconds(routerSpec.getIdleTimeout());
            }

            if (routerSpec.getLinkCapacity() != null) {
                amqpPublic.setLinkCapacity(routerSpec.getLinkCapacity());
            }

            if (routerSpec.getHandshakeTimeout() != null) {
                amqpPublic.setInitialHandshakeTimeoutSeconds(routerSpec.getHandshakeTimeout());
            }
        }

        Listener amqpsPublic = new Listener();
        amqpsPublic.setHost("0.0.0.0");
        amqpsPublic.setPort(5671);
        amqpsPublic.setSaslPlugin("auth_service");
        amqpsPublic.setSslProfile("ssl_details");
        amqpsPublic.setRequireSsl(true);
        amqpsPublic.setAuthenticatePeer(true);

        if (routerSpec != null && routerSpec.getPolicy() != null) {
            amqpsPublic.setPolicyVhost("public");
        }

        if (routerSpec != null) {
            if (routerSpec.getIdleTimeout() != null) {
                amqpsPublic.setIdleTimeoutSeconds(routerSpec.getIdleTimeout());
            }

            if (routerSpec.getLinkCapacity() != null) {
                amqpsPublic.setLinkCapacity(routerSpec.getLinkCapacity());
            }

            if (routerSpec.getHandshakeTimeout() != null) {
                amqpsPublic.setInitialHandshakeTimeoutSeconds(routerSpec.getHandshakeTimeout());
            }
        }


        Listener amqpsInternal = new Listener();
        amqpsInternal.setHost("0.0.0.0");
        amqpsInternal.setPort(55671);
        amqpsInternal.setSslProfile("inter_router_tls");
        amqpsInternal.setRequireSsl(true);
        amqpsInternal.setSaslMechanisms("EXTERNAL");
        amqpsInternal.setAuthenticatePeer(true);

        if (routerSpec != null) {
            if (routerSpec.getIdleTimeout() != null) {
                amqpsInternal.setIdleTimeoutSeconds(routerSpec.getIdleTimeout());
            }

            if (routerSpec.getLinkCapacity() != null) {
                amqpsInternal.setLinkCapacity(routerSpec.getLinkCapacity());
            }
        }

        Listener amqpsRouteContainer = new Listener();
        amqpsRouteContainer.setHost("0.0.0.0");
        amqpsRouteContainer.setPort(56671);
        amqpsRouteContainer.setSslProfile("inter_router_tls");
        amqpsRouteContainer.setRole(Role.route_container);
        amqpsRouteContainer.setRequireSsl(true);
        amqpsRouteContainer.setSaslMechanisms("EXTERNAL");
        amqpsRouteContainer.setAuthenticatePeer(true);

        if (routerSpec != null) {
            if (routerSpec.getIdleTimeout() != null) {
                amqpsRouteContainer.setIdleTimeoutSeconds(routerSpec.getIdleTimeout());
            }

            if (routerSpec.getLinkCapacity() != null) {
                amqpsRouteContainer.setLinkCapacity(routerSpec.getLinkCapacity());
            }
        }

        // Policies
        List<Policy> policies = Collections.emptyList();
        if (routerSpec != null && routerSpec.getPolicy() != null) {
            Policy policy = new Policy();
            policy.setEnableVhostPolicy(true);
            policies = Collections.singletonList(policy);
        }

        // VhostPolicy
        List<VhostPolicy> vhostPolicies = Collections.emptyList();
        if (routerSpec != null && routerSpec.getPolicy() != null) {
            vhostPolicies = createVhostPolices(routerSpec.getPolicy());
        }

        // Connectors
        Connector ragentConnector = new Connector();
        ragentConnector.setHost("ragent-" + infraUuid);
        ragentConnector.setPort(5671);
        ragentConnector.setSslProfile("inter_router_tls");
        ragentConnector.setVerifyHostname(false);

        // LinkRoutes
        LinkRoute mqttLwtInLinkRoute = new LinkRoute();
        mqttLwtInLinkRoute.setName("override.lwt_in");
        mqttLwtInLinkRoute.setPrefix("$${dummy}lwt");
        mqttLwtInLinkRoute.setDirection(LinkRoute.Direction.in);
        mqttLwtInLinkRoute.setContainerId("lwt-service");

        LinkRoute mqttLwtOutLinkRoute = new LinkRoute();
        mqttLwtOutLinkRoute.setName("override.lwt_out");
        mqttLwtOutLinkRoute.setPrefix("$${dummy}lwt");
        mqttLwtOutLinkRoute.setDirection(LinkRoute.Direction.out);
        mqttLwtOutLinkRoute.setContainerId("lwt-service");

        // Addresses
        Address mqttAddress = new Address();
        mqttAddress.setName("override.mqtt");
        mqttAddress.setPrefix("$${dummy}mqtt");
        mqttAddress.setDistribution(Distribution.balanced);

        Address subctrlAddress = new Address();
        subctrlAddress.setName("override.subctrl");
        subctrlAddress.setPrefix("$${dummy}subctrl");
        subctrlAddress.setDistribution(Distribution.balanced);

        Address tempAddress = new Address();
        tempAddress.setName("override.temp");
        tempAddress.setPrefix("$${dummy}temp");
        tempAddress.setDistribution(Distribution.balanced);

        return new RouterConfig(router,
                Arrays.asList(authServiceSsl, sslDetails, interRouterTls),
                Collections.singletonList(authService),
                Arrays.asList(localBypass, livenessProbe, httpsPublic, amqpPublic, amqpsPublic, amqpsInternal, amqpsRouteContainer, interRouter),
                policies,
                Collections.singletonList(ragentConnector),
                Arrays.asList(mqttLwtInLinkRoute, mqttLwtOutLinkRoute),
                Arrays.asList(mqttAddress, subctrlAddress, tempAddress),
                vhostPolicies);
    }

    static List<VhostPolicy> createVhostPolices(RouterPolicySpec policy) {
        // Public settings derived from infra config settings
        VhostPolicyGroup group = new VhostPolicyGroup();
        group.setRemoteHosts("*");
        group.setSources("*");
        group.setTargets("*");
        group.setAllowAnonymousSender(true);
        group.setAllowDynamicSource(true);

        if (policy.getMaxSessionsPerConnection() != null) {
            group.setMaxSessions(policy.getMaxSessionsPerConnection());
        }

        if (policy.getMaxSendersPerConnection() != null) {
            group.setMaxSenders(policy.getMaxSendersPerConnection());
        }

        if (policy.getMaxReceiversPerConnection() != null) {
            group.setMaxReceivers(policy.getMaxReceiversPerConnection());
        }

        VhostPolicy vhostPolicy = new VhostPolicy();
        vhostPolicy.setHostname("public");
        vhostPolicy.setAllowUnknownUser(true);

        if (policy.getMaxConnections() != null) {
            vhostPolicy.setMaxConnections(policy.getMaxConnections());
        }

        if (policy.getMaxConnectionsPerHost() != null) {
            vhostPolicy.setMaxConnectionsPerHost(policy.getMaxConnectionsPerHost());
        }

        if (policy.getMaxConnectionsPerUser() != null) {
            vhostPolicy.setMaxConnectionsPerUser(policy.getMaxConnectionsPerUser());
        }

        vhostPolicy.setGroups(Collections.singletonMap("$${dummy}default", group));

        VhostPolicyGroup internalGroup = new VhostPolicyGroup();
        internalGroup.setRemoteHosts("*");
        internalGroup.setSources("*");
        internalGroup.setTargets("*");
        internalGroup.setAllowDynamicSource(true);
        internalGroup.setAllowAnonymousSender(true);

        VhostPolicy internalVhostPolicy = new VhostPolicy();
        internalVhostPolicy.setHostname("$${dummy}default");
        internalVhostPolicy.setAllowUnknownUser(true);
        internalVhostPolicy.setGroups(Collections.singletonMap("$${dummy}default", internalGroup));

        return Arrays.asList(internalVhostPolicy, vhostPolicy);
    }

    @Override
    public String toString() {
        return "RouterConfigController";
    }
}
