/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const assert = require('assert');
const mock = require('../mock-console-server.js');

mock.setStateChangeTimeout(100);
describe('mock', function () {
    describe('create_address', function () {
        it('with_resource_name', function () {
            var a = mock.createAddress({
                metadata: {
                    name: "jupiter_as1.foo1",
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "fooaddr"
                }

            });

            assert.strictEqual(a.name, "jupiter_as1.foo1");
        });
        it('with_address', function () {
            var a = mock.createAddress({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "foo2"
                }

            }, "jupiter_as1");

            assert.strictEqual(a.name, "jupiter_as1.foo2");
        });
        it('with_address_uppercase', function () {
            var a = mock.createAddress({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "FOO3"
                }

            }, "jupiter_as1");

            assert.strictEqual(a.name, "jupiter_as1.foo3");
        });
        it('with_address_illegalchars', function () {
            var a = mock.createAddress({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "!foo4"
                }

            }, "jupiter_as1");

            assert.match(a.name, /^jupiter_as1\.foo4\..*$/);
        });
        it('with_deadletter', function () {
            var a = mock.createAddress({
                metadata: {
                    name: "jupiter_as1.foo4",
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "fooaddr",
                    deadletter: "kale"
                }

            });
            assert.strictEqual(a.name, "jupiter_as1.foo4");
        });
        it('with_invalid_deadletter', function () {
            assert.throws(() => {
                mock.createAddress({
                    metadata: {
                        name: "jupiter_as1.foo4",
                        namespace: "app1_ns"
                    },
                    spec: {
                        type: "queue",
                        plan: "standard-small-queue",
                        address: "fooaddr",
                        deadletter: "unknown"
                    }
                });

            }, /Unrecognised deadletter address 'unknown', known ones are : 'kale'/);
        });
        it('with_invalid_expiry', function () {
            assert.throws(() => {
                mock.createAddress({
                    metadata: {
                        name: "jupiter_as1.foo4",
                        namespace: "app1_ns"
                    },
                    spec: {
                        type: "queue",
                        plan: "standard-small-queue",
                        address: "fooaddr",
                        expiry: "unknown"
                    }
                });

            }, /Unrecognised expiry address 'unknown', known ones are : 'kale'/);
        });

    });
    describe('patchAddress', function () {
        it('known_plan', function () {
            var result = mock.patchAddress({
                    name: "jupiter_as1.io",
                    namespace: "app1_ns"
                },
                "[{\"op\":\"replace\",\"path\":\"/spec/plan\",\"value\":\"standard-small-queue\"}]",
                "application/json-patch+json"
            );

            assert.strictEqual(result.spec.plan.metadata.name, "standard-small-queue");
        });
    });

    describe('addressCommand', function () {
        it('with_resource_name', function () {
            var cmd = mock.addressCommand({
                metadata: {
                    name: "jupiter_as1.bar1",
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "fooaddr"
                }

            });

            assert.match(cmd, /name: jupiter_as1.bar1/);
        });
        it('with_address', function () {
            var cmd = mock.addressCommand({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "bar2"
                }

            }, "jupiter_as1");

            assert.match(cmd, /name: jupiter_as1.bar2/);
        });

    });
    describe('createAddressSpace', function () {
        it('valid_selfsigned', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "venus_as1",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            certificate: {
                                provider: "selfsigned"
                            },
                            expose: {
                                type: "route",
                                routeTlsTermination: "passthrough",
                                routeServicePort: "amqps",
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.addressSpaces(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` = 'venus_as1'"});
                assert.strictEqual(result.total, 1);

                var addressSpace = result.addressSpaces[0];
                assert.strictEqual(addressSpace.spec.endpoints.length, 1);
                assert.strictEqual(addressSpace.status.endpointStatus.length, 1);
                var endpointStatus = addressSpace.status.endpointStatus[0];
                assert.strictEqual(endpointStatus.name, "messaging");
                assert.strictEqual(endpointStatus.externalPorts.length, 1);
                assert.strictEqual(endpointStatus.servicePorts.length, 3);
                assert.strictEqual(addressSpace.status.caCertificate && addressSpace.status.caCertificate.length > 0, true);
            });
        });
        it('valid_certBundle', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "venus_as2",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            certificate: {
                                provider: "certBundle",
                                tlsKey: Buffer.from("PEMKEY").toString("base64"),
                                tlsCert: Buffer.from("PEMCERT").toString("base64"),
                            },
                            expose: {
                                type: "route",
                                routeTlsTermination: "passthrough",
                                routeServicePort: "amqps",
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.addressSpaces(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` = 'venus_as2'"});
                assert.strictEqual(result.total, 1);

                var addressSpace = result.addressSpaces[0];
                assert.strictEqual(addressSpace.spec.endpoints.length, 1);
                assert.strictEqual(addressSpace.status.endpointStatus.length, 1);
                var endpointStatus = addressSpace.status.endpointStatus[0];
                assert.strictEqual(endpointStatus.name, "messaging");
                assert.strictEqual(endpointStatus.externalPorts.length, 1);
                assert.strictEqual(endpointStatus.servicePorts.length, 3);
                assert.strictEqual(endpointStatus.certificate, addressSpace.spec.endpoints[0].certificate.tlsCert);
                assert.strictEqual(addressSpace.status.caCertificate === undefined, true);
            });
        });
        it('valid_openshift', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "venus_as3",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            certificate: {
                                provider: "openshift",
                            },
                            expose: {
                                type: "route",
                                routeTlsTermination: "passthrough",
                                routeServicePort: "amqps",
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.addressSpaces(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` = 'venus_as3'"});
                assert.strictEqual(result.total, 1);

                var addressSpace = result.addressSpaces[0];
                assert.strictEqual(addressSpace.spec.endpoints.length, 1);
                assert.strictEqual(addressSpace.status.endpointStatus.length, 1);
                var endpointStatus = addressSpace.status.endpointStatus[0];
                assert.strictEqual(endpointStatus.name, "messaging");
                assert.strictEqual(endpointStatus.externalPorts.length, 1);
                assert.strictEqual(endpointStatus.servicePorts.length, 3);
                assert.strictEqual(endpointStatus.certificate && endpointStatus.certificate.length > 0, true);
                assert.strictEqual(addressSpace.status.caCertificate === undefined, true);
            });
        });

    });
    describe('messaging_endpoints', function () {
        it('cluster_only', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "mercury_as1",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging"
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.messagingEndpoints(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` LIKE 'mercury_as1.%'"});
                assert.strictEqual(result.total, 1);

                var endpoint = result.messagingEndpoints[0];
                assert.strictEqual(endpoint.spec.protocols.length, 3);
                assert.strictEqual(endpoint.status.type, "Cluster");
                assert.strictEqual(endpoint.status.phase, "Active");
                assert.strictEqual(endpoint.status.host, "messaging-mercury_as1.app1_ns.svc");
                assert.strictEqual(endpoint.status.ports.length, 3);
            });
        });
        it('amqps_route_and_cluster', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "mercury_as2",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            expose: {
                                type: "route",
                                routeTlsTermination: "passthrough",
                                routeServicePort: "amqps",
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.messagingEndpoints(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` LIKE 'mercury_as2.%'"});
                assert.strictEqual(result.total, 2);

                var clusterEndpoint = result.messagingEndpoints[0];
                assert.strictEqual(clusterEndpoint.status.type, "Cluster");

                var routeEndpoint = result.messagingEndpoints[1];
                assert.strictEqual(routeEndpoint.spec.protocols.length, 1);
                assert.strictEqual(routeEndpoint.spec.protocols[0], "AMQPS");
                assert.strictEqual(routeEndpoint.spec.route.routeTlsTermination, "passthrough");

                assert.strictEqual(routeEndpoint.status.type, "Route");
                assert.strictEqual(routeEndpoint.status.phase, "Active");
                assert.strictEqual(routeEndpoint.status.host, "messaging-mercury_as2.app1_ns.apps-crc.testing");
                assert.strictEqual(routeEndpoint.status.ports.length, 1);
            });
        });
        it('amqp-wss_route_and_cluster', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "mercury_as3",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging-wss",
                            service: "messaging",
                            expose: {
                                type: "route",
                                routeTlsTermination: "reencrypt",
                                routeServicePort: "https",
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.messagingEndpoints(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` LIKE 'mercury_as3.%'"});
                assert.strictEqual(result.total, 2);

                var clusterEndpoint = result.messagingEndpoints[0];
                assert.strictEqual(clusterEndpoint.status.type, "Cluster");

                var routeEndpoint = result.messagingEndpoints[1];
                assert.strictEqual(routeEndpoint.spec.protocols.length, 1);
                assert.strictEqual(routeEndpoint.spec.protocols[0], "AMQP_WSS");
                assert.strictEqual(routeEndpoint.spec.route.routeTlsTermination, "reencrypt");

                assert.strictEqual(routeEndpoint.status.type, "Route");
                assert.strictEqual(routeEndpoint.status.phase, "Active");
                assert.strictEqual(routeEndpoint.status.host, "messaging-wss-mercury_as3.app1_ns.apps-crc.testing");
                assert.strictEqual(routeEndpoint.status.ports.length, 1);
                assert.strictEqual(routeEndpoint.status.ports[0].protocol, "AMQP_WSS");
            });
        });
        it('two_routes_and_cluster', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "mercury_as4",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            expose: {
                                type: "route",
                                routeTlsTermination: "passthrough",
                                routeServicePort: "amqps",
                            }
                        },
                        {
                            name: "messaging-wss",
                            service: "messaging",
                            expose: {
                                type: "route",
                                routeTlsTermination: "reencrypt",
                                routeServicePort: "https",
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.messagingEndpoints(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` LIKE 'mercury_as4.%'"});
                assert.strictEqual(result.total, 3);
            });
        });
        it('amqps_route_with_host_and_cluster', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "mercury_as5",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            expose: {
                                type: "route",
                                routeTlsTermination: "passthrough",
                                routeServicePort: "amqps",
                                routeHost: "myamqp.example.com"
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.messagingEndpoints(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` LIKE 'mercury_as5.%' AND `$.status.type` = 'Route'"});
                assert.strictEqual(result.total, 1);

                var routeEndpoint = result.messagingEndpoints[0];
                assert.strictEqual(routeEndpoint.status.host, "myamqp.example.com");
            });
        });
        it('loadbalancer_and_cluster', function () {
            var m = mock.createAddressSpace({
                metadata: {
                    name: "mercury_as6",
                    namespace: "app1_ns"
                },
                spec: {
                    plan: "standard-small",
                    type: "standard",
                    endpoints: [
                        {
                            name: "messaging",
                            service: "messaging",
                            expose: {
                                type: "loadbalancer",
                                loadBalancerPorts: ["amqps"]
                            }
                        }
                    ]
                }
            });

            return mock.whenActive(m).then(() => {
                var result = mock.resolvers.Query.messagingEndpoints(null,
                    {filter: "`$.metadata.namespace` = 'app1_ns' AND `$.metadata.name` LIKE 'mercury_as6.%' AND `$.status.type` = 'LoadBalancer'"});
                assert.strictEqual(result.total, 1);

                var loadbalancerEndpoint = result.messagingEndpoints[0];
                assert.strictEqual(loadbalancerEndpoint.status.type, "LoadBalancer");
                assert.strictEqual(loadbalancerEndpoint.status.phase, "Active");
                assert.strictEqual(loadbalancerEndpoint.status.ports.length, 1);
                assert.strictEqual(loadbalancerEndpoint.status.ports[0].protocol, "AMQPS");
            });
        });

    });

});