/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const uuidv1 = require("uuid/v1");
const uuidv5 = require("uuid/v5");
const { ApolloServer, ApolloError, gql } = require("apollo-server");
const { formatApolloErrors } = require("apollo-server-errors");
const typeDefs = require("./schema");
const { applyPatch, compare } = require("fast-json-patch");
const parser = require("./filter_parser.js");
const clone = require("clone");
const orderer = require("./orderer.js");
const _ = require("lodash");

class MultiError extends ApolloError {
  constructor(message, errors) {
    super(message);
    this.errors = errors;
  }
}

function runOperationForAll(input, operation) {
  var errors = [];
  input.forEach(i => {
    try {
      operation(i);
    } catch (e) {
      errors.push(e);
    }
  });
  if (errors) {
    throw new MultiError("multi-operation failed", errors);
  }
}

function calcLowerUpper(offset, first, len) {
  var lower = 0;
  if (offset !== undefined && offset > 0) {
    lower = Math.min(offset, len);
  }
  var upper = len;
  if (first !== undefined && first > 0) {
    upper = Math.min(lower + first, len);
  }
  return { lower, upper };
}

var stateChangeTimeout = process.env.STATE_CHANGE_TIMEOUT;
if (!stateChangeTimeout) {
  stateChangeTimeout = 10000;
}

function setStateChangeTimeout(timeout) {
  stateChangeTimeout = timeout;
}

var active = {};

function whenActive(metadata) {
  return active[metadata] ? active[metadata] : Promise.resolve();
}

const availableAddressTypes = [
  {
    metadata: {
      name: "queue",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "queue",
      shortDescription: "A store-and-forward queue",
      longDescription:
        "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases where you want to decouple the producer and consumer. A queue in the brokered address space supports selectors, message groups, transactions, and other JMS features. Message order can be lost with released messages.",
      displayOrder: 0
    }
  },
  {
    metadata: {
      name: "topic",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "topic",
      shortDescription:
        "A publish-and-subscribe address with store-and-forward semantics",
      longDescription:
        "The topic address type supports the publish-subscribe messaging pattern in which there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      displayOrder: 1
    }
  },
  {
    metadata: {
      name: "anycast",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "anycast",
      shortDescription:
        "A scalable 'direct' address for sending messages to one consumer",
      longDescription:
        "The anycast address type is a scalable direct address for sending messages to one consumer. Messages sent to an anycast address are not stored, but are instead forwarded directly to the consumer. This method makes this address type ideal for request-reply (RPC) uses or even work distribution. This is the cheapest address type as it does not require any persistence.",
      displayOrder: 0
    }
  },
  {
    metadata: {
      name: "multicast",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "multicast",
      shortDescription:
        "A scalable 'direct' address for sending messages to multiple consumers",
      longDescription:
        "The multicast address type is a scalable direct address for sending messages to multiple consumers. Messages sent to a multicast address are forwarded to all consumers receiving messages on that address. Because message acknowledgments from consumers are not propagated to producers, only pre-settled messages can be sent to multicast addresses.",
      displayOrder: 1
    }
  },
  {
    metadata: {
      name: "subscription",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "subscription",
      shortDescription: "A subscription on a specified topic",
      longDescription:
        "The subscription address type allows a subscription to be created for a topic that holds messages published to the topic even if the subscriber is not attached. The subscription is accessed by the consumer using <topic-address>::<subscription-address>. For example, for a subscription `mysub` on a topic `mytopic` the consumer consumes from the address `mytopic::mysub`.",
      displayOrder: 3
    }
  },
  {
    metadata: {
      name: "topic",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "topic",
      shortDescription: "A publish-subscribe topic",
      longDescription:
        "The topic address type supports the publish-subscribe messaging pattern where there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      displayOrder: 4
    }
  },
  {
    metadata: {
      name: "deadLetter",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: "deadLetter",
      shortDescription: "A global read-only queue",
      longDescription:
        "The deadLetter address type can be used as a dead letter or expiry address for queues and subscriptions.",
      displayOrder: 4
    }
  }
];

const availableNamespaces = [
  {
    metadata: {
      name: "app1_ns"
    },
    status: {
      phase: "Active"
    }
  },
  {
    metadata: {
      name: "app2_ns"
    },
    status: {
      phase: "Active"
    }
  },
  {
    metadata: {
      name: "app3_ns"
    },
    status: {
      phase: "Active"
    }
  }
];

const availableAddressPlans = [];

/*
function createMessagingAddressPlan(name, displayName, shortDescription, longDescription, displayOrder)
{
  return {
    metadata: {
      name: name,
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      displayName: displayName,
      displayOrder: displayOrder,
      longDescription: longDescription,
      shortDescription: shortDescription
    }
  };
}

  createAddressPlan("standard-small-queue",
      "queue",
      "Small Queue",
      "Creates a small queue sharing underlying broker with other queues.",
      "Creates a small queue sharing underlying broker with other queues.",
      {
        "broker": 0.01,
        "router": 0.001
      },
      0),
  createAddressPlan("standard-medium-queue",
      "queue",
      "Medium Queue",
      "Creates a medium sized queue sharing underlying broker with other queues.",
      "Creates a medium sized queue sharing underlying broker with other queues.",
      {
        "broker": 0.01,
        "router": 0.001
      },
      1),
  createAddressPlan("standard-small-anycast",
      "anycast",
      "Small Anycast",
      "Creates a small anycast address.",
      "Creates a small anycast address where messages go via a router that does not take ownership of the messages.",
      {
        "router": 0.001
      },
      2),
  createAddressPlan("standard-medium-anycast",
      "anycast",
      "Medium Anycast",
      "Creates a medium anycast address.",
      "Creates a medium anycast address where messages go via a router that does not take ownership of the messages.",
      {
        "router": 0.001
      },
      3),
  createAddressPlan("standard-large-anycast",
      "anycast",
      "Large Anycast",
      "Creates a large anycast address.",
      "Creates a large anycast address where messages go via a router that does not take ownership of the messages.",
      {
        "router": 0.001
      },
      4),
  createAddressPlan("standard-small-multicast",
      "multicast",
      "Small Multicast",
      "Creates a small multicast address.",
      "Creates a small multicast address where messages go via a router that does not take ownership of the messages.",
      {
        "router": 0.001
      },
      5),
  createAddressPlan("standard-medium-multicast",
      "multicast",
      "Medium Multicast",
      "Creates a medium multicast address.",
      "Creates a medium multicast address where messages go via a router that does not take ownership of the messages.",
      {
        "router": 0.001
      },
      6),
  createAddressPlan("standard-small-topic",
      "topic",
      "Small Topic",
      "Creates a small topic sharing underlying broker with other topics.",
      "Creates a small topic sharing underlying broker with other topics.",
      {
        "broker": 0
      },
      7),
  createAddressPlan("standard-medium-topic",
      "topic",
      "Medium Topic",
      "Creates a medium topic sharing underlying broker with other topics.",
      "Creates a medium topic sharing underlying broker with other topics.",
      {
        "broker": 0
      },
      8),
  createAddressPlan("standard-small-subscription",
      "subscription",
      "Small Subscription",
      "Creates a small durable subscription on a topic.",
      "Creates a small durable subscription on a topic, which is then accessed as a distinct address.",
      {
        "broker": 0
      },
      9),
  createAddressPlan("brokered-queue",
      "queue",
      "Brokered Queue",
      "Creates a queue on a broker.",
      "Creates a queue on a broker.",
      {
        "broker": 0
      },
      0),
  createAddressPlan("brokered-topic",
      "topic",
      "Brokered Topic",
      "Creates a topic on a broker.",
      "Creates a topic on a broker.",
      {
        "broker": 0
      },
      1)
];


const availableAddressSpacePlans = [
  {
    metadata: {
      name: "standard-small",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressPlans: availableAddressPlans.filter(p => !p.metadata.name.startsWith("brokered-")),
      displayName: "Small",
      shortDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
      longDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis. This plan allows up to 1 router and 1 broker in total, and is suitable for small applications using small address plans and few addresses.",
      displayOrder: 0,
      resourceLimits: {
        aggregate: 2,
        broker: 1,
        router: 1
      }
    }
  },
  {
    metadata: {
      name: "standard-medium",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressPlans: availableAddressPlans.filter(p => !p.metadata.name.startsWith("brokered-")),
      displayName: "Medium",
      shortDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
      longDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis. This plan allows up to 3 routers and 3 broker in total, and is suitable for applications using small address plans and few addresses.",
      displayOrder: 1,
      resourceLimits: {
        aggregate: 2.0,
        broker: 3.0,
        router: 3.0
      }
    }
  },
  {
    metadata: {
      name: "brokered-single-broker",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressPlans: availableAddressPlans.filter(p => p.metadata.name.startsWith("brokered-")),
      displayName: "Single Broker",
      shortDescription: "Single Broker instance",
      longDescription: "Single Broker plan where you can create an infinite number of queues until the system falls over.",
      displayOrder: 0,
      resourceLimits: {
        broker: 1.9,
      }
    }
  }
];
*/

function getRandomCreationDate(floor) {
  var created = new Date().getTime() - Math.random() * 1000 * 60 * 60 * 24;
  if (floor && created < floor.getTime()) {
    created = floor.getTime();
  }
  var date = new Date();
  date.setTime(created);
  return date;
}

function scheduleSetMessagingTenantStatus(messagingTenant, phase, message) {
  return new Promise(resolve => {
    setTimeout(() => {
      if (!messagingTenant.status) {
        messagingTenant.status = {};
      }

      messagingTenant.status.message = message;
      messagingTenant.status.phase = phase;

      if (phase !== "Active") {
        scheduleSetMessagingTenantStatus(messagingTenant, "Active", "");
      } else {
        resolve();
      }
    }, stateChangeTimeout);
  });
}

function createMessagingTenant(tenant) {
  var namespace = availableNamespaces.find(
    n => n.metadata.name === tenant.metadata.namespace
  );
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${tenant.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  /*
  var spacePlan = availableAddressSpacePlans.find(o => o.metadata.name === as.spec.plan && as.spec.type === o.spec.messagingTenantType);
  if (spacePlan === undefined) {
    var knownPlansNames = availableAddressSpacePlans.filter(p => as.spec.type === p.spec.messagingTenantType).map(p => p.metadata.name);
    throw `Unrecognised address space plan '${as.spec.plan}', known plans for type '${as.spec.type}' are : ${knownPlansNames}`;
  }
  */

  if (
    messagingTenants.find(
      existing =>
        tenant.metadata.name === existing.metadata.name &&
        tenant.metadata.namespace === existing.metadata.namespace
    ) !== undefined
  ) {
    throw `Messaging tenant with name  '${tenant.metadata.name} already exists in namespace ${tenant.metadata.namespace}`;
  }

  var phase = "Active";
  var message = "";
  if (tenant.status && tenant.status.phase) {
    phase = tenant.status.phase;
  }
  if (phase !== "Active") {
    message = "Not yet bound to infrastructure";
  }

  var messagingTenant = {
    metadata: {
      name: tenant.metadata.name,
      namespace: namespace.metadata.name,
      uid: uuidv1(),
      creationTimestamp: tenant.metadata.creationTimestamp
        ? tenant.metadata.creationTimestamp
        : getRandomCreationDate()
    },
    spec: {},
    status: {
      phase: "",
      message: ""
    }
  };

  messagingTenants.push(messagingTenant);
  active[messagingTenant.metadata] = scheduleSetMessagingTenantStatus(
    messagingTenant,
    phase,
    message
  );
  return messagingTenant.metadata;
}

function createDefaultEndpoints() {
  return [
    {
      certificate: {
        provider: "selfsigned"
      },
      expose: {
        routeServicePort: "amqps",
        routeTlsTermination: "passthrough",
        type: "route"
      },
      name: "messaging",
      service: "messaging"
    },
    {
      certificate: {
        provider: "selfsigned"
      },
      expose: {
        routeServicePort: "https",
        routeTlsTermination: "reencrypt",
        type: "route"
      },
      name: "messaging-wss",
      service: "messaging"
    }
  ];
}

function createEndpointStatuses(messagingTenant) {
  var endpointStatuses = [];

  messagingTenant.spec.endpoints.forEach(e => {
    var endpointStatus = {
      name: e.name
    };

    if (!e.certificate) {
      e.certificate = { provider: "selfsigned" };
    } else if (!e.certificate.provider) {
      e.certificate.provider = "selfsigned";
    }

    if (e.certificate.provider === "certBundle") {
      endpointStatus.certificate = e.certificate.tlsCert;
    } else {
      endpointStatus.certificate = `
----BEGIN CERTIFICATE-----
MIIDrzCCApegAwIBAgIUSBUjPhOi9/v+7QezYF/scX+tH3IwDQYJKoZIhvcNAQEL
BQAwQjELMAkGA1UEBhMCWFgxFTATBgNVBAcMDERlZmF1bHQgQ2l0eTEcMBoGA1UE
CgwTRGVmYXVsdCBDb21wYW55IEx0ZDAgFw0yMDA1MjIwODQxNTJaGA8yMDUwMDcw
NDA4NDE1MlowRjETMBEGA1UECgwKaW8uZW5tYXNzZTEvMC0GA1UEAwwmbWVzc2Fn
aW5nLXF1ZXVlc3BhY2UuZW5tYXNzZS1pbmZyYS5zdmMwggEiMA0GCSqGSIb3DQEB
AQUAA4IBDwAwggEKAoIBAQDK9JHlyeZt+WRfO7jxL5vMykIjNoC8TbrENXWA+y6E
E4XBiJ8tWpfwWM8uFYiJeaoFeqCzjSuFnTgU/bkgKdlp5PDsZFBUOlmYH0U7tbwK
GJedimNHquZSgGT5m4wL+5VxnHWEShn/y+4YuhnwYQBjm5zKWy9mufNuwpWYBLzJ
Ii7E1SxjJpeD+VkakSbf8fE6QZTw4KWkfq4iuU3IevaViZZYBF7MhKQu2+JXrZnK
ydfrCIL4HjP7Vy9ZGRsG7OJ2++VD9X17qHyChnRuOv0nHCS8LfceNEw08UuS+Kg3
n2jYKByxPNpOZxkl+TLazSwZ3yA0cJtrs/PuwrMzhcWnAgMBAAGjgZYwgZMwgZAG
A1UdEQSBiDCBhYImbWVzc2FnaW5nLXF1ZXVlc3BhY2UuZW5tYXNzZS1pbmZyYS5z
dmOCM21lc3NhZ2luZy1xdWV1ZXNwYWNlLWVubWFzc2UtaW5mcmEuYXBwcy1jcmMu
dGVzdGluZ4ImbWVzc2FnaW5nLXF1ZXVlc3BhY2UuZW5tYXNzZS1pbmZyYS5zdmMw
DQYJKoZIhvcNAQELBQADggEBAJv5Qfhi0pLJJK4Y9go1sXF0x1YcnU5zd9Aur7aP
0BpdZhHpiLoXuP3Um5WIMXZw1tF4H7yisb4yZTPG+vHOI3W1JzLp1sxDQC48rbAf
vWRM3ZqORjeaBNppdsSsEecUq/6VPTmmnnxgEj4NMUaL/sKDgjoZT1alkwfk+s6v
QzMugsw71TgwnpWtpOxHIhQuSxkIyNBHswNTq+js8I1RVqBJiwMKhsOMCssbyCaq
iuQC6VD1peb6Eby7JeDQjPrBmJnInuBNfLlDq0jgxZB/6kLfhug8dPc7v5TSPV9E
37Bon8FHRQit5qZNw/AGSzcPXMUeBG3pUOCuAZ5/yU7X0fc=
-----END CERTIFICATE-----`;
      if (e.certificate.provider === "selfsigned") {
        messagingTenant.status.caCertificate = `-----BEGIN CERTIFICATE-----
MIIDZzCCAk+gAwIBAgIUdLEKQr2fin9g5ZI+Fr5GOyvWNtowDQYJKoZIhvcNAQEL
BQAwQjELMAkGA1UEBhMCWFgxFTATBgNVBAcMDERlZmF1bHQgQ2l0eTEcMBoGA1UE
CgwTRGVmYXVsdCBDb21wYW55IEx0ZDAgFw0yMDA1MjIwODQxNTJaGA8yMDUwMDcw
NDA4NDE1MlowQjELMAkGA1UEBhMCWFgxFTATBgNVBAcMDERlZmF1bHQgQ2l0eTEc
MBoGA1UECgwTRGVmYXVsdCBDb21wYW55IEx0ZDCCASIwDQYJKoZIhvcNAQEBBQAD
ggEPADCCAQoCggEBAOdye+zIiD7PuWjzehyhIXtPyOq3isnPRIg8Y+CMXdlsWZFK
Tx6KTqRvycOQoZLqy16eiYTA3i7QU1TljX/jzylXZ0ZfRRWG2iVfcl88H9HwP6bS
e61rR3h8UvBFizvsCVWVa95wSv1pWO9kORInERQBsNaFOiA3WDA97X0ujN1AxzAX
VxNzU/SmR3NyQvKqL0z6YBCL7kK+gMW5UmJwvWw5EKQAo0jZKecBtOMQOo0dMCbj
fORWIBCaENu+05ufvGC4Og4OCnoJRaMIru9BeRkHhBgcIdzkDlH2CJxjaPAFZ7dW
zj5eo+FAxb5QFy18F5ZyMtRNQb+dlglpMtf7zB0CAwEAAaNTMFEwHQYDVR0OBBYE
FGU2LfUON9+IN8lE2J6J6/9X3usuMB8GA1UdIwQYMBaAFGU2LfUON9+IN8lE2J6J
6/9X3usuMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBADPtJccg
tvfDE02RGVMuQMQ+PKCqZJCjHIWp1rVR5jHS4smuHKAhCYLHXUTRWGfzzcXnbCOB
oJ8N8wzCSLTqscuiEUTQTspxJPPXMxR6ETiktszDEI1KYuk233CWxIuc6VuNkbVI
X7vvR7CYfopRzwKT8k8BqncQl3MTK/80/So+afYA+vCrobTNhYDiiPaDUFqchSby
rGcfstMRWL5xxzDG+kgP2ArW72ZrlDjemscN4mUx0IDyEDuMyaX29uKF/MEE0YNx
Mai51vfoGPbivv+DrqQ08OLx9BqyxptjGijKMa7UwAy/g70RXDICoyFFX9avW5Yv
s5YkUqynapz1Meo=
-----END CERTIFICATE-----`;
      }
    }

    if (e.expose) {
      if (e.expose.type === "route") {
        endpointStatus.externalHost = e.expose.routeHost
          ? e.expose.routeHost
          : `${e.name}-${messagingTenant.metadata.name}.${messagingTenant.metadata.namespace}.apps-crc.testing`;
        endpointStatus.externalPorts = [
          { name: e.expose.routeServicePort, port: 443 }
        ];
      } else if (e.expose.type === "loadbalancer") {
        endpointStatus.externalPorts = [
          { name: "amqps", port: 5671 },
          { name: "amqps", port: 5672 },
          { name: "amqp-wss", port: 443 }
        ];
      }
    }

    if (e.service) {
      endpointStatus.serviceHost = `${e.service}-${messagingTenant.metadata.name}.${messagingTenant.metadata.namespace}.svc`;
      endpointStatus.servicePorts = [
        { name: "amqps", port: 5671 },
        { name: "amqp", port: 5672 },
        { name: "amqp-wss", port: 443 }
      ];
    }

    endpointStatuses.push(endpointStatus);
  });

  return endpointStatuses;
}

function makeMessagingEndpoints() {
  function makeMessagingEndpoint(as, name, spec, status) {
    return {
      metadata: {
        name: name,
        namespace: as.metadata.namespace,
        uid: uuidv5(name, as.metadata.uid),
        creationTimestamp: as.metadata.creationTimestamp
      },
      spec: spec,
      status: status
    };
  }

  function mapPorts(src, targetProtocols, targetPorts) {
    src.forEach(sp => {
      var mappedProtocol = sp.name.replace("-", "_").toUpperCase();
      if (mappedProtocol === "HTTPS") {
        mappedProtocol = "AMQP_WSS";
      }
      targetProtocols.push(mappedProtocol);
      targetPorts.push({
        name: sp.name,
        protocol: mappedProtocol,
        port: sp.port
      });
    });
  }

  var messagingEndpoints = [];
  messagingTenants.forEach(as => {
    var serviceAdded = false;
    as.spec.endpoints.forEach(ep => {
      var endpointStatus =
        as.status && as.status.endpointStatus
          ? as.status.endpointStatus.find(eps => eps.name === ep.name)
          : null;

      if (ep.service && !serviceAdded) {
        var spec = {
          protocols: [],
          cluster: {}
        };
        var status = {
          phase: "Active",
          message: "",
          ports: [],
          internalPorts: [],
          type: "Cluster"
        };

        var serviceName = `${as.metadata.name}.${ep.service}.cluster`;

        if (endpointStatus) {
          status.host = endpointStatus.serviceHost;
          mapPorts(endpointStatus.servicePorts, spec.protocols, status.ports);
        }

        messagingEndpoints.push(
          makeMessagingEndpoint(as, serviceName, spec, status)
        );
        serviceAdded = true;
      }

      if (ep.expose) {
        var spec = {
          protocols: []
        };
        var status = {
          phase: "Active",
          message: "",
          ports: [],
          internalPorts: [],
          type: ep.expose.type === "route" ? "Route" : "LoadBalancer"
        };

        var name = `${as.metadata.name}.${ep.name}`;

        if (endpointStatus) {
          if (ep.expose.type === "route") {
            spec.route = {
              routeTlsTermination: ep.expose.routeTlsTermination
            };

            status.host = endpointStatus.externalHost;
            mapPorts(
              endpointStatus.externalPorts,
              spec.protocols,
              status.ports
            );
          } else {
            spec.loadbalancer = {};
            ep.expose.loadBalancerPorts.forEach(lbp => {
              spec.protocols.push(lbp);

              var ep = endpointStatus.externalPorts.find(ep => ep.name === lbp);
              if (ep) {
                var mappedProtocol = ep.name.replace("-", "_").toUpperCase();
                status.ports.push({
                  name: ep.name,
                  protocol: mappedProtocol,
                  port: ep.port
                });
              }
            });
          }
        }

        messagingEndpoints.push(makeMessagingEndpoint(as, name, spec, status));
      }
    });
  });

  return messagingEndpoints;
}

function patchMessagingTenant(metadata, jsonPatch, patchType) {
  var index = messagingTenants.findIndex(
    existing =>
      metadata.name === existing.metadata.name &&
      metadata.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Messaging tenant with name  '${metadata.name}' in namespace ${metadata.namespace} does not exist`;
  }

  var knownPatchTypes = [
    "application/json-patch+json",
    "application/merge-patch+json",
    "application/strategic-merge-patch+json"
  ];
  if (knownPatchTypes.find(p => p === patchType) === undefined) {
    throw `Unsupported patch type '$patchType'`;
  } else if (patchType !== "application/json-patch+json") {
    throw `Unsupported patch type '$patchType', this mock currently supports only 'application/json-patch+json'`;
  }

  var patch = JSON.parse(jsonPatch);
  var current = JSON.parse(JSON.stringify(messagingTenants[index]));
  var patched = applyPatch(JSON.parse(JSON.stringify(current)), patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    /*
    if (!_.isEqual(replacement.spec.plan, current.spec.plan)) {
      var replacementPlan = typeof(replacement.spec.plan) === "string" ? replacement.spec.plan : replacement.metadata.name;
      var spacePlan = availableAddressSpacePlans.find(o => o.metadata.name === replacementPlan);
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressSpacePlans.map(p => p.metadata.name);
        throw `Unrecognised address space plan '${replacementPlan}', known ones are : ${knownPlansNames}`;
      }
      replacement.spec.plan = spacePlan;
    }
    */

    messagingTenants[index].spec = replacement.spec;
    return messagingTenants[index];
  } else {
    throw `Failed to patch messaging tenant with name  '${metadata.name}' in namespace ${metadata.namespace}`;
  }
}

function deleteMessagingTenant(objectmeta) {
  var index = messagingTenants.findIndex(
    existing =>
      objectmeta.name === existing.metadata.name &&
      objectmeta.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Messaging tenant with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }
  var tenant = messagingTenants[index];
  delete messagingtenant_connection[tenant.metadata.uid];

  messagingTenants.splice(index, 1);
}

var messagingTenants = [];

createMessagingTenant({
  metadata: {
    name: "default",
    namespace: availableNamespaces[0].metadata.name
  },
  spec: {}
});
createMessagingTenant({
  metadata: {
    name: "default",
    namespace: availableNamespaces[1].metadata.name
  },
  spec: {}
});
createMessagingTenant({
  metadata: {
    name: "default",
    namespace: availableNamespaces[2].metadata.name
  },
  spec: {}
});
var connections = [];

var users = ["guest", "bob", "alice"];
function createConnection(messagingTenant, hostname) {
  var port = Math.floor(Math.random() * 25536) + 40000;
  var hostport = hostname + ":" + port;
  var encrypted = port % 2 === 0;
  var properties = [
    {
      key: "platform",
      value:
        "JVM: 1.8.0_191, 25.191-b12, Oracle Corporation, OS: Mac OS X, 10.13.6, x86_64"
    },
    {
      key: "product",
      value: "QpidJMS"
    },
    {
      key: "version",
      value: "0.38.0-SNAPSHOT"
    }
  ];
  return {
    metadata: {
      name: hostport,
      uid: uuidv1() + "",
      namespace: messagingTenant.metadata.namespace,
      creationTimestamp: getRandomCreationDate(
        messagingTenant.metadata.creationTimestamp
      )
    },
    spec: {
      messagingTenant: messagingTenant.metadata.name,
      hostname: hostport,
      containerId: uuidv1() + "",
      protocol: encrypted ? "amqps" : "amqp",
      encrypted: encrypted,
      properties: properties,
      principal: users[Math.floor(Math.random() * users.length)],
      metrics: []
    }
  };
}

connections = connections.concat(
  [
    "juno",
    "galileo",
    "ulysses",
    "cassini",
    "pioneer10",
    "pioneer11",
    "voyager1",
    "voyager2",
    "horizons",
    "clipper",
    "icy",
    "dragonfly",
    "kosmos",
    "mariner4",
    "mariner5",
    "zond2",
    "mariner6",
    "nozomi",
    "rosetta",
    "yinghuo1",
    "pathfinder"
  ].map(n => createConnection(messagingTenants[0], n))
);

connections = connections.concat(
  ["dragonfly"].map(n => createConnection(messagingTenants[1], n))
);

connections = connections.concat(
  [
    "kosmos",
    "mariner4",
    "mariner5",
    "zond2",
    "mariner6",
    "nozomi",
    "rosetta",
    "yinghuo1",
    "pathfinder"
  ].map(n => createConnection(messagingTenants[2], n))
);

var messagingtenant_connection = {};
messagingTenants.forEach(tenant => {
  messagingtenant_connection[tenant.metadata.uid] = connections.filter(
    c => c.spec.namespace === tenant.metadata.namespace
  );
});

var addresses = [];

function scheduleSetAddressStatus(address, phase, message) {
  setTimeout(() => {
    address.status = {
      message: message,
      phase: phase
    };
    if (phase !== "Active") {
      scheduleSetAddressStatus(address, "Active", "");
    }
  }, stateChangeTimeout);
}

function defaultResourceNameFromAddress(address, messagingTenantName) {
  var clean = address.toLowerCase();
  var maxLength = 253 - messagingTenantName.length - 1;
  if (
    /^[a-z0-9][-a-z0-9_.]*[a-z0-9]$/.test(clean) &&
    messagingTenantName.length < maxLength
  ) {
    return messagingTenantName + "." + clean;
  } else {
    clean = clean.replace(/[^-a-z0-9_.]/g, "");
    if (
      clean.charAt(0) === "-" ||
      clean.charAt(0) === "." ||
      clean.charAt(0) === "_"
    )
      clean = clean.substring(1);
    if (
      clean.charAt(clean.length - 1) === "-" ||
      clean.charAt(clean.length - 1) === "." ||
      clean.charAt(clean.length - 1) === "_"
    )
      clean = clean.substring(0, clean.length - 1);
    var uid = "" + uuidv1();
    maxLength = 253 - messagingTenantName.length - uid.length - 2;
    if (clean.length > maxLength) clean = clean.substring(0, maxLength);
    return messagingTenantName + "." + clean + "." + uid;
  }
}

function createAddress(addr, messagingTenantName) {
  var namespace = availableNamespaces.find(
    n => n.metadata.name === addr.metadata.namespace
  );
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${addr.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  var messagingTenantsInNamespace = messagingTenants.filter(
    as => as.metadata.namespace === addr.metadata.namespace
  );
  if (addr.metadata.name) {
    messagingTenantName = addr.metadata.namespace;
  } else if (addr.spec.address) {
    if (!messagingTenantName) {
      throw `messagingTenant is not provided, cannot default resource name from address '${addr.spec.address}'`;
    }
    addr.metadata.name = defaultResourceNameFromAddress(
      addr.spec.address,
      messagingTenantName
    );
  } else {
    throw `address is undefined, cannot default resource name`;
  }

  var messagingTenant = messagingTenantsInNamespace.find(
    as => as.metadata.namespace === messagingTenantName
  );
  if (messagingTenant === undefined) {
    var addressspacenames = messagingTenantsInNamespace.map(
      p => p.metadata.Namespace
    );
    throw `Unrecognised address space '${messagingTenantName}', known ones are : ${addressspacenames}`;
  }

  /*
  var knownTypes = ["queue", "topic", "subscription", "multicast", "anycast"];
  if (knownTypes.find(t => t === addr.spec.type) === undefined) {
    throw `Unrecognised address type '${addr.spec.type}', known ones are : '${knownTypes}'`;
  }

  var plan = availableAddressPlans.find(
    p =>
      p.metadata.name === addr.spec.plan &&
      addr.spec.type === p.spec.addressType
  );
  if (plan === undefined) {
    var knownPlansNames = availableAddressPlans
      .filter(p => addr.spec.Type === p.spec.addressType)
      .map(p => p.metadata.name);
    throw `Unrecognised address plan '${addr.spec.plan}', known plans for type '${addr.spec.type}' are : ${knownPlansNames}`;
  }
  */

  if (addr.spec.subscription !== undefined) {
    var topics = addresses.filter(
      a =>
        a.metadata.name.startsWith(messagingTenantName) &&
        a.spec.topic !== undefined
    );
    if (!addr.spec.subscription.topic) {
      throw `spec.subscription.topic is mandatory for the subscription type`;
    } else if (
      topics.find(
        t =>
          t.metadata.name == addr.spec.subscription.topic ||
          t.spec.address === addr.spec.subscription.topic
      ) === undefined
    ) {
      var topicNames = topics.map(t =>
        t.spec.address !== undefined ? t.metadata.name : t.spec.address
      );
      throw `Unrecognised topic address '${addr.spec.subscription.topic}', known ones are : '${topicNames}'`;
    }
  }

  if (
    addresses.find(
      existing =>
        addr.metadata.name === existing.metadata.name &&
        addr.metadata.namespace === existing.metadata.namespace
    ) !== undefined
  ) {
    throw `Address with name  '${addr.metadata.name} already exists in address space ${messagingTenantName}`;
  }

  var phase = "Active";
  var message = "";
  if (addr.status && addr.status.phase) {
    phase = addr.status.phase;
  }
  if (phase !== "Active") {
    message = "Address " + addr.metadata.name + " not found on qdrouterd";
  }

  /*
  var planStatus = null;
  if (messagingTenant.spec.type === "standard") {
    planStatus = {
      name: plan.metadata.name,
      partitions: 1
    };
  }
  */

  var address = {
    metadata: {
      name: addr.metadata.name,
      namespace: addr.metadata.namespace,
      uid: uuidv1(),
      creationTimestamp: addr.metadata.creationTimestamp
        ? addr.metadata.creationTimestamp
        : getRandomCreationDate()
    },
    spec: addr.spec,
    //    address: addr.spec.address,
    // messagingTenant: addr.spec.messagingTenant,
    // plan: plan,
    //   type: addr.spec.type,
    //   topic: addr.spec.topic
    status: {
      phase: "",
      message: ""
    }
  };
  scheduleSetAddressStatus(address, phase, message);
  addresses.push(address);
  return address.metadata;
}

function patchAddress(objectmeta, jsonPatch, patchType) {
  var index = addresses.findIndex(
    existing =>
      objectmeta.name === existing.metadata.name &&
      objectmeta.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Address with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }

  var knownPatchTypes = [
    "application/json-patch+json",
    "application/merge-patch+json",
    "application/strategic-merge-patch+json"
  ];
  if (knownPatchTypes.find(p => p === patchType) === undefined) {
    throw `Unsupported patch type '$patchType'`;
  } else if (patchType !== "application/json-patch+json") {
    throw `Unsupported patch type '$patchType', this mock currently supports only 'application/json-patch+json'`;
  }

  var patch = JSON.parse(jsonPatch);
  var current = JSON.parse(JSON.stringify(addresses[index]));
  var patched = applyPatch(JSON.parse(JSON.stringify(current)), patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    if (!_.isEqual(replacement.spec.plan, current.spec.plan)) {
      var replacementPlan =
        typeof replacement.spec.plan === "string"
          ? replacement.spec.plan
          : replacement.plan.metadata.name;
      var spacePlan = availableAddressPlans.find(
        o => o.metadata.name === replacementPlan
      );
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressPlans.map(p => p.metadata.name);
        throw `Unrecognised address plan '${replacementPlan}', known ones are : ${knownPlansNames}`;
      }
      replacement.spec.plan = spacePlan;
    }

    addresses[index].spec = replacement.spec;
    return addresses[index];
  } else {
    throw `Failed to patch address with name  '${objectmeta.name}' in namespace ${objectmeta.namespace}`;
  }
}

function titleCasePath(str) {
  var splitStr = str.toLowerCase().split("/");
  for (var i = 0; i < splitStr.length; i++) {
    splitStr[i] =
      splitStr[i].charAt(0).toUpperCase() + splitStr[i].substring(1);
  }
  return splitStr.join("/");
}

function deleteAddress(metadata) {
  var index = addresses.findIndex(
    existing =>
      metadata.name === existing.metadata.name &&
      metadata.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Address with name  '${metadata.name}' in namespace ${metadata.namespace} does not exist`;
  }
  addresses.splice(index, 1);
}

function purgeAddress(objectmeta) {
  var index = addresses.findIndex(
    existing =>
      objectmeta.name === existing.metadata.name &&
      objectmeta.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Address with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }
}

function closeConnection(objectmeta) {
  var index = connections.findIndex(
    existing =>
      objectmeta.name === existing.metadata.name &&
      objectmeta.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    var knownCons = connections
      .filter(c => c.metadata.namespace === objectmeta.namespace)
      .map(c => c.metadata.name);
    throw `Connection with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist. Known connection names are: ${knownCons}`;
  }
  var targetCon = connections[index];

  var messagingTenantName = connections[index].spec.messagingTenant;
  var as = messagingTenants.find(
    as => as.metadata.name === messagingTenantName
  );

  var as_cons = addressspace_connection[as.metadata.uid];
  var as_cons_index = as_cons.findIndex(c => c === targetCon);
  as_cons.splice(as_cons_index, 1);

  connections.splice(index, 1);
}

[
  "ganymede",
  "callisto",
  "io",
  "europa",
  "amalthea",
  "himalia",
  "thebe",
  "elara",
  "pasiphae",
  "metis",
  "carme",
  "sinope"
].map(n =>
  createAddress({
    metadata: {
      name: messagingTenants[0].metadata.name + "." + n,
      namespace: messagingTenants[0].metadata.namespace
    },
    spec: {
      address: n,
      messagingTenant: messagingTenants[0].metadata.name,
      plan: "standard-small-queue",
      type: "queue"
    },
    status: {
      phase: n.startsWith("c")
        ? "Configuring"
        : n.startsWith("p")
        ? "Pending"
        : "Active"
    }
  })
);

function createTopicWithSub(messagingTenant, topicName) {
  createAddress({
    metadata: {
      name: messagingTenant.metadata.name + "." + topicName,
      namespace: messagingTenant.metadata.namespace
    },
    spec: {
      address: topicName,
      messagingTenant: messagingTenant.metadata.name,
      plan: "standard-small-topic",
      type: "topic"
    },
    status: {
      phase: topicName.startsWith("c")
        ? "Configuring"
        : topicName.startsWith("p")
        ? "Pending"
        : "Active"
    }
  });
  var subname = topicName + "-sub";
  createAddress({
    metadata: {
      name: messagingTenants[0].metadata.name + "." + subname,
      namespace: messagingTenant.metadata.namespace
    },
    spec: {
      address: subname,
      messagingTenant: messagingTenant.metadata.name,
      plan: "standard-small-subscription",
      type: "subscription",
      topic: topicName
    },
    status: {
      phase: topicName.startsWith("c")
        ? "Configuring"
        : topicName.startsWith("p")
        ? "Pending"
        : "Active"
    }
  });
}

// Topic with a subscription
["themisto"].map(n => createTopicWithSub(messagingTenants[0], n));

["titan", "rhea", "iapetus", "dione", "tethys", "enceladus", "mimas"].map(n =>
  createAddress({
    metadata: {
      name: n,
      namespace: messagingTenants[1].metadata.namespace
    },
    spec: {
      address: n,
      messagingTenant: messagingTenants[1].metadata.name,
      plan: "standard-small-queue",
      type: "queue"
    }
  })
);

["phobos", "deimous"].map(n =>
  createAddress({
    metadata: {
      name: messagingTenants[2].metadata.name + "." + n,
      namespace: messagingTenants[2].metadata.namespace
    },
    spec: {
      address: n,
      messagingTenant: messagingTenants[2].metadata.name,
      plan: "brokered-queue",
      type: "queue"
    }
  })
);

function* makeAddrIter(namespace, addressspace) {
  var filter = addresses.filter(
    a =>
      a.metadata.namespace === namespace &&
      a.metadata.name.startsWith(addressspace + ".")
  );
  var i = 0;
  while (filter.length) {
    var addr = filter[i++ % filter.length];
    yield addr;
  }
}

var addressItrs = {};
messagingTenants.forEach(as => {
  addressItrs[as.metadata.uid] = makeAddrIter(
    as.metadata.namespace,
    as.metadata.name
  );
});

var links = [];
connections.forEach(c => {
  var messagingTenantName = c.spec.messagingTenant;
  var messagingTenant = messagingTenants.find(
    as => as.metadata.name === messagingTenantName
  );
  var uid = messagingTenant.metadata.uid;
  var addr = addressItrs[uid].next().value;

  for (var i = 0; i < addr.metadata.name.length; i++) {
    links.push({
      metadata: {
        name: uuidv1()
      },
      spec: {
        connection: c,
        address: addr.metadata.name,
        role: i % 2 === 0 ? "sender" : "receiver"
      }
    });
  }
});

function buildFilterer(filter) {
  return filter ? parser.parse(filter) : { evaluate: () => true };
}

function init(input) {
  if (input.metadata) {
    input.metadata.creationTimestamp = new Date();
  }
  return input;
}

function makeMockAddressMetrics() {
  return [
    {
      name: "enmasse_messages_stored",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "messages"
    },
    {
      name: "enmasse_senders",
      type: "gauge",
      value: Math.floor(Math.random() * 3),
      units: "links"
    },
    {
      name: "enmasse_receivers",
      type: "gauge",
      value: Math.floor(Math.random() * 3),
      units: "links"
    },
    {
      name: "enmasse_messages_in",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "msg/s"
    },
    {
      name: "enmasse_messages_out",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "msg/s"
    }
  ];
}

function makeMockConnectionMetrics() {
  return [
    {
      name: "enmasse_messages_in",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "msg/s"
    },
    {
      name: "enmasse_messages_out",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "msg/s"
    },
    {
      name: "enmasse_senders",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "total"
    },
    {
      name: "enmasse_receivers",
      type: "gauge",
      value: Math.floor(Math.random() * 10),
      units: "total"
    }
  ];
}

function makeMockLinkMetrics(is_addr_query, link) {
  if (is_addr_query) {
    return [
      {
        name:
          link.spec.role === "sender"
            ? "enmasse_messages_in"
            : "enmasse_messages_out",
        type: "gauge",
        value: Math.floor(Math.random() * 10),
        units: "msg/s"
      },
      {
        name: "enmasse_messages_backlog",
        type: "gauge",
        value: Math.floor(Math.random() * 15),
        units: "msg"
      }
    ];
  } else {
    var messagingTenantName = link.spec.connection.spec.messagingTenant;
    var as = messagingTenants.find(
      as => as.metadata.name === messagingTenantName
    );
    if (as.spec.type === "brokered") {
      return [
        {
          name: "enmasse_deliveries",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        }
      ];
    } else {
      return [
        {
          name: "enmasse_deliveries",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        },
        {
          name: "enmasse_accepted",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        },
        {
          name: "enmasse_rejected",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        },
        {
          name: "enmasse_released",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        },
        {
          name: "enmasse_modified",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        },
        {
          name: "enmasse_presettled",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        },
        {
          name: "enmasse_undelivered",
          type: "counter",
          value: Math.floor(Math.random() * 10),
          units: "deliveries"
        }
      ];
    }
  }
}

function makeAddressSpaceMetrics(as) {
  var cons =
    as.metadata.uid in addressspace_connection
      ? addressspace_connection[as.metadata.uid]
      : [];
  var addrs = addresses.filter(
    a =>
      as.metadata.namespace === a.metadata.namespace &&
      a.metadata.name.startsWith(as.metadata.name + ".")
  );

  return [
    {
      name: "enmasse_connections",
      type: "gauge",
      value: cons.length,
      units: "connections"
    },
    {
      name: "enmasse_addresses",
      type: "gauge",
      value: addrs.length,
      units: "addresses"
    }
  ];
}

function addressCommand(addr, messagingTenantName) {
  if (addr.metadata.name) {
    // pass
  } else if (addr.spec.address) {
    if (!messagingTenantName) {
      throw `messagingTenant is not provided, cannot default resource name from address '${addr.spec.address}'`;
    }
    addr.metadata.name = defaultResourceNameFromAddress(
      addr.spec.address,
      messagingTenantName
    );
  } else {
    throw `address is undefined, cannot default resource name`;
  }

  return `apiVersion: enmasse.io/v1beta1
oc apply -f - << EOF
kind: Address
metadata:
  name: ${addr.metadata.name}
spec:
  address: ${addr.spec.address}
  type: ${addr.spec.type}
  plan: ${addr.spec.plan}
EOF
`;
}

function messagingTenantCommand(as) {
  return `apiVersion: enmasse.io/v1beta1
oc apply -f - << EOF
kind: AddressSpace
metadata:
  name: ${as.metadata.name}
spec:
  type: ${as.spec.type}
  plan: ${as.spec.plan}
EOF
`;
}

// A map of functions which return data for the schema.
const resolvers = {
  Mutation: {
    createAddressSpace: (parent, args) => {
      return createAddressSpace(init(args.input));
    },
    patchAddressSpace: (parent, args) => {
      patchAddressSpace(args.input, args.jsonPatch, args.patchType);
      return true;
    },
    deleteAddressSpace: (parent, args) => {
      deleteAddressSpace(args.input);
      return true;
    },
    deleteAddressSpaces: (parent, args) => {
      runOperationForAll(args.input, t => deleteAddressSpace(t));
      return true;
    },
    createAddress: (parent, args) => {
      return createAddress(init(args.input), args.messagingTenant);
    },
    patchAddress: (parent, args) => {
      patchAddress(args.input, args.jsonPatch, args.patchType);
      return true;
    },
    deleteAddress: (parent, args) => {
      deleteAddress(args.input);
      return true;
    },
    deleteAddresses: (parent, args) => {
      runOperationForAll(args.input, t => deleteAddress(t));
      return true;
    },
    purgeAddress: (parent, args) => {
      purgeAddress(args.input);
      return true;
    },
    purgeAddresses: (parent, args) => {
      runOperationForAll(args.input, t => purgeAddress(t));
      return true;
    },
    closeConnections: (parent, args) => {
      runOperationForAll(args.input, t => closeConnection(t));
      return true;
    }
  },
  Query: {
    hello: () => "world",

    messagingCertificateChain: () => `-----BEGIN CERTIFICATE-----
MIICLDCCAdKgAwIBAgIBADAKBggqhkjOPQQDAjB9MQswCQYDVQQGEwJCRTEPMA0G
A1UEChMGR251VExTMSUwIwYDVQQLExxHbnVUTFMgY2VydGlmaWNhdGUgYXV0aG9y
aXR5MQ8wDQYDVQQIEwZMZXV2ZW4xJTAjBgNVBAMTHEdudVRMUyBjZXJ0aWZpY2F0
ZSBhdXRob3JpdHkwHhcNMTEwNTIzMjAzODIxWhcNMTIxMjIyMDc0MTUxWjB9MQsw
CQYDVQQGEwJCRTEPMA0GA1UEChMGR251VExTMSUwIwYDVQQLExxHbnVUTFMgY2Vy
dGlmaWNhdGUgYXV0aG9yaXR5MQ8wDQYDVQQIEwZMZXV2ZW4xJTAjBgNVBAMTHEdu
dVRMUyBjZXJ0aWZpY2F0ZSBhdXRob3JpdHkwWTATBgcqhkjOPQIBBggqhkjOPQMB
BwNCAARS2I0jiuNn14Y2sSALCX3IybqiIJUvxUpj+oNfzngvj/Niyv2394BWnW4X
uQ4RTEiywK87WRcWMGgJB5kX/t2no0MwQTAPBgNVHRMBAf8EBTADAQH/MA8GA1Ud
DwEB/wQFAwMHBgAwHQYDVR0OBBYEFPC0gf6YEr+1KLlkQAPLzB9mTigDMAoGCCqG
SM49BAMCA0gAMEUCIDGuwD1KPyG+hRf88MeyMQcqOFZD0TbVleF+UsAGQ4enAiEA
l4wOuDwKQa+upc8GftXE2C//4mKANBC6It01gUaTIpo=
-----END CERTIFICATE-----`,

    messagingTenantCommand: (parent, args, context, info) => {
      var as = args.input;
      return messagingTenantCommand(as);
    },

    addressCommand: (parent, args, context, info) => {
      var addr = args.input;
      var messagingTenantName = args.messagingTenant;
      return addressCommand(addr, messagingTenantName);
    },

    namespaces: () => availableNamespaces,

    authenticationServices: () => availableAuthenticationServices,
    messagingTenantSchema: () => availableAddressSpaceSchemas,
    messagingTenantSchema_v2: (parent, args, context, info) => {
      return availableAddressSpaceSchemas.filter(
        o =>
          args.messagingTenantType === undefined ||
          o.metadata.name === args.messagingTenantType
      );
    },

    addressTypes: () => [
      "queue",
      "topic",
      "subscription",
      "multicast",
      "anycast"
    ],
    addressTypes_v2: (parent, args, context, info) => {
      return availableAddressTypes
        .filter(
          o =>
            args.messagingTenantType === undefined ||
            o.spec.messagingTenantType === args.messagingTenantType
        )
        .sort(o => o.spec.displayOrder);
    },
    messagingTenantTypes: () => ["standard", "brokered"],
    messagingTenantTypes_v2: (parent, args, context, info) => {
      return availableAddressSpaceTypes.sort(o => o.spec.displayOrder);
    },
    messagingTenantPlans: (parent, args, context, info) => {
      return availableAddressSpacePlans
        .filter(
          o =>
            args.messagingTenantType === undefined ||
            o.spec.messagingTenantType === args.messagingTenantType
        )
        .sort(o => o.spec.displayOrder);
    },
    addressPlans: (parent, args, context, info) => {
      var plans = availableAddressPlans;
      if (args.messagingTenantPlan) {
        var spacePlan = availableAddressSpacePlans.find(
          o => o.metadata.name === args.messagingTenantPlan
        );
        if (spacePlan === undefined) {
          var knownPlansNames = availableAddressSpacePlans.map(
            p => p.metadata.name
          );
          throw `Unrecognised address space plan '${args.messagingTenantPlan}', known ones are : ${knownPlansNames}`;
        }
        plans = spacePlan.spec.addressPlans;
      }

      return plans
        .filter(
          p =>
            args.addressType === undefined ||
            p.spec.addressType === args.addressType
        )
        .sort(o => o.spec.displayOrder);
    },
    messagingTenants: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var copy = clone(messagingTenants);
      copy.forEach(as => {
        as.metrics = makeAddressSpaceMetrics(as);
      });
      var as = copy.filter(as => filterer.evaluate(as)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, as.length);
      var page = as.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: as.length,
        messagingTenants: page
      };
    },
    addresses: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var copy = clone(addresses);
      copy.forEach(a => {
        a.metrics = makeMockAddressMetrics();
      });

      var a = copy.filter(a => filterer.evaluate(a)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, a.length);
      var page = a.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: a.length,
        addresses: page
      };
    },
    connections: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);
      var copy = clone(connections);
      copy.forEach(c => {
        c.metrics = makeMockConnectionMetrics();
      });
      var cons = copy.filter(c => filterer.evaluate(c)).sort(orderBy);

      var paginationBounds = calcLowerUpper(
        args.offset,
        args.first,
        cons.length
      );
      var page = cons.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: cons.length,
        connections: page
      };
    },
    messagingEndpoints: (parent, args, context, info) => {
      var messagingEndpoints = makeMessagingEndpoints();
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);
      var endpoints = messagingEndpoints
        .filter(me => filterer.evaluate(me))
        .sort(orderBy);

      var paginationBounds = calcLowerUpper(
        args.offset,
        args.first,
        endpoints.length
      );
      var page = endpoints.slice(
        paginationBounds.lower,
        paginationBounds.upper
      );

      return {
        total: endpoints.length,
        messagingEndpoints: page
      };
    }
  },

  AddressSpace_consoleapi_enmasse_io_v1beta1: {
    connections: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var as = parent;
      var cons =
        as.metadata.uid in addressspace_connection
          ? addressspace_connection[as.metadata.uid]
          : [];
      var copy = clone(cons);
      copy.forEach(c => {
        c.metrics = makeMockConnectionMetrics();
      });

      copy = copy.filter(c => filterer.evaluate(c)).sort(orderBy);

      var paginationBounds = calcLowerUpper(
        args.offset,
        args.first,
        copy.length
      );
      var page = copy.slice(paginationBounds.lower, paginationBounds.upper);
      return { total: copy.length, connections: page };
    },
    addresses: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var as = parent;

      var copy = clone(
        addresses.filter(
          a =>
            as.metadata.namespace === a.metadata.namespace &&
            a.metadata.name.startsWith(as.metadata.name + ".")
        )
      );
      copy.forEach(a => {
        a.metrics = makeMockAddressMetrics();
      });

      var addrs = copy.filter(a => filterer.evaluate(a)).sort(orderBy);

      var paginationBounds = calcLowerUpper(
        args.offset,
        args.first,
        addrs.length
      );
      var page = addrs.slice(paginationBounds.lower, paginationBounds.upper);
      return { total: addrs.length, addresses: page };
    }
  },
  Address_consoleapi_enmasse_io_v1beta1: {
    links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var addr = parent;
      var copy = clone(
        links.filter(
          l =>
            l.spec.connection.metadata.namespace === addr.metadata.namespace &&
            addr.metadata.name.startsWith(
              l.spec.connection.spec.messagingTenant + "."
            )
        )
      );
      copy.forEach(l => {
        l.metrics = makeMockLinkMetrics(true, l);
      });
      var addrlinks = copy.filter(l => filterer.evaluate(l)).sort(orderBy);

      var paginationBounds = calcLowerUpper(
        args.offset,
        args.first,
        addrlinks.length
      );
      var page = addrlinks.slice(
        paginationBounds.lower,
        paginationBounds.upper
      );

      return {
        total: addrlinks.length,
        links: page
      };
    }
  },
  Connection_consoleapi_enmasse_io_v1beta1: {
    links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var con = parent;
      var copy = clone(
        links.filter(l => _.isEqual(l.spec.connection.metadata, con.metadata))
      );
      copy.forEach(l => {
        l.metrics = makeMockLinkMetrics(false, l);
      });

      var connlinks = copy.filter(l => filterer.evaluate(l)).sort(orderBy);
      var paginationBounds = calcLowerUpper(
        args.offset,
        args.first,
        connlinks.length
      );
      var page = connlinks.slice(
        paginationBounds.lower,
        paginationBounds.upper
      );

      return {
        total: connlinks.length,
        links: page
      };
    }
  },
  ConnectionSpec_consoleapi_enmasse_io_v1beta1: {
    messagingTenant: (parent, args, context, info) => {
      var as = messagingTenants.find(
        as => as.metadata.name === parent.AddressSpace
      );
      return as;
    }
  },

  Link_consoleapi_enmasse_io_v1beta1: {},
  ObjectMeta_v1: {
    creationTimestamp: (parent, args, context, info) => {
      var meta = parent;
      return meta.creationTimestamp;
    }
  }
};

const mocks = {
  Int: () => 6,
  Float: () => 22.1,
  String: () => undefined,
  User_v1: () => ({
    metadata: {
      name: "vtereshkova"
    },
    identities: ["vtereshkova"],
    fullName: "Valentina Tereshkova",
    groups: ["admin"]
  })
};

if (require.main === module) {
  console.log(gql);
  console.log(typeDefs);

  const server = new ApolloServer({
    typeDefs,
    resolvers,
    mocks,
    mockEntireSchema: false,
    introspection: true,
    playground: true,
    formatResponse: (response, context) => {
      // Hack - apollo-graphql doesn't support multi-errors from queries/mutations (oddly it does for validation errors).
      // Here we turn MultiErrors into separate error responses.
      var errors = response.errors;
      if (errors) {
        var replacement = [];
        errors.forEach(err => {
          if (err.originalError && err.originalError.errors) {
            err.originalError.errors.forEach(underlying => {
              replacement.push(
                new ApolloError(underlying, err.code, err.extensions)
              );
            });
          } else {
            replacement.push(err);
          }
        });
        response.errors = formatApolloErrors(replacement);
      }
      return response;
    }
  });

  server.listen().then(({ url }) => {
    console.log(`? Server ready at ${url}`);
  });
}

module.exports.createAddress = createAddress;
module.exports.createAddressSpace = createAddressSpace;
module.exports.patchAddress = patchAddress;
module.exports.patchAddressSpace = patchAddressSpace;
module.exports.addressCommand = addressCommand;
module.exports.whenActive = whenActive;
module.exports.setStateChangeTimeout = setStateChangeTimeout;
module.exports.resolvers = resolvers;
