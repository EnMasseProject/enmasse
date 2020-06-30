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

const availableAddressSpaceTypes = [
  {
    metadata: {
      name: "brokered",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "brokered",
      displayName: "brokered",
      shortDescription: "The brokered address space.",
      longDescription:
        'The brokered address space type is the "classic" message broker in the cloud which supports AMQP, CORE, OpenWire, and MQTT protocols. It supports JMS with transactions, message groups, selectors on queues and so on.',
      displayOrder: 0
    }
  },
  {
    metadata: {
      name: "standard",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
      displayName: "standard",
      shortDescription: "The standard address space.",
      longDescription:
        "The standard address space type is the default type in EnMasse, and is focused on scaling in the number of connections and the throughput of the system. It supports AMQP and MQTT protocols.",
      displayOrder: 1
    }
  }
];

const availableAddressTypes = [
  {
    metadata: {
      name: "brokered.queue",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "brokered",
      displayName: "queue",
      shortDescription: "A store-and-forward queue",
      longDescription:
        "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases where you want to decouple the producer and consumer. A queue in the brokered address space supports selectors, message groups, transactions, and other JMS features. Message order can be lost with released messages.",
      displayOrder: 0
    }
  },
  {
    metadata: {
      name: "brokered.topic",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "brokered",
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
      name: "standard.anycast",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
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
      name: "standard.multicast",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
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
      name: "standard.queue",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
      displayName: "queue",
      shortDescription: "A store-and-forward queue",
      longDescription:
        "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases when you want to decouple the producer and consumer. A queue can be sharded across multiple storage units. Message ordering might be lost for queues in the standard address space.",
      displayOrder: 2
    }
  },
  {
    metadata: {
      name: "standard.subscription",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
      displayName: "subscription",
      shortDescription: "A subscription on a specified topic",
      longDescription:
        "The subscription address type allows a subscription to be created for a topic that holds messages published to the topic even if the subscriber is not attached. The subscription is accessed by the consumer using <topic-address>::<subscription-address>. For example, for a subscription `mysub` on a topic `mytopic` the consumer consumes from the address `mytopic::mysub`.",
      displayOrder: 3
    }
  },
  {
    metadata: {
      name: "standard.topic",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
      displayName: "topic",
      shortDescription: "A publish-subscribe topic",
      longDescription:
        "The topic address type supports the publish-subscribe messaging pattern where there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      displayOrder: 4
    }
  }
];

const availableAuthenticationServices = [
  createAuthenticationService("none", "none-authservice"),
  createAuthenticationService("standard", "standard-authservice")
];

function createAuthenticationService(type, name) {
  return {
    spec: {
      Type: type
    },
    metadata: {
      name: name,
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    }
  };
}

function createAddressSpaceSchema(name, description) {
  return {
    metadata: {
      name: name
    },
    spec: {
      authenticationServices: ["none-authservice", "standard-authservice"],
      description: description,
      routeServicePorts: [
        {
          name: "amqps",
          displayName: "Messaging (AMQP)",
          routeTlsTerminations: ["passthrough"]
        },
        {
          name: "https",
          displayName: "Websocket messaging (AMQP-WS)",
          routeTlsTerminations: ["passthrough", "reencrypt"]
        }
      ],
      certificateProviderTypes: [
        {
          name: "openshift",
          displayName: "OpenShift",
          description: "OpenShift provides a TLS certificate"
        },
        {
          name: "selfsigned",
          displayName: "Self-Signed",
          description: "System generates self-signed TLS certificate"
        },
        {
          name: "certBundle",
          displayName: "Certificate Bundle",
          description: "Upload a TLS certificate bundle"
        }
      ],
      endpointExposeTypes: [
        {
          name: "route",
          displayName: "OpenShift Route",
          description: "OpenShift Route"
        },
        {
          name: "loadbalancer",
          displayName: "LoadBalancer service",
          description: "LoadBalancer service"
        }
      ]
    }
  };
}

const availableAddressSpaceSchemas = [
  createAddressSpaceSchema(
    "brokered",
    "A brokered address space consists of a broker combined with a console for managing addresses."
  ),
  createAddressSpaceSchema(
    "standard",
    "A standard address space consists of an AMQP router network in combination with attachable 'storage units'. The implementation of a storage unit is hidden from the client and the routers with a well defined API."
  )
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
  }
];

function createAddressPlan(
  name,
  addressType,
  displayName,
  shortDescription,
  longDescription,
  resources,
  displayOrder
) {
  return {
    metadata: {
      name: name,
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressType: addressType,
      displayName: displayName,
      displayOrder: displayOrder,
      longDescription: longDescription,
      resources: resources,
      shortDescription: shortDescription
    }
  };
}

const availableAddressPlans = [
  createAddressPlan(
    "standard-small-queue",
    "queue",
    "Small Queue",
    "Creates a small queue sharing underlying broker with other queues.",
    "Creates a small queue sharing underlying broker with other queues.",
    {
      broker: 0.01,
      router: 0.001
    },
    0
  ),
  createAddressPlan(
    "standard-medium-queue",
    "queue",
    "Medium Queue",
    "Creates a medium sized queue sharing underlying broker with other queues.",
    "Creates a medium sized queue sharing underlying broker with other queues.",
    {
      broker: 0.01,
      router: 0.001
    },
    1
  ),
  createAddressPlan(
    "standard-small-anycast",
    "anycast",
    "Small Anycast",
    "Creates a small anycast address.",
    "Creates a small anycast address where messages go via a router that does not take ownership of the messages.",
    {
      router: 0.001
    },
    2
  ),
  createAddressPlan(
    "standard-medium-anycast",
    "anycast",
    "Medium Anycast",
    "Creates a medium anycast address.",
    "Creates a medium anycast address where messages go via a router that does not take ownership of the messages.",
    {
      router: 0.001
    },
    3
  ),
  createAddressPlan(
    "standard-large-anycast",
    "anycast",
    "Large Anycast",
    "Creates a large anycast address.",
    "Creates a large anycast address where messages go via a router that does not take ownership of the messages.",
    {
      router: 0.001
    },
    4
  ),
  createAddressPlan(
    "standard-small-multicast",
    "multicast",
    "Small Multicast",
    "Creates a small multicast address.",
    "Creates a small multicast address where messages go via a router that does not take ownership of the messages.",
    {
      router: 0.001
    },
    5
  ),
  createAddressPlan(
    "standard-medium-multicast",
    "multicast",
    "Medium Multicast",
    "Creates a medium multicast address.",
    "Creates a medium multicast address where messages go via a router that does not take ownership of the messages.",
    {
      router: 0.001
    },
    6
  ),
  createAddressPlan(
    "standard-small-topic",
    "topic",
    "Small Topic",
    "Creates a small topic sharing underlying broker with other topics.",
    "Creates a small topic sharing underlying broker with other topics.",
    {
      broker: 0
    },
    7
  ),
  createAddressPlan(
    "standard-small-topic",
    "topic",
    "Small Topic",
    "Creates a small topic sharing underlying broker with other topics.",
    "Creates a small topic sharing underlying broker with other topics.",
    {
      broker: 0
    },
    8
  ),
  createAddressPlan(
    "standard-medium-topic",
    "topic",
    "Medium Topic",
    "Creates a medium topic sharing underlying broker with other topics.",
    "Creates a medium topic sharing underlying broker with other topics.",
    {
      broker: 0
    },
    9
  ),
  createAddressPlan(
    "standard-small-subscription",
    "subscription",
    "Small Subscription",
    "Creates a small durable subscription on a topic.",
    "Creates a small durable subscription on a topic, which is then accessed as a distinct address.",
    {
      broker: 0
    },
    10
  ),
  createAddressPlan(
    "brokered-queue",
    "queue",
    "Brokered Queue",
    "Creates a queue on a broker.",
    "Creates a queue on a broker.",
    {
      broker: 0
    },
    0
  ),
  createAddressPlan(
    "brokered-topic",
    "topic",
    "Brokered Topic",
    "Creates a topic on a broker.",
    "Creates a topic on a broker.",
    {
      broker: 0
    },
    1
  )
];

const availableAddressSpacePlans = [
  {
    metadata: {
      name: "standard-small",
      uid: uuidv1(),
      creationTimestamp: getRandomCreationDate()
    },
    spec: {
      addressSpaceType: "standard",
      addressPlans: availableAddressPlans.filter(
        p => !p.metadata.name.startsWith("brokered-")
      ),
      displayName: "Small",
      shortDescription:
        "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
      longDescription:
        "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis. This plan allows up to 1 router and 1 broker in total, and is suitable for small applications using small address plans and few addresses.",
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
      addressSpaceType: "standard",
      addressPlans: availableAddressPlans.filter(
        p => !p.metadata.name.startsWith("brokered-")
      ),
      displayName: "Medium",
      shortDescription:
        "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
      longDescription:
        "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis. This plan allows up to 3 routers and 3 broker in total, and is suitable for applications using small address plans and few addresses.",
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
      addressSpaceType: "brokered",
      addressPlans: availableAddressPlans.filter(p =>
        p.metadata.name.startsWith("brokered-")
      ),
      displayName: "Single Broker",
      shortDescription: "Single Broker instance",
      longDescription:
        "Single Broker plan where you can create an infinite number of queues until the system falls over.",
      displayOrder: 0,
      resourceLimits: {
        broker: 1.9
      }
    }
  }
];

function getRandomCreationDate(floor) {
  var created = new Date().getTime() - Math.random() * 1000 * 60 * 60 * 24;
  if (floor && created < floor.getTime()) {
    created = floor.getTime();
  }
  var date = new Date();
  date.setTime(created);
  return date;
}

function scheduleSetAddressSpaceStatus(addressSpace, phase, messages) {
  return new Promise(resolve => {
    setTimeout(() => {
      if (!addressSpace.status) {
        addressSpace.status = {};
      }

      addressSpace.status.idReady = phase === "Active";
      addressSpace.status.messages = messages;
      addressSpace.status.phase = phase;
      addressSpace.status.endpointStatus =
        phase === "Active" ? createEndpointStatuses(addressSpace) : [];

      if (phase !== "Active") {
        scheduleSetAddressSpaceStatus(addressSpace, "Active", []);
      } else {
        resolve();
      }
    }, stateChangeTimeout);
  });
}

function createAddressSpace(as) {
  var namespace = availableNamespaces.find(
    n => n.metadata.name === as.metadata.namespace
  );
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${as.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  if (as.spec.type !== "brokered" && as.spec.type !== "standard") {
    throw `Unrecognised address space type '${as.spec.type}', known ones are : brokered, standard`;
  }

  var spacePlan = availableAddressSpacePlans.find(
    o =>
      o.metadata.name === as.spec.plan &&
      as.spec.type === o.spec.addressSpaceType
  );
  if (spacePlan === undefined) {
    var knownPlansNames = availableAddressSpacePlans
      .filter(p => as.spec.type === p.spec.addressSpaceType)
      .map(p => p.metadata.name);
    throw `Unrecognised address space plan '${as.spec.plan}', known plans for type '${as.spec.type}' are : ${knownPlansNames}`;
  }

  if (
    addressSpaces.find(
      existing =>
        as.metadata.name === existing.metadata.name &&
        as.metadata.namespace === existing.metadata.namespace
    ) !== undefined
  ) {
    throw `Address space with name  '${as.metadata.name} already exists in namespace ${as.metadata.namespace}`;
  }

  if (as.spec.endpoints) {
    as.spec.endpoints.forEach(ep => {
      if (ep.service !== "messaging") {
        throw `Unrecognised endpoint service type '${ep.service}', known ones are : messaging`;
      }
      if (!ep.name) {
        throw `Endpoint name is mandatory`;
      }

      if (ep.expose) {
        if (ep.expose.type !== "route" && ep.expose.type !== "loadbalancer") {
          throw `Unrecognised endpoint expose type '${ep.expose.type}', known ones are : route or loadbalancer`;
        }
        if (ep.expose.type === "route") {
          if (
            ep.expose.routeTlsTermination !== "passthrough" &&
            ep.expose.routeTlsTermination !== "reencrypt"
          ) {
            throw `Unrecognised endpoint expose routeTlsTermination '${ep.expose.routeTlsTermination}', known ones are : passthrough or reencrypt`;
          }
          if (
            ep.expose.routeServicePort !== "amqps" &&
            ep.expose.routeServicePort !== "https"
          ) {
            throw `Unrecognised endpoint expose routeServicePort '${ep.expose.routeServicePort}', known ones are : https or amqps`;
          }
        } else {
          if (
            !ep.expose.loadBalancerPorts ||
            !Array.isArray(ep.expose.loadBalancerPorts)
          ) {
            throw `Endpoint loadBalancerPorts is mandatory and must be a list`;
          }
          ep.expose.loadBalancerPorts.forEach(lbp => {
            if (lbp !== "amqps" && lbp !== "amqp") {
              throw `Unrecognised endpoint expose loadBalancerPorts '${ep.expose.loadBalancerPorts}', known ones are : amqp or amqps`;
            }
          });
        }
      }

      if (ep.certificate) {
        if (ep.certificate.provider) {
          if (
            ep.certificate.provider !== "selfsigned" &&
            ep.certificate.provider !== "certBundle" &&
            ep.certificate.provider !== "openshift"
          ) {
            throw `Unrecognised endpoint cert provider '${ep.certificate.provider}', known ones are : selfsigned, certBundle, or openshift`;
          }
          if (ep.certificate.provider === "certBundle") {
            function validBase64(str) {
              return Buffer.from(str, "base64").toString("base64") === str;
            }
            if (!ep.certificate.tlsKey) {
              throw `Endpoint cert provider '${ep.certificate.provider}' requires 'cert.tlsKey' field.`;
            }
            if (!validBase64(ep.certificate.tlsKey)) {
              throw `Endpoint cert provider '${ep.certificate.provider}' requires 'cert.tlsKey' to be valid base64.`;
            }

            if (!ep.certificate.tlsCert) {
              throw `Endpoint cert provider '${ep.certificate.provider}' requires 'cert.tlsCert' field.`;
            }
            if (!validBase64(ep.certificate.tlsCert)) {
              throw `Endpoint cert provider '${ep.certificate.provider}' requires 'cert.tlsCert' to be valid base64.`;
            }
          }
        }
      }
    });
  }

  var phase = "Active";
  var messages = [];
  if (as.status && as.status.phase) {
    phase = as.status.phase;
  }
  if (phase !== "Active") {
    messages.push("The following deployments are not ready: [admin.daf7b31]");
  }

  var addressSpace = {
    metadata: {
      name: as.metadata.name,
      namespace: namespace.metadata.name,
      uid: uuidv1(),
      creationTimestamp: as.metadata.creationTimestamp
        ? as.metadata.creationTimestamp
        : getRandomCreationDate()
    },
    spec: {
      plan: spacePlan,
      type: as.spec.type,
      endpoints: as.spec.endpoints
        ? as.spec.endpoints
        : createDefaultEndpoints(),
      authenticationService: {
        name: as.spec.authenticationService
          ? as.spec.authenticationService.name
          : null
      }
    },
    status: {
      phase: "",
      messages: []
    }
  };

  addressSpaces.push(addressSpace);
  active[addressSpace.metadata] = scheduleSetAddressSpaceStatus(
    addressSpace,
    phase,
    messages
  );
  return addressSpace.metadata;
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

function createEndpointStatuses(addressSpace) {
  var endpointStatuses = [];

  addressSpace.spec.endpoints.forEach(e => {
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
        addressSpace.status.caCertificate = `-----BEGIN CERTIFICATE-----
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
          : `${e.name}-${addressSpace.metadata.name}.${addressSpace.metadata.namespace}.apps-crc.testing`;
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
      endpointStatus.serviceHost = `${e.service}-${addressSpace.metadata.name}.${addressSpace.metadata.namespace}.svc`;
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
  addressSpaces.forEach(as => {
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

function patchAddressSpace(metadata, jsonPatch, patchType) {
  var index = addressSpaces.findIndex(
    existing =>
      metadata.name === existing.metadata.name &&
      metadata.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Address space with name  '${metadata.name}' in namespace ${metadata.namespace} does not exist`;
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
  var current = JSON.parse(JSON.stringify(addressSpaces[index]));
  var patched = applyPatch(JSON.parse(JSON.stringify(current)), patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    if (!_.isEqual(replacement.spec.plan, current.spec.plan)) {
      var replacementPlan =
        typeof replacement.spec.plan === "string"
          ? replacement.spec.plan
          : replacement.metadata.name;
      var spacePlan = availableAddressSpacePlans.find(
        o => o.metadata.name === replacementPlan
      );
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressSpacePlans.map(
          p => p.metadata.name
        );
        throw `Unrecognised address space plan '${replacementPlan}', known ones are : ${knownPlansNames}`;
      }
      replacement.spec.plan = spacePlan;
    }

    addressSpaces[index].spec = replacement.spec;
    return addressSpaces[index];
  } else {
    throw `Failed to patch address space with name  '${metadata.name}' in namespace ${metadata.namespace}`;
  }
}

function deleteAddressSpace(objectmeta) {
  var index = addressSpaces.findIndex(
    existing =>
      objectmeta.name === existing.metadata.name &&
      objectmeta.namespace === existing.metadata.namespace
  );
  if (index < 0) {
    throw `Address space with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }
  var as = addressSpaces[index];
  delete addressspace_connection[as.metadata.uid];

  addressSpaces.splice(index, 1);
}

var addressSpaces = [];

createAddressSpace({
  metadata: {
    name: "jupiter_as1",
    namespace: availableNamespaces[0].metadata.name
  },
  spec: {
    plan: "standard-small",
    type: "standard",
    authenticationService: {
      name: "none-authservice"
    }
  }
});

createAddressSpace({
  metadata: {
    name: "saturn_as2",
    namespace: availableNamespaces[0].metadata.name
  },
  spec: {
    plan: "standard-medium",
    type: "standard",
    authenticationService: {
      name: "none-authservice"
    }
  },
  status: {
    phase: "Configuring"
  }
});

createAddressSpace({
  metadata: {
    name: "mars_as2",
    namespace: availableNamespaces[1].metadata.name
  },
  spec: {
    plan: "brokered-single-broker",
    type: "brokered",
    authenticationService: {
      name: "none-authservice"
    }
  }
});

var connections = [];

var users = ["guest", "bob", "alice"];
function createConnection(addressSpace, hostname) {
  var port = Math.floor(Math.random() * 25536) + 40000;
  var hostport = hostname + ":" + port;
  var encrypted = port % 2 === 0;
  var properties = [];
  if (addressSpace.spec.Type === "standard") {
    properties = [
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
  }
  return {
    metadata: {
      name: hostport,
      uid: uuidv1() + "",
      namespace: addressSpace.metadata.namespace,
      creationTimestamp: getRandomCreationDate(
        addressSpace.metadata.creationTimestamp
      )
    },
    spec: {
      addressSpace: addressSpace.metadata.name,
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
  ].map(n => createConnection(addressSpaces[0], n))
);

connections = connections.concat(
  ["dragonfly"].map(n => createConnection(addressSpaces[1], n))
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
  ].map(n => createConnection(addressSpaces[2], n))
);

var addressspace_connection = {};
addressSpaces.forEach(as => {
  addressspace_connection[as.metadata.uid] = connections.filter(
    c => c.spec.addressSpace === as.metadata.name
  );
});

var addresses = [];

function scheduleSetAddressStatus(address, phase, messages, planStatus) {
  setTimeout(() => {
    address.status = {
      isReady: phase === "Active",
      messages: messages,
      phase: phase,
      planStatus: planStatus
    };
    if (phase !== "Active") {
      scheduleSetAddressStatus(address, "Active", [], planStatus);
    }
  }, stateChangeTimeout);
}

function defaultResourceNameFromAddress(address, addressSpaceName) {
  var clean = address.toLowerCase();
  var maxLength = 253 - addressSpaceName.length - 1;
  if (
    /^[a-z0-9][-a-z0-9_.]*[a-z0-9]$/.test(clean) &&
    addressSpaceName.length < maxLength
  ) {
    return addressSpaceName + "." + clean;
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
    maxLength = 253 - addressSpaceName.length - uid.length - 2;
    if (clean.length > maxLength) clean = clean.substring(0, maxLength);
    return addressSpaceName + "." + clean + "." + uid;
  }
}

function createAddress(addr, addressSpaceName) {
  var namespace = availableNamespaces.find(
    n => n.metadata.name === addr.metadata.namespace
  );
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${addr.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  var addressSpacesInNamespace = addressSpaces.filter(
    as => as.metadata.namespace === addr.metadata.namespace
  );
  if (addr.metadata.name) {
    var parts = addr.metadata.name.split(".", 2);
    if (parts.length < 2) {
      throw `Address name '${addr.metadata.name}' is badly formed, expected for '<addressspace>.<name>'`;
    }
    addressSpaceName = parts[0];
  } else if (addr.spec.address) {
    if (!addressSpaceName) {
      throw `addressSpace is not provided, cannot default resource name from address '${addr.spec.address}'`;
    }
    addr.metadata.name = defaultResourceNameFromAddress(
      addr.spec.address,
      addressSpaceName
    );
  } else {
    throw `address is undefined, cannot default resource name`;
  }

  var addressSpace = addressSpacesInNamespace.find(
    as => as.metadata.name === addressSpaceName
  );
  if (addressSpace === undefined) {
    var addressspacenames = addressSpacesInNamespace.map(p => p.metadata.Name);
    throw `Unrecognised address space '${addressSpaceName}', known ones are : ${addressspacenames}`;
  }

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

  if (addr.spec.type === "subscription") {
    var topics = addresses.filter(
      a =>
        a.metadata.name.startsWith(addressSpaceName) && a.spec.type === "topic"
    );
    if (!addr.spec.topic) {
      throw `spec.topic is mandatory for the subscription type`;
    } else if (
      topics.find(t => t.spec.address === addr.spec.topic) === undefined
    ) {
      var topicNames = topics.map(t => t.spec.address);
      throw `Unrecognised topic address '${addr.spec.topic}', known ones are : '${topicNames}'`;
    }
  } else {
    if (addr.spec.topic) {
      throw `spec.topic is not allowed for the address type '${addr.spec.type}'.`;
    }
  }

  if (
    addresses.find(
      existing =>
        addr.metadata.name === existing.metadata.name &&
        addr.metadata.namespace === existing.metadata.namespace
    ) !== undefined
  ) {
    throw `Address with name  '${addr.metadata.name} already exists in address space ${addressSpaceName}`;
  }

  var phase = "Active";
  var messages = [];
  if (addr.status && addr.status.phase) {
    phase = addr.status.phase;
  }
  if (phase !== "Active") {
    messages.push("Address " + addr.metadata.name + " not found on qdrouterd");
  }

  var planStatus = null;
  if (addressSpace.spec.type === "standard") {
    planStatus = {
      name: plan.metadata.name,
      partitions: 1
    };
  }

  var address = {
    metadata: {
      name: addr.metadata.name,
      namespace: addr.metadata.namespace,
      uid: uuidv1(),
      creationTimestamp: addr.metadata.creationTimestamp
        ? addr.metadata.creationTimestamp
        : getRandomCreationDate()
    },
    spec: {
      address: addr.spec.address,
      addressSpace: addr.spec.addressSpace,
      plan: plan,
      type: addr.spec.type,
      topic: addr.spec.topic
    },
    status: {
      phase: "",
      messages: []
    }
  };
  scheduleSetAddressStatus(address, phase, messages, planStatus);
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

  var addressSpaceName = connections[index].spec.addressSpace;
  var as = addressSpaces.find(as => as.metadata.name === addressSpaceName);

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
      name: addressSpaces[0].metadata.name + "." + n,
      namespace: addressSpaces[0].metadata.namespace
    },
    spec: {
      address: n,
      addressSpace: addressSpaces[0].metadata.name,
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

function createTopicWithSub(addressSpace, topicName) {
  createAddress({
    metadata: {
      name: addressSpace.metadata.name + "." + topicName,
      namespace: addressSpace.metadata.namespace
    },
    spec: {
      address: topicName,
      addressSpace: addressSpace.metadata.name,
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
      name: addressSpaces[0].metadata.name + "." + subname,
      namespace: addressSpace.metadata.namespace
    },
    spec: {
      address: subname,
      addressSpace: addressSpace.metadata.name,
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
["themisto"].map(n => createTopicWithSub(addressSpaces[0], n));

["titan", "rhea", "iapetus", "dione", "tethys", "enceladus", "mimas"].map(n =>
  createAddress({
    metadata: {
      name: addressSpaces[1].metadata.name + "." + n,
      namespace: addressSpaces[1].metadata.namespace
    },
    spec: {
      address: n,
      addressSpace: addressSpaces[1].metadata.name,
      plan: "standard-small-queue",
      type: "queue"
    }
  })
);

["phobos", "deimous"].map(n =>
  createAddress({
    metadata: {
      name: addressSpaces[2].metadata.name + "." + n,
      namespace: addressSpaces[2].metadata.namespace
    },
    spec: {
      address: n,
      addressSpace: addressSpaces[2].metadata.name,
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
addressSpaces.forEach(as => {
  addressItrs[as.metadata.uid] = makeAddrIter(
    as.metadata.namespace,
    as.metadata.name
  );
});

var links = [];
connections.forEach(c => {
  var addressSpaceName = c.spec.addressSpace;
  var addressSpace = addressSpaces.find(
    as => as.metadata.name === addressSpaceName
  );
  var uid = addressSpace.metadata.uid;
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
    var addressSpaceName = link.spec.connection.spec.addressSpace;
    var as = addressSpaces.find(as => as.metadata.name === addressSpaceName);
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

function addressCommand(addr, addressSpaceName) {
  if (addr.metadata.name) {
    // pass
  } else if (addr.spec.address) {
    if (!addressSpaceName) {
      throw `addressSpace is not provided, cannot default resource name from address '${addr.spec.address}'`;
    }
    addr.metadata.name = defaultResourceNameFromAddress(
      addr.spec.address,
      addressSpaceName
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

function addressSpaceCommand(as) {
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

var iotProjects = [];
var iotdevices = [];

createIotProject({
  metadata: {
    name: "iotProjectFrance",
    namespace: availableNamespaces[0].metadata.name
  },
  enabled: true,
  spec: {
    downstreamStrategyType: "managed"
  }
});

createIotProject({
  metadata: {
    name: "iotProjectIndia",
    namespace: availableNamespaces[1].metadata.name
  },
  enabled: true,
  spec: {
    downstreamStrategyType: "external"
  }
});

function getMockIotEndpoints() {
  return [
    {
      name: "HttpAdapter",
      url: "https://http.amq-online-iot.rhmi.com:8443",
      host: "http.amq-online-iot.rhmi.com",
      port: 8443
    },
    {
      name: "MqttAdapter",
      url: "mqtts://mqtt.amq-online-iot.rhmi.com:8883",
      host: "mqtt.amq-online-iot.rhmi.com",
      port: 8883,
      tls: true
    },
    {
      name: "AmqpAdapter",
      url: "amqps://amqp.amq-online-iot.rhmi.com:8443",
      host: "amqp.amq-online-iot.rhmi.com",
      port: 5671
    },
    {
      name: "DeviceRegistrationManagement",
      url: "https://management.amq-online-iot.rhmi.com/devices",
      host: "management.amq-online-iot.rhmi.com",
      port: 443
    },
    {
      name: "DeviceCredentialManagement",
      url: "https://management.amq-online-iot.rhmi.com/credentials",
      host: "management.amq-online-iot.rhmi.com",
      port: 443
    }
  ];
}

function createIotProject(pj) {
  var namespace = availableNamespaces.find(
    n => n.metadata.name === pj.metadata.namespace
  );
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${pj.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  var strategyType = pj.spec.downstreamStrategyType;
  if (strategyType !== "managed" && strategyType !== "external") {
    throw `Unrecognised downstream strategy type '${pj.spec.downstreamStrategyType}', known ones are : managed, external`;
  }

  if (
    iotProjects.find(
      existing =>
        pj.metadata.name === existing.metadata.name &&
        pj.metadata.namespace === existing.metadata.namespace
    ) !== undefined
  ) {
    throw `Iot Project with name  '${pj.metadata.name} already exists in namespace ${pj.metadata.namespace}`;
  }

  var phase = "Active";
  var messages = [];
  if (pj.status && pj.status.phase) {
    phase = pj.status.phase;
  }
  if (phase !== "Active") {
    messages.push(
      "The following deployments are not ready: [iot-device-registry.daf7b31]"
    );
  }

  var iotproject = {
    metadata: {
      name: pj.metadata.name,
      namespace: namespace.metadata.name,
      uid: uuidv1(),
      creationTimestamp: pj.metadata.creationTimestamp
        ? pj.metadata.creationTimestamp
        : getRandomCreationDate()
    },
    enabled: pj.enabled,
    spec: {
      downstreamStrategyType: "managed",
      downstreamStrategy: getMockDownstreamStrategy("managed"),
      configuration: "{}"
    },
    endpoints: getMockIotEndpoints(),
    status: getMockIotProjectStatus()
  };

  //TODO : set a timer for iot projects to become ready ?
  //scheduleSetAddressSpaceStatus(iotproject, phase, messages);

  iotProjects.push(iotproject);

  iotdevices.push({
    project: iotproject.metadata.name,
    devices: []
  });
  return iotproject.metadata;
}

function deleteIotProject(iotProject) {
  let pjIndex = getIotProjectIndex(iotProject.name);
  let devIndex = getIotDevicesProjectIndex(iotProject.name);

  // delete iot devices for this project
  iotdevices[devIndex].splice(devIndex, 1);

  iotProjects.splice(pjIndex, 1);
}

function getMockDownstreamStrategy(strategyType) {
  if (strategyType === "managed") {
    return {
      addressSpace: {
        name: addressSpaces[0].metadata.name,
        plan: addressSpaces[0].spec.plan,
        type: addressSpaces[0].spec.type
      },
      addresses: {
        Telemetry: {
          name: "telemetry-c127edbe-0242ac130003",
          plan: "standard-small-queue",
          type: "queue"
        },
        Event: {
          name: "event-c127edbe-0242ac130003",
          plan: "standard-small-queue",
          type: "queue"
        },
        Command: [
          {
            name: "command-c127edbe-0242ac130003",
            plan: "standard-small-queue",
            type: "queue"
          },
          {
            name: "command-receiver",
            plan: "standard-small-queue",
            type: "queue"
          }
        ]
      }
    };
  }
  if (strategyType === "external") {
    return {
      connectionInformation: {
        host: "messaging.external.host.tld",
        port: 5674,
        credentials: {
          username: "admin",
          password: "pasword"
        }
      }
    };
  }
}

function patchIotProject(metadata, jsonPatch, patchType) {
  var index = getIotDevicesProjectIndex(metadata.name);

  verifyPatchType(patchType);

  var patch = JSON.parse(jsonPatch);
  var current = JSON.parse(JSON.stringify(iotProjects[index]));
  var patched = applyPatch(JSON.parse(JSON.stringify(current)), patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;

    if (
      !_.isEqual(
        replacement.spec.downstreamStrategyType,
        current.spec.downstreamStrategyType
      )
    ) {
      if (strategyType !== "managed" && strategyType !== "external") {
        throw `Unrecognised downstream strategy type '${pj.spec.downstreamStrategyType}', known ones are : managed, external`;
      }
      replacement.spec.downstreamStrategy = getDownstreamStrategy(strategyType);
    }

    iotProjects[index].spec = replacement.spec;
    return iotProjects[index];
  } else {
    throw `Failed to patch iot project with name '${metadata.name}' in namespace ${metadata.namespace}`;
  }
}

function getMockIotProjectStatus() {
  return {
    phase: "Active",

    tenantName: "tenantName.iot",
    downstreamEndpoint: {
      host: "host.domain.com",
      port: 5674,
      credentials: {
        username: "messaging@435bm730c495zx834s53467",
        password: "verysecret"
      },
      tls: true,
      certificate: "averylongString"
    }
  };
}

function getIotProjectIndex(projectName) {
  let index = iotProjects.findIndex(
    existing => projectName === existing.metadata.name
  );
  if (index < 0) {
    throw `Iot project with name '${projectName}' does not exist`;
  }
  return index;
}

function getIotDevicesProjectIndex(projectName) {
  let devIndex = iotdevices.findIndex(
    existing => projectName === existing.project
  );
  if (devIndex < 0) {
    throw `Iot project with name '${projectName}' does not exist in iotDevices`;
  }
  return devIndex;
}

function createIotDevice(iotProject, newDevice) {
  getIotProjectIndex(iotProject);
  let devIndex = getIotDevicesProjectIndex(iotProject);

  if (
    iotdevices[devIndex].devices.find(
      d => d.deviceId === newDevice.deviceId
    ) !== undefined
  ) {
    throw `Iot device with deviceId  '${newDevice.deviceId} already exists in iot project ${iotProject}`;
  }

  iotdevices[devIndex].devices.push(newDevice);
  return getIotDevice(iotProject, newDevice.deviceId);
}

function deleteIotDevice(iotProject, deviceId) {
  getIotProjectIndex(iotProject);
  let pjIndex = getIotDevicesProjectIndex(iotProject);

  let devIndex = iotdevices[pjIndex].devices.findIndex(
    d => d.deviceId === deviceId
  );
  if (devIndex < 0) {
    throw `Iot device with deviceId '${deviceId} does not exist in iot project ${iotProject}`;
  }
  iotdevices[pjIndex].devices.splice(devIndex, 1);
}

function getIotDeviceStatusSection() {
  return {
    created: "2020-01-01T00:00:00+01:00",
    updated: "2020-06-10T09:45:00+01:00",
    "last-user": "Keanu Reeves"
  };
}

createIotDevice("iotProjectFrance", {
  deviceId: "10",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: {
      imei: "abcdef",
      manufacturer: "company",
      specs: {
        sensor1: "temp",
        sensor2: "light",
        sensor3: {
          lat: "nmea",
          long: "nmea"
        }
      }
    },
    status: getIotDeviceStatusSection()
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "11",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: { ocean: "atlantic" },
    status: getIotDeviceStatusSection()
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "12",
  enabled: true,
  viaGateway: true,
  jsonData: JSON.stringify({
    via: ["device-1", "device-2"],
    ext: { summit: "Mt Blanc" },
    status: getIotDeviceStatusSection()
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "13",
  enabled: false,
  viaGateway: true,
  jsonData: JSON.stringify({
    via: ["device-1", "device-2"],
    ext: { site: "Notre dame" }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "14",
  enabled: false,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: { author: "Jules Verne" }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "15",
  enabled: false,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: { city: "Nice" }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "16",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: { city: "Nice" }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "17",
  enabled: false,
  viaGateway: true,
  jsonData: JSON.stringify({
    ext: {
      via: ["device-1", "device-2"]
    }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "18",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: {
      via: []
    }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "19",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: {
      via: []
    }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "50",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: {
      via: []
    }
  }),
  credentials: []
});

createIotDevice("iotProjectFrance", {
  deviceId: "51",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    ext: {
      via: []
    }
  }),
  credentials: []
});

createIotDevice("iotProjectIndia", {
  deviceId: "20",
  enabled: true,
  viaGateway: true,
  jsonData: JSON.stringify({
    via: ["20", "21"],
    default: {
      "content-type-1": "text/plain",
      "content-type-2": "text/plain",
      "content-type-3": "text/plain",
      long: 12.3544
    },
    ext: {
      custom: {
        level: 0,
        serial_id: "0000",
        location: {
          long: 1.234,
          lat: 5.678
        },
        features: ["foo", "bar", "baz"]
      }
    },
    status: getIotDeviceStatusSection()
  }),
  credentials: []
});

createIotDevice("iotProjectIndia", {
  deviceId: "21",
  enabled: true,
  viaGateway: false,
  jsonData: JSON.stringify({
    via: [],
    ext: { summit: "Kanchenjunga" },
    status: getIotDeviceStatusSection()
  }),
  credentials: []
});

function setCredentials(iotProjectName, deviceId, creds) {
  let device = getIotDevice(iotProjectName, deviceId);
  device.credentials = creds;
}

setCredentials(
  "iotProjectFrance",
  "10",
  JSON.stringify([{ "auth-id": "10-id", type: "psk" }])
);
setCredentials(
  "iotProjectFrance",
  "11",
  JSON.stringify([{ "auth-id": "11-id", type: "password" }])
);
setCredentials(
  "iotProjectFrance",
  "12",
  JSON.stringify([
    { "auth-id": "12-id", type: "password" },
    { "auth-id": "12-id", type: "psk" }
  ])
);

setCredentials(
  "iotProjectIndia",
  "20",
  JSON.stringify([
    {
      type: "hashed-password",
      "auth-id": "user-1",
      enabled: false,
      secrets: [
        {
          "not-after": "2020-10-01T10:00:00Z",
          "pwd-hash": "bjb232138d"
        },
        {
          "not-before": "2020-10-01T10:00:00Z",
          "pwd-hash": "adfhk327823"
        }
      ]
    },
    {
      type: "hashed-password",
      "auth-id": "alternate-user-1",
      enabled: true,
      secrets: [
        {
          "pwd-hash": "pwd-test",
          comment: "was just for testing"
        }
      ]
    },
    {
      type: "psk",
      "auth-id": "user-1",
      secrets: [
        {
          key: "123knsd8=",
          comment: "was just for testing"
        }
      ]
    },
    {
      type: "x509-cert",
      "auth-id": "other-id-1",
      enabled: false,
      secrets: [],
      ext: {
        "para-1": "value-1",
        "para-2": "value-2",
        "para-3": "value-3",
        "para-4": "value-4"
      }
    }
  ])
);
setCredentials(
  "iotProjectIndia",
  "21",
  JSON.stringify([{ "auth-id": "21-id", type: "password" }])
);

function getIotDevice(iotProject, deviceId) {
  devices = getIotDevices(iotProject);
  for (var d in devices) {
    if (devices[d].deviceId == deviceId) {
      return devices[d];
    }
  }
  throw `device not found`;
}

function getIotDevices(iotProject) {
  for (var p in iotdevices) {
    if (iotdevices[p].project == iotProject) {
      return iotdevices[p].devices;
    }
  }
  throw `iot project not found`;
}

function getIotCredentials(iotProject, deviceId) {
  device = getIotDevice(iotProject, deviceId);
  return device.credentials;
}

function getAddressSpacesAndOrIotProjects(projectType) {
  var result;

  switch (projectType) {
    case "addressSpace":
      result = clone(addressSpaces);
      result.forEach(as => {
        as.metrics = makeAddressSpaceMetrics(as);
      });
      break;
    case "iotProject":
      result = clone(iotProjects);
      break;
  }
  return result;
}

// A map of functions which return data for the schema.
const resolvers = {
  IotProjectDownStreamStrategy: {
    __resolveType(DownStreamStrategy, context, info) {
      if (DownStreamStrategy.addressSpace) {
        return "ManagedDownstreamStrategy_iot_enmasse_io_v1alpha1";
      }

      if (DownStreamStrategy.connectionInformation) {
        return "ExternalDownstreamStrategy_iot_enmasse_io_v1alpha1";
      }

      return null;
    }
  },
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
      return createAddress(init(args.input), args.addressSpace);
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
    },

    createIotDevice: (parent, args) => {
      return createIotDevice(args.iotproject, args.device);
    },
    deleteIotDevice: (parent, args) => {
      deleteIotDevice(args.iotproject, args.deviceId);
      return true;
    },
    updateIotDevice: (parent, args) => {
      deleteIotDevice(args.iotproject, args.device.deviceId);
      return createIotDevice(args.iotproject, args.device);
    },
    setCredentialsForDevice: (parent, args) => {
      setCredentials(args.iotproject, args.deviceId, args.jsonData);
      return true;
    },
    deleteCredentialsForDevice: (parent, args) => {
      setCredentials(args.iotproject, args.deviceId, []);
      return true;
    },
    createIotProject: (parent, args) => {
      let input = init(args.input);
      input.spec = { downstreamStrategyType: "managed" };

      return createIotProject(input);
    },
    patchIotProject: (parent, args) => {
      patchIotProject(args.input, args.jsonPatch, args.patchType);
      return true;
    },
    deleteIotProject: (parent, args) => {
      deleteIotProject(args.input);
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

    addressSpaceCommand: (parent, args, context, info) => {
      var as = args.input;
      return addressSpaceCommand(as);
    },

    addressCommand: (parent, args, context, info) => {
      var addr = args.input;
      var addressSpaceName = args.addressSpace;
      return addressCommand(addr, addressSpaceName);
    },

    namespaces: () => availableNamespaces,

    authenticationServices: () => availableAuthenticationServices,
    addressSpaceSchema: () => availableAddressSpaceSchemas,
    addressSpaceSchema_v2: (parent, args, context, info) => {
      return availableAddressSpaceSchemas.filter(
        o =>
          args.addressSpaceType === undefined ||
          o.metadata.name === args.addressSpaceType
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
            args.addressSpaceType === undefined ||
            o.spec.addressSpaceType === args.addressSpaceType
        )
        .sort(o => o.spec.displayOrder);
    },
    addressSpaceTypes: () => ["standard", "brokered"],
    addressSpaceTypes_v2: (parent, args, context, info) => {
      return availableAddressSpaceTypes.sort(o => o.spec.displayOrder);
    },
    addressSpacePlans: (parent, args, context, info) => {
      return availableAddressSpacePlans
        .filter(
          o =>
            args.addressSpaceType === undefined ||
            o.spec.addressSpaceType === args.addressSpaceType
        )
        .sort(o => o.spec.displayOrder);
    },
    addressPlans: (parent, args, context, info) => {
      var plans = availableAddressPlans;
      if (args.addressSpacePlan) {
        var spacePlan = availableAddressSpacePlans.find(
          o => o.metadata.name === args.addressSpacePlan
        );
        if (spacePlan === undefined) {
          var knownPlansNames = availableAddressSpacePlans.map(
            p => p.metadata.name
          );
          throw `Unrecognised address space plan '${args.addressSpacePlan}', known ones are : ${knownPlansNames}`;
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
    addressSpaces: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var copy = clone(addressSpaces);
      copy.forEach(as => {
        as.metrics = makeAddressSpaceMetrics(as);
      });
      var as = copy.filter(as => filterer.evaluate(as)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, as.length);
      var page = as.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: as.length,
        addressSpaces: page
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
    },

    devices: (parent, args, context, info) => {
      var iotProject = args.iotproject;
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      copy = getIotDevices(iotProject);

      var as = copy.filter(as => filterer.evaluate(as)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, as.length);
      var page = as.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: as.length,
        devices: page
      };
    },
    credentials: (parent, args, context, info) => {
      var iotProject = args.iotproject;
      var deviceId = args.deviceId;
      var filterer = buildFilterer(args.filter);
      var creds = getIotCredentials(iotProject, deviceId);
      var copy = clone(creds);
      var copyCreds = JSON.parse(copy);
      var resultCreds = copyCreds.filter(me => filterer.evaluate(me));
      return {
        total: creds.length,
        credentials: JSON.stringify(resultCreds)
      };
    },
    allProjects: (parent, args, context, info) => {
      var projectType = args.projectType;
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      // fetch address spaces
      if (projectType === undefined || projectType === "addressSpace") {
        resultAs = clone(addressSpaces);
        resultAs.forEach(as => {
          as.metrics = makeAddressSpaceMetrics(as);
        });
        var as = resultAs.filter(as => filterer.evaluate(as)).sort(orderBy);
        var paginationBounds = calcLowerUpper(
          args.offset,
          args.first,
          as.length
        );
        var pageAs = as.slice(paginationBounds.lower, paginationBounds.upper);
      }
      if (projectType === undefined || projectType === "iotProject") {
        // fetch iot projects
        resultPj = clone(iotProjects);
        var pj = resultPj.filter(pj => filterer.evaluate(pj)).sort(orderBy);
        var paginationBounds = calcLowerUpper(
          args.offset,
          args.first,
          pj.length
        );
        var pagePj = pj.slice(paginationBounds.lower, paginationBounds.upper);

        return {
          total: pj.length,
          addressSpaces: pageAs,
          iotProjects: pagePj
        };
      }
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
              l.spec.connection.spec.addressSpace + "."
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
    addressSpace: (parent, args, context, info) => {
      var as = addressSpaces.find(
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
