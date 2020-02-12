/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const uuidv1 = require('uuid/v1');
const { ApolloServer, gql } = require('apollo-server');
const typeDefs = require('./schema');
const { applyPatch, compare } = require('fast-json-patch');
const parser = require('./filter_parser.js');
const clone = require('clone');
const orderer = require('./orderer.js');
const _ = require('lodash');

function calcLowerUpper(offset, first, len) {
  var lower = 0;
  if (offset !== undefined && offset > 0) {
    lower = Math.min(offset, len);
  }
  var upper = len;
  if (first !== undefined && first > 0) {
    upper = Math.min(lower + first, len);
  }
  return {lower, upper};
}

var stateChangeTimeout = process.env.STATE_CHANGE_TIMEOUT;
if (!stateChangeTimeout) {
  stateChangeTimeout = 30000;
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
      longDescription: "The brokered address space type is the \"classic\" message broker in the cloud which supports AMQP, CORE, OpenWire, and MQTT protocols. It supports JMS with transactions, message groups, selectors on queues and so on.",
      displayOrder: 0,
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
      longDescription: "The standard address space type is the default type in EnMasse, and is focused on scaling in the number of connections and the throughput of the system. It supports AMQP and MQTT protocols.",
      displayOrder: 1,
    }
  },
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
      longDescription: "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases where you want to decouple the producer and consumer. A queue in the brokered address space supports selectors, message groups, transactions, and other JMS features. Message order can be lost with released messages.",
      displayOrder: 0,
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
      shortDescription: "A publish-and-subscribe address with store-and-forward semantics",
      longDescription: "The topic address type supports the publish-subscribe messaging pattern in which there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      displayOrder: 1,
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
      shortDescription: "A scalable 'direct' address for sending messages to one consumer",
      longDescription: "The anycast address type is a scalable direct address for sending messages to one consumer. Messages sent to an anycast address are not stored, but are instead forwarded directly to the consumer. This method makes this address type ideal for request-reply (RPC) uses or even work distribution. This is the cheapest address type as it does not require any persistence.",
      displayOrder: 0,
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
      shortDescription: "A scalable 'direct' address for sending messages to multiple consumers",
      longDescription: "The multicast address type is a scalable direct address for sending messages to multiple consumers. Messages sent to a multicast address are forwarded to all consumers receiving messages on that address. Because message acknowledgments from consumers are not propagated to producers, only pre-settled messages can be sent to multicast addresses.",
      displayOrder: 1,
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
      longDescription: "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases when you want to decouple the producer and consumer. A queue can be sharded across multiple storage units. Message ordering might be lost for queues in the standard address space.",
      displayOrder: 2,
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
      longDescription: "The subscription address type allows a subscription to be created for a topic that holds messages published to the topic even if the subscriber is not attached. The subscription is accessed by the consumer using <topic-address>::<subscription-address>. For example, for a subscription `mysub` on a topic `mytopic` the consumer consumes from the address `mytopic::mysub`.",
      displayOrder: 3,
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
      longDescription: "The topic address type supports the publish-subscribe messaging pattern where there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      displayOrder: 4,
    }
  },
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

const availableAddressSpaceSchemas = [
  {
    metadata: {
      name: "brokered"
    },
    spec: {
      authenticationServices: ["none-authservice", "standard-authservice"],
      description:
        "A brokered address space consists of a broker combined with a console for managing addresses."
    }
  },
  {
    metadata: {
      name: "standard"
    },
    spec: {
      authenticationServices: ["none-authservice", "standard-authservice"],
      description:
        "A standard address space consists of an AMQP router network in combination with attachable 'storage units'. The implementation of a storage unit is hidden from the client and the routers with a well defined API."
    }
  }
];

const availableNamespaces = [
  {
    metadata: {
      name: "app1_ns",
    },
    status: {
      phase: "Active"
    }
  },
  {
    metadata: {
      name: "app2_ns",
    },
    status: {
      phase: "Active"
    }
  }
];

function createAddressPlan(name, addressType, displayName, shortDescription, longDescription, resources, displayOrder)
{
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
  createAddressPlan("standard-small-topic",
      "topic",
      "Small Topic",
      "Creates a small topic sharing underlying broker with other topics.",
      "Creates a small topic sharing underlying broker with other topics.",
      {
        "broker": 0
      },
      8),
  createAddressPlan("standard-medium-topic",
      "topic",
      "Medium Topic",
      "Creates a medium topic sharing underlying broker with other topics.",
      "Creates a medium topic sharing underlying broker with other topics.",
      {
        "broker": 0
      },
      9),
  createAddressPlan("standard-small-subscription",
      "subscription",
      "Small Subscription",
      "Creates a small durable subscription on a topic.",
      "Creates a small durable subscription on a topic, which is then accessed as a distinct address.",
      {
        "broker": 0
      },
      10),
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
      addressSpaceType: "standard",
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
      addressSpaceType: "standard",
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
      addressSpaceType: "brokered",
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

function getRandomCreationDate(floor) {

  var created = new Date().getTime() - (Math.random() * 1000 * 60 * 60 * 24);
  if (floor && created < floor.getTime()) {
    created = floor.getTime();
  }
  var date = new Date();
  date.setTime(created);
  return date;
}

function scheduleSetAddressSpaceStatus(addressSpace, phase, messages) {
  setTimeout(() => {
    addressSpace.status = {
      isReady: phase === "Active",
      messages: messages,
      phase: phase
    };
    if (phase !== "Active") {
      scheduleSetAddressSpaceStatus(addressSpace, "Active", []);
    }
  }, stateChangeTimeout);
}

function createAddressSpace(as) {
  var namespace = availableNamespaces.find(n => n.metadata.name === as.metadata.namespace);
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${as.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  if (as.spec.type !== 'brokered' && as.spec.type !== 'standard') {
    throw `Unrecognised address space type '${(as.spec.type)}', known ones are : brokered, standard`;
  }

  var spacePlan = availableAddressSpacePlans.find(o => o.metadata.name === as.spec.plan && as.spec.type === o.spec.addressSpaceType);
  if (spacePlan === undefined) {
    var knownPlansNames = availableAddressSpacePlans.filter(p => as.spec.type === p.spec.addressSpaceType).map(p => p.metadata.name);
    throw `Unrecognised address space plan '${as.spec.plan}', known plans for type '${as.spec.type}' are : ${knownPlansNames}`;
  }

  if (addressSpaces.find(existing => as.metadata.name === existing.metadata.name && as.metadata.namespace === existing.metadata.namespace) !== undefined) {
    throw `Address space with name  '${as.metadata.name} already exists in namespace ${as.metadata.namespace}`;
  }

  var phase = "Active";
  var messages = [];
  if (as.status && as.status.phase) {
    phase = as.status.phase
  }
  if (phase !== "Active") {
    messages.push("The following deployments are not ready: [admin.daf7b31]")
  }

  var addressSpace = {
    metadata: {
      name: as.metadata.name,
      namespace: namespace.metadata.name,
      uid: uuidv1(),
      creationTimestamp: as.metadata.creationTimestamp ? as.metadata.creationTimestamp : getRandomCreationDate()
    },
    spec: {
      plan: spacePlan,
      type: as.spec.type,
      authenticationService: {
        name: as.spec.authenticationService.name
      }
    },
    status: null
  };

  scheduleSetAddressSpaceStatus(addressSpace, phase, messages);

  addressSpaces.push(addressSpace);
  return addressSpace.metadata;
}

function patchAddressSpace(metadata, jsonPatch, patchType) {
  var index = addressSpaces.findIndex(existing => metadata.name === existing.metadata.name && metadata.namespace === existing.metadata.namespace);
  if (index < 0) {
    throw `Address space with name  '${metadata.name}' in namespace ${metadata.namespace} does not exist`;
  }

  var knownPatchTypes = ["application/json-patch+json", "application/merge-patch+json", "application/strategic-merge-patch+json"];
  if (knownPatchTypes.find(p => p === patchType) === undefined) {
    throw `Unsupported patch type '$patchType'`
  } else if ( patchType !== 'application/json-patch+json') {
    throw `Unsupported patch type '$patchType', this mock currently supports only 'application/json-patch+json'`;
  }

  var patch = JSON.parse(jsonPatch);
  var current = JSON.parse(JSON.stringify(addressSpaces[index]));
  var patched = applyPatch(JSON.parse(JSON.stringify(current)) , patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    if (!_.isEqual(replacement.spec.plan, current.spec.plan)) {
      var replacementPlan = typeof(replacement.spec.plan) === "string" ? replacement.spec.plan : replacement.metadata.name;
      var spacePlan = availableAddressSpacePlans.find(o => o.metadata.name === replacementPlan);
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressSpacePlans.map(p => p.metadata.name);
        throw `Unrecognised address space plan '${replacementPlan}', known ones are : ${knownPlansNames}`;
      }
      replacement.spec.plan = spacePlan;
    }

    addressSpaces[index].spec = replacement.spec;
    return addressSpaces[index];
  } else {
    throw `Failed to patch address space with name  '${metadata.name}' in namespace ${metadata.namespace}`
  }
}

function deleteAddressSpace(objectmeta) {
  var index = addressSpaces.findIndex(existing => objectmeta.name === existing.metadata.name && objectmeta.namespace === existing.metadata.namespace);
  if (index < 0) {
    throw `Address space with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }
  var as = addressSpaces[index];
  delete addressspace_connection[as.metadata.uid];

  addressSpaces.splice(index, 1);
}


var addressSpaces = [];

createAddressSpace(
    {
      metadata: {
        name: "jupiter_as1",
        namespace: availableNamespaces[0].metadata.name,
      },
      spec: {
        plan: "standard-small",
        type: "standard",
        authenticationService: {
          name: "none-authservice"
        }
      }
    });

createAddressSpace(
    {
      metadata: {
        name: "saturn_as2",
        namespace: availableNamespaces[0].metadata.name,
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

createAddressSpace(
    {
      metadata: {
        name: "mars_as2",
        namespace: availableNamespaces[1].metadata.name,
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

function createConnection(addressSpace, hostname) {
  var port = Math.floor(Math.random() * 25536) + 40000;
  var hostport = hostname + ":" + port;
  var encrypted = (port % 2 === 0);
  var properties = [];
  if (addressSpace.spec.Type === "standard") {
    properties = [
      {
        "key": "platform",
        "value": "JVM: 1.8.0_191, 25.191-b12, Oracle Corporation, OS: Mac OS X, 10.13.6, x86_64"
      },
      {
        "key": "product",
        "value": "QpidJMS"
      },
      {
        "key": "version",
        "value": "0.38.0-SNAPSHOT"
      }
    ];

  }
  return {
    metadata: {
      name: hostport,
      uid: uuidv1() + "",
      namespace: addressSpace.metadata.namespace,
      creationTimestamp: getRandomCreationDate(addressSpace.metadata.creationTimestamp)
    },
    spec: {
      addressSpace: addressSpace.metadata.name,
      hostname: hostport,
      containerId: uuidv1() + "",
      protocol: encrypted ? "amqps" : "amqp",
      ecrypted: encrypted,
      poperties: properties,
      metrics: []
    }
  };
}

connections = connections.concat(["juno",
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
].map(
    (n) => (
        createConnection(addressSpaces[0], n)
    )
));

connections = connections.concat(["dragonfly"].map(
    (n) => (
        createConnection(addressSpaces[1], n)
    )
));

connections = connections.concat(["kosmos",
                                  "mariner4",
                                  "mariner5",
                                  "zond2",
                                  "mariner6",
                                  "nozomi",
                                  "rosetta",
                                  "yinghuo1",
                                  "pathfinder"
].map(
    (n) => (
        createConnection(addressSpaces[2], n)
    )
));

var addressspace_connection = {};
addressSpaces.forEach(as => {
  addressspace_connection[as.metadata.uid] = connections.filter((c) => c.spec.addressSpace === as.metadata.name);
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
  if (/^[a-z0-9][-a-z0-9_.]*[a-z0-9]$/.test(clean) && addressSpaceName.length < maxLength) {
    return addressSpaceName + "." + clean;
  } else {
    clean = clean.replace(/[^-a-z0-9_.]/g, "");
    if (clean.charAt(0) === '-' || clean.charAt(0) === '.' || clean.charAt(0) === '_') clean = clean.substring(1);
    if (clean.charAt(clean.length - 1) === '-' || clean.charAt(clean.length - 1) === '.' || clean.charAt(clean.length - 1) === '_') clean = clean.substring(0, clean.length - 1);
    var uid = "" + uuidv1();
    maxLength = 253 - addressSpaceName.length - uid.length - 2;
    if (clean.length > maxLength) clean = clean.substring(0, maxLength);
    return  addressSpaceName + "." + clean + "." + uid;
  }
}

function createAddress(addr) {
  var namespace = availableNamespaces.find(n => n.metadata.name === addr.metadata.namespace);
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.metadata.name);
    throw `Unrecognised namespace '${addr.metadata.namespace}', known ones are : ${knownNamespaces}`;
  }

  var addressSpacesInNamespace = addressSpaces.filter(as => as.metadata.namespace === addr.metadata.namespace);
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
    addr.metadata.name = defaultResourceNameFromAddress(addr.spec.address, addressSpaceName);
  } else {
    throw `address is undefined, cannot default resource name`
  }

  var addressSpace = addressSpacesInNamespace.find(as => as.metadata.name === addressSpaceName);
  if (addressSpace === undefined) {
    var addressspacenames = addressSpacesInNamespace.map(p => p.metadata.Name);
    throw `Unrecognised address space '${addressSpaceName}', known ones are : ${addressspacenames}`;
  }

  var knownTypes = ['queue', 'topic', 'subscription', 'multicast', 'anycast'];
  if (knownTypes.find(t => t === addr.spec.type) === undefined) {
    throw `Unrecognised address type '${addr.spec.type}', known ones are : '${knownTypes}'`;
  }

  var plan = availableAddressPlans.find(p => p.metadata.name === addr.spec.plan && addr.spec.type === p.spec.addressType);
  if (plan === undefined) {
    var knownPlansNames = availableAddressPlans.filter(p => addr.spec.Type === p.spec.addressType).map(p => p.metadata.name);
    throw `Unrecognised address plan '${addr.spec.plan}', known plans for type '${addr.spec.type}' are : ${knownPlansNames}`;
  }

  if (addr.spec.type === 'subscription') {
      var topics  = addresses.filter(a => a.metadata.name.startsWith(addressSpaceName) && a.spec.type === "topic");
      if (!addr.spec.topic) {
        throw `Spec.Topic is mandatory for the subscription type`;
      } else if (topics.find(t => t.spec.address === addr.spec.topic) === undefined) {
        var topicNames  = topics.map(t => t.spec.address);
        throw `Unrecognised topic address '${addr.spec.topic}', known ones are : '${topicNames}'`;
      }
  } else {
      if (addr.spec.topic) {
        throw `spec.topic is not allowed for the address type '${addr.spec.type}'.`;
      }
  }

  if (addresses.find(existing => addr.metadata.name === existing.metadata.name && addr.metadata.namespace === existing.metadata.namespace) !== undefined) {
    throw `Address with name  '${addr.metadata.name} already exists in address space ${addressSpaceName}`;
  }

  var phase = "Active";
  var messages = [];
  if (addr.status && addr.status.phase) {
    phase = addr.status.phase
  }
  if (phase !== "Active") {
    messages.push("Address " + addr.metadata.name + " not found on qdrouterd")
  }

  var planStatus = null;
  if (addressSpace.spec.type === "standard") {
    planStatus = {
      name: plan.metadata.name,
      partitions: 1
    }
  }

  var address = {
    metadata: {
      name: addr.metadata.name,
      namespace: addr.metadata.namespace,
      uid: uuidv1(),
      creationTimestamp: addr.metadata.creationTimestamp ? addr.metadata.creationTimestamp : getRandomCreationDate()
    },
    spec: {
      address: addr.spec.address,
      addressSpace: addr.spec.addressSpace,
      plan: plan,
      type: addr.spec.type,
      topic: addr.spec.topic
    },
    status: null,
  };
  scheduleSetAddressStatus(address, phase, messages, planStatus);
  addresses.push(address);
  return address.metadata;
}

function patchAddress(objectmeta, jsonPatch, patchType) {
  var index = addresses.findIndex(existing => objectmeta.name === existing.metadata.name && objectmeta.namespace === existing.metadata.namespace);
  if (index < 0) {
    throw `Address with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }

  var knownPatchTypes = ["application/json-patch+json", "application/merge-patch+json", "application/strategic-merge-patch+json"];
  if (knownPatchTypes.find(p => p === patchType) === undefined) {
    throw `Unsupported patch type '$patchType'`
  } else if ( patchType !== 'application/json-patch+json') {
    throw `Unsupported patch type '$patchType', this mock currently supports only 'application/json-patch+json'`;
  }

  var patch = JSON.parse(jsonPatch);
  var current = JSON.parse(JSON.stringify(addresses[index]));
  var patched = applyPatch(JSON.parse(JSON.stringify(current)) , patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    if (!_.isEqual(replacement.spec.plan,current.spec.plan)) {
      var replacementPlan = typeof(replacement.spec.plan) === "string" ? replacement.spec.plan : replacement.plan.metadata.name;
      var spacePlan = availableAddressPlans.find(o => o.metadata.name === replacementPlan);
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressPlans.map(p => p.metadata.name);
        throw `Unrecognised address plan '${replacementPlan}', known ones are : ${knownPlansNames}`;
      }
      replacement.spec.plan = spacePlan;
    }

    addresses[index].spec = replacement.spec;
    return addresses[index];
  } else {
    throw `Failed to patch address with name  '${objectmeta.name}' in namespace ${objectmeta.namespace}`
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
  var index = addresses.findIndex(existing => metadata.name === existing.metadata.name && metadata.namespace === existing.metadata.namespace);
  if (index < 0) {
    throw `Address with name  '${metadata.name}' in namespace ${metadata.namespace} does not exist`;
  }
  addresses.splice(index, 1);
}

function purgeAddress(objectmeta) {
  var index = addresses.findIndex(existing => objectmeta.name === existing.metadata.name && objectmeta.namespace === existing.metadata.namespace);
  if (index < 0) {
    throw `Address with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist`;
  }
}

function closeConnection(objectmeta) {

  var index = connections.findIndex(existing => objectmeta.name === existing.metadata.name && objectmeta.namespace === existing.metadata.namespace);
  if (index < 0) {
    var knownCons = connections.filter(c => c.metadata.namespace === objectmeta.namespace).map(c => c.metadata.name);
    throw `Connection with name  '${objectmeta.name}' in namespace ${objectmeta.namespace} does not exist. Known connection names are: ${knownCons}`;
  }
  var targetCon = connections[index];

  var addressSpaceName = connections[index].spec.addressSpace;
  var as = addressSpaces.find(as => as.metadata.name === addressSpaceName);

  var as_cons = addressspace_connection[as.metadata.uid];
  var as_cons_index = as_cons.findIndex((c) => c === targetCon);
  as_cons.splice(as_cons_index, 1);

  connections.splice(index, 1);

}

["ganymede", "callisto", "io", "europa", "amalthea", "himalia", "thebe", "elara", "pasiphae", "metis", "carme", "sinope"].map(n =>
    (createAddress({
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
        phase: n.startsWith("c") ? "Configuring" : (n.startsWith("p") ? "Pending" : "Active")
      }
    })));

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
        phase: topicName.startsWith("c") ? "Configuring" : (topicName.startsWith("p") ? "Pending" : "Active")
      }
    });
    var subname = topicName + '-sub';
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
        phase: topicName.startsWith("c") ? "Configuring" : (topicName.startsWith("p") ? "Pending" : "Active")
      }
    });
}

// Topic with a subscription
["themisto"].map(n => (createTopicWithSub(addressSpaces[0], n)));


["titan", "rhea", "iapetus", "dione", "tethys", "enceladus", "mimas"].map(n =>
    (createAddress({
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
    })));

["phobos", "deimous"].map(n =>
    (createAddress({
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
    })));

function* makeAddrIter(namespace, addressspace) {
  var filter = addresses.filter(a => a.metadata.namespace === namespace && a.metadata.name.startsWith(addressspace + "."));
  var i = 0;
  while(filter.length) {
    var addr = filter[i++ % filter.length];
    yield addr;
  }
}

var addressItrs = {};
addressSpaces.forEach((as) => {
  addressItrs[as.metadata.uid] = makeAddrIter(as.metadata.namespace, as.metadata.name);
});

var links = [];
connections.forEach(c => {
  var addressSpaceName = c.spec.addressSpace;
  var addressSpace = addressSpaces.find(as => as.metadata.name === addressSpaceName);
  var uid = addressSpace.metadata.uid;
  var addr = addressItrs[uid].next().value;

  for (var i = 0; i< addr.metadata.name.length; i++) {
    links.push(
        {
          metadata: {
            name: uuidv1(),
          },
          spec: {
            connection: c,
            address: addr.metadata.name,
            role: i % 2 === 0 ? "sender" : "receiver",
          }
        });
  }
});

function buildFilterer(filter) {
  return filter ? parser.parse(filter) : {evaluate: () => true};
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
    },

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
    },
  ];
}

function makeMockLinkMetrics(is_addr_query, link) {
  if (is_addr_query) {

    return [
      {
        name: link.Spec.Role === "sender" ? "enmasse_messages_in" : "enmasse_messages_out",
        type: "gauge",
        value: Math.floor(Math.random() * 10),
        units: "msg/s"
      },
      {
        name: "enmasse_messages_backlog",
        type: "gauge",
        value: Math.floor(Math.random() * 15),
        units: "msg"
      },
    ];
  } else {

    var addressSpaceName = link.Spec.connection.spec.addressSpace;
    var as = addressSpaces.find(as => as.metadata.name === addressSpaceName);
    if (as.Spec.Type === "brokered") {
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
        },
      ];

    }
  }
}

function makeAddressSpaceMetrics(as) {
  var cons = as.metadata.uid in addressspace_connection ? addressspace_connection[as.metadata.uid] : [];
  var addrs = addresses.filter((a) => as.metadata.namespace === a.metadata.namespace &&
      a.metadata.name.startsWith(as.metadata.name + "."));

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
    },
  ];
}

function addressCommand(addr, addressSpaceName) {
  if (addr.metadata.name) {
    // pass
  } else if (addr.spec.address) {
    if (!addressSpaceName) {
      throw `addressSpace is not provided, cannot default resource name from address '${addr.spec.address}'`;
    }
    addr.metadata.name = defaultResourceNameFromAddress(addr.spec.address, addressSpaceName);
  } else {
    throw `address is undefined, cannot default resource name`
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
    purgeAddress: (parent, args) => {
      purgeAddress(args.input);
      return true;
    },
    closeConnection: (parent, args) => {
      closeConnection(args.input);
      return true;
    },
  },
  Query: {
    hello: () => 'world',

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

    addressTypes: () => (['queue', 'topic', 'subscription', 'multicast', 'anycast']),
    addressTypes_v2: (parent, args, context, info) => {
      return availableAddressTypes
          .filter(o => args.addressSpaceType === undefined || o.spec.addressSpaceType === args.addressSpaceType)
          .sort(o => o.Spec.DisplayOrder);
    },
    addressSpaceTypes: () => (['standard', 'brokered']),
    addressSpaceTypes_v2: (parent, args, context, info) => {
      return availableAddressSpaceTypes
          .sort(o => o.Spec.DisplayOrder);
    },
    addressSpacePlans: (parent, args, context, info) => {
      return availableAddressSpacePlans
          .filter(o => args.addressSpaceType === undefined || o.spec.addressSpaceType === args.addressSpaceType)
          .sort(o => o.Spec.DisplayOrder);
    },
    addressPlans: (parent, args, context, info) => {
      var plans = availableAddressPlans;
      if (args.addressSpacePlan) {
        var spacePlan = availableAddressSpacePlans.find(o => o.metadata.name === args.addressSpacePlan);
        if (spacePlan === undefined) {
          var knownPlansNames = availableAddressSpacePlans.map(p => p.metadata.name);
          throw `Unrecognised address space plan '${args.addressSpacePlan}', known ones are : ${knownPlansNames}`;
        }
        plans = spacePlan.spec.addressPlans;
      }

      return plans.filter(p => (args.addressType === undefined || p.spec.addressType === args.addressType)).sort(o => o.Spec.DisplayOrder);
    },
    addressSpaces:(parent, args, context, info) => {

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
        AddressSpaces: page
      };
    },
    addresses:(parent, args, context, info) => {

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
    connections:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);
      var copy = clone(connections);
      copy.forEach(c => {
        c.Metrics = makeMockConnectionMetrics();
      });
      var cons = copy.filter(c => filterer.evaluate(c)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, cons.length);
      var page = cons.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: cons.length,
        connections: page
      };

    }
  },

  AddressSpace_consoleapi_enmasse_io_v1beta1: {
    connections:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var as = parent;
      var cons = as.metadata.uid in addressspace_connection ? addressspace_connection[as.metadata.uid] : [];
      var copy = clone(cons);
      copy.forEach(c => {
        c.metrics = makeMockConnectionMetrics();
      });

      copy = copy.filter(c => filterer.evaluate(c)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, copy.length);
      var page = copy.slice(paginationBounds.lower, paginationBounds.upper);
      return {total: copy.length, connections: page};
    },
    addresses:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var as = parent;

      var copy = clone(addresses.filter(a => as.metadata.namespace === a.metadata.namespace && a.metadata.name.startsWith(as.metadata.name + ".")));
      copy.forEach(a => {
        a.metrics = makeMockAddressMetrics();
      });

      var addrs = copy.filter(a => filterer.evaluate(a)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, addrs.length);
      var page = addrs.slice(paginationBounds.lower, paginationBounds.upper);
      return {total: addrs.length,
        addresses: page};
    },
  },
  Address_consoleapi_enmasse_io_v1beta1: {
    links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);


      var addr = parent;
      var copy = clone(links.filter((l) => l.Spec.Connection.metadata.namespace === addr.metadata.namespace && addr.metadata.name.startsWith(l.Spec.Connection.spec.addressSpace + ".")));
      copy.forEach(l => {
        l.metrics = makeMockLinkMetrics(true, l);
      });
      var addrlinks = copy.filter(l => filterer.evaluate(l)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, addrlinks.length);
      var page = addrlinks.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: addrlinks.length,
        links: page
      };
    },
  },
  Connection_consoleapi_enmasse_io_v1beta1: {
    links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var con = parent;
      var copy = clone(links.filter((l) => _.isEqual(l.spec.connection.metadata, con.metadata)));
      copy.forEach(l => {
        l.metrics = makeMockLinkMetrics(false, l);
      });

      var connlinks = copy.filter(l => filterer.evaluate(l)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, connlinks.length);
      var page = connlinks.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        total: connlinks.length,
        links: page
      };
    },
  },
  ConnectionSpec_consoleapi_enmasse_io_v1beta1: {
    addressSpace: (parent, args, context, info) => {
      var as = addressSpaces.find(as => as.metadata.name ===  parent.AddressSpace);
      return as
    },
  },

  Link_consoleapi_enmasse_io_v1beta1: {
  },
  ObjectMeta_v1 : {
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
    fullname: "Valentina Tereshkova",
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
  });

  server.listen().then(({ url }) => {
    console.log(`? Server ready at ${url}`);
  });
}


module.exports.createAddress = createAddress;
module.exports.patchAddress = patchAddress;
module.exports.patchAddressSpace = patchAddressSpace;
module.exports.addressCommand = addressCommand;
module.exports.resolvers = resolvers;
