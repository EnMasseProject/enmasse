
const uuidv1 = require('uuid/v1');
const traverse = require('traverse');
const fs = require('fs');
const path = require('path');
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

const availableAddressSpaceTypes = [
  {
    ObjectMeta: {
      Name: "brokered",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "brokered",
      DisplayName: "brokered",
      ShortDescription: "The brokered address space.",
      LongDescription: "The brokered address space type is the \"classic\" message broker in the cloud which supports AMQP, CORE, OpenWire, and MQTT protocols. It supports JMS with transactions, message groups, selectors on queues and so on.",
      DisplayOrder: 0,
    }
  },
  {
    ObjectMeta: {
      Name: "standard",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      DisplayName: "standard",
      ShortDescription: "The standard address space.",
      LongDescription: "The standard address space type is the default type in EnMasse, and is focused on scaling in the number of connections and the throughput of the system. It supports AMQP and MQTT protocols.",
      DisplayOrder: 1,
    }
  },
];

const availableAddressTypes = [
  {
    ObjectMeta: {
      Name: "brokered.queue",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "brokered",
      DisplayName: "queue",
      ShortDescription: "A store-and-forward queue",
      LongDescription: "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases where you want to decouple the producer and consumer. A queue in the brokered address space supports selectors, message groups, transactions, and other JMS features. Message order can be lost with released messages.",
      DisplayOrder: 0,
    }
  },
  {
    ObjectMeta: {
      Name: "brokered.topic",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "brokered",
      DisplayName: "topic",
      ShortDescription: "A publish-and-subscribe address with store-and-forward semantics",
      LongDescription: "The topic address type supports the publish-subscribe messaging pattern in which there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      DisplayOrder: 1,
    }
  },
  {
    ObjectMeta: {
      Name: "standard.anycast",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      DisplayName: "anycast",
      ShortDescription: "A scalable 'direct' address for sending messages to one consumer",
      LongDescription: "The anycast address type is a scalable direct address for sending messages to one consumer. Messages sent to an anycast address are not stored, but are instead forwarded directly to the consumer. This method makes this address type ideal for request-reply (RPC) uses or even work distribution. This is the cheapest address type as it does not require any persistence.",
      DisplayOrder: 0,
    }
  },
  {
    ObjectMeta: {
      Name: "standard.multicast",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      DisplayName: "multicast",
      ShortDescription: "A scalable 'direct' address for sending messages to multiple consumers",
      LongDescription: "The multicast address type is a scalable direct address for sending messages to multiple consumers. Messages sent to a multicast address are forwarded to all consumers receiving messages on that address. Because message acknowledgments from consumers are not propagated to producers, only pre-settled messages can be sent to multicast addresses.",
      DisplayOrder: 1,
    }
  },
  {
    ObjectMeta: {
      Name: "standard.queue",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      DisplayName: "queue",
      ShortDescription: "A store-and-forward queue",
      LongDescription: "The queue address type is a store-and-forward queue. This address type is appropriate for implementing a distributed work queue, handling traffic bursts, and other use cases when you want to decouple the producer and consumer. A queue can be sharded across multiple storage units. Message ordering might be lost for queues in the standard address space.",
      DisplayOrder: 2,
    }
  },
  {
    ObjectMeta: {
      Name: "standard.subscription",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      DisplayName: "subscription",
      ShortDescription: "A subscription on a specified topic",
      LongDescription: "The subscription address type allows a subscription to be created for a topic that holds messages published to the topic even if the subscriber is not attached. The subscription is accessed by the consumer using <topic-address>::<subscription-address>. For example, for a subscription `mysub` on a topic `mytopic` the consumer consumes from the address `mytopic::mysub`.",
      DisplayOrder: 3,
    }
  },
  {
    ObjectMeta: {
      Name: "standard.topic",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      DisplayName: "topic",
      ShortDescription: "A publish-subscribe topic",
      LongDescription: "The topic address type supports the publish-subscribe messaging pattern where there are 1..N producers and 1..M consumers. Each message published to a topic address is forwarded to all subscribers for that address. A subscriber can also be durable, in which case messages are kept until the subscriber has acknowledged them.",
      DisplayOrder: 4,
    }
  },
];

const availableAuthenticationServices = [
  createAuthenticationService("none", "none-authservice"),
  createAuthenticationService("standard", "standard-authservice")
];

function createAuthenticationService(type, name) {
  return {
    Spec: {
      Type: type
    },
    ObjectMeta: {
      Name: name,
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    }
  };
}

const availableAddressSpaceSchemas = [
  {
    ObjectMeta: {
      Name: "brokered"
    },
    Spec: {
      AuthenticationServices: ["none-authservice", "standard-authservice"],
      Description:
        "A brokered address space consists of a broker combined with a console for managing addresses."
    }
  },
  {
    ObjectMeta: {
      Name: "standard"
    },
    Spec: {
      AuthenticationServices: ["none-authservice", "standard-authservice"],
      Description:
        "A standard address space consists of an AMQP router network in combination with attachable 'storage units'. The implementation of a storage unit is hidden from the client and the routers with a well defined API."
    }
  }
];

const availableNamespaces = [
  {
    ObjectMeta: {
      Name: "app1_ns",
    },
    Status: {
      Phase: "Active"
    }
  },
  {
    ObjectMeta: {
      Name: "app2_ns",
    },
    Status: {
      Phase: "Active"
    }
  }
];

function createAddressPlan(name, addressType, displayName, shortDescription, longDescription, resources, displayOrder)
{
  return {
    ObjectMeta: {
      Name: name,
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressType: addressType,
      DisplayName: displayName,
      DisplayOrder: displayOrder,
      LongDescription: longDescription,
      Resources: resources,
      ShortDescription: shortDescription
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
  createAddressPlan("standard-small-multicast",
      "multicast",
      "Small Multicast",
      "Creates a small multicast address.",
      "Creates a small multicast address where messages go via a router that does not take ownership of the messages.",
      {
        "router": 0.001
      },
      3),
  createAddressPlan("standard-small-topic",
      "topic",
      "Small Topic",
      "Creates a small topic sharing underlying broker with other topics.",
      "Creates a small topic sharing underlying broker with other topics.",
      {
        "broker": 0
      },
      4),
  createAddressPlan("standard-small-subscription",
      "subscription",
      "Small Subscription",
      "Creates a small durable subscription on a topic.",
      "Creates a small durable subscription on a topic, which is then accessed as a distinct address.",
      {
        "broker": 0
      },
      5),
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
    ObjectMeta: {
      Name: "standard-small",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      AddressPlans: availableAddressPlans.filter(p => !p.ObjectMeta.Name.startsWith("brokered-")),
      DisplayName: "Small",
      ShortDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
      LongDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis. This plan allows up to 1 router and 1 broker in total, and is suitable for small applications using small address plans and few addresses.",
      DisplayOrder: 0,
      ResourceLimits: {
        aggregate: 2,
        broker: 1,
        router: 1
      }
    }
  },
  {
    ObjectMeta: {
      Name: "standard-medium",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "standard",
      AddressPlans: availableAddressPlans.filter(p => !p.ObjectMeta.Name.startsWith("brokered-")),
      DisplayName: "Medium",
      ShortDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis",
      LongDescription: "Messaging infrastructure based on Apache Qpid Dispatch Router and Apache ActiveMQ Artemis. This plan allows up to 3 routers and 3 broker in total, and is suitable for applications using small address plans and few addresses.",
      DisplayOrder: 1,
      ResourceLimits: {
        aggregate: 2.0,
        broker: 3.0,
        router: 3.0
      }
    }
  },
  {
    ObjectMeta: {
      Name: "brokered-single-broker",
      Uid: uuidv1(),
      CreationTimestamp: getRandomCreationDate()
    },
    Spec: {
      AddressSpaceType: "brokered",
      AddressPlans: availableAddressPlans.filter(p => p.ObjectMeta.Name.startsWith("brokered-")),
      DisplayName: "Single Broker",
      ShortDescription: "Single Broker instance",
      LongDescription: "Single Broker plan where you can create an infinite number of queues until the system falls over.",
      DisplayOrder: 0,
      ResourceLimits: {
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

function createAddressSpace(as) {
  var namespace = availableNamespaces.find(n => n.ObjectMeta.Name === as.ObjectMeta.Namespace);
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.ObjectMeta.Name);
    throw `Unrecognised namespace '${as.ObjectMeta.Namespace}', known ones are : ${knownNamespaces}`;
  }

  if (as.Spec.Type !== 'brokered' && as.Spec.Type !== 'standard') {
    throw `Unrecognised address space type '${(as.Spec.Type)}', known ones are : brokered, standard`;
  }

  var spacePlan = availableAddressSpacePlans.find(o => o.ObjectMeta.Name === as.Spec.Plan && as.Spec.Type === o.Spec.AddressSpaceType);
  if (spacePlan === undefined) {
    var knownPlansNames = availableAddressSpacePlans.filter(p => as.Spec.Type === p.Spec.AddressSpaceType).map(p => p.ObjectMeta.Name);
    throw `Unrecognised address space plan '${as.Spec.Plan}', known plans for type '${as.Spec.Type}' are : ${knownPlansNames}`;
  }

  if (addressSpaces.find(existing => as.ObjectMeta.Name === existing.ObjectMeta.Name && as.ObjectMeta.Namespace === existing.ObjectMeta.Namespace) !== undefined) {
    throw `Address space with name  '${as.ObjectMeta.Name} already exists in namespace ${as.ObjectMeta.Namespace}`;
  }

  var phase = "Active";
  var messages = [];
  if (as.Status && as.Status.Phase) {
    phase = as.Status.Phase
  }
  if (phase !== "Active") {
    messages.push("The following deployments are not ready: [admin.daf7b31]")
  }

  var addressSpace = {
    ObjectMeta: {
      Name: as.ObjectMeta.Name,
      Namespace: namespace.ObjectMeta.Name,
      Uid: uuidv1(),
      CreationTimestamp: as.ObjectMeta.CreationTimestamp ? as.ObjectMeta.CreationTimestamp : getRandomCreationDate()
    },
    Spec: {
      Plan: spacePlan,
      Type: as.Spec.Type
    },
    Status: {
      IsReady: phase === "Active",
      Messages: messages,
      Phase: phase
    }
  };

  addressSpaces.push(addressSpace);
  return addressSpace.ObjectMeta;
}

function patchAddressSpace(objectMeta, jsonPatch, patchType) {
  var index = addressSpaces.findIndex(existing => objectMeta.Name === existing.ObjectMeta.Name && objectMeta.Namespace === existing.ObjectMeta.Namespace);
  if (index < 0) {
    throw `Address space with name  '${objectMeta.Name}' in namespace ${objectMeta.Namespace} does not exist`;
  }

  var knownPatchTypes = ["application/json-patch+json", "application/merge-patch+json", "application/strategic-merge-patch+json"];
  if (knownPatchTypes.find(p => p === patchType) === undefined) {
    throw `Unsupported patch type '$patchType'`
  } else if ( patchType !== 'application/json-patch+json') {
    throw `Unsupported patch type '$patchType', this mock currently supports only 'application/json-patch+json'`;
  }

  var patch = JSON.parse(jsonPatch);
  var current = addressSpaces[index].Spec;
  var patched = applyPatch(JSON.parse(JSON.stringify(current)) , patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    if (replacement.Plan !== current.Plan) {
      var replacementPlan = typeof(replacement.Plan) === "string" ? replacement.Plan : replacement.ObjectMeta.Name;
      var spacePlan = availableAddressSpacePlans.find(o => o.ObjectMeta.Name === replacementPlan);
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressSpacePlans.map(p => p.ObjectMeta.Name);
        throw `Unrecognised address space plan '${replacement.Spec.Plan}', known ones are : ${knownPlansNames}`;
      }
      replacement.Plan = spacePlan;
    }

    addressSpaces[index].Spec = replacement;
    return true;
  } else {
    throw `Failed to patch address space with name  '${metadata.Name}' in namespace ${metadata.Namespace}`
  }
}

function deleteAddressSpace(objectmeta) {
  var index = addressSpaces.findIndex(existing => objectmeta.Name === existing.ObjectMeta.Name && objectmeta.Namespace === existing.ObjectMeta.Namespace);
  if (index < 0) {
    throw `Address space with name  '${objectmeta.Name}' in namespace ${objectmeta.Namespace} does not exist`;
  }
  var as = addressSpaces[index];
  delete addressspace_connection[as.ObjectMeta.Uid];

  addressSpaces.splice(index, 1);
}


var addressSpaces = [];

createAddressSpace(
    {
      ObjectMeta: {
        Name: "jupiter_as1",
        Namespace: availableNamespaces[0].ObjectMeta.Name,
      },
      Spec: {
        Plan: "standard-small",
        Type: "standard"
      }
    });

createAddressSpace(
    {
      ObjectMeta: {
        Name: "saturn_as2",
        Namespace: availableNamespaces[0].ObjectMeta.Name,
      },
      Spec: {
        Plan: "standard-medium",
        Type: "standard"
      },
      Status: {
        Phase: "Configuring"
      }
    });

createAddressSpace(
    {
      ObjectMeta: {
        Name: "mars_as2",
        Namespace: availableNamespaces[1].ObjectMeta.Name,
      },
      Spec: {
        Plan: "brokered-single-broker",
        Type: "brokered"
      }
    });


console.log("patch : %j", compare(addressSpaces[0], addressSpaces[1]));

var connections = [];

function createConnection(addressSpace, hostname) {
  var port = Math.floor(Math.random() * 25536) + 40000;
  var hostport = hostname + ":" + port;
  var encrypted = (port % 2 === 0);
  var properties = [];
  if (addressSpace.Spec.Type === "standard") {
    properties = [
      {
        "Key": "platform",
        "Value": "JVM: 1.8.0_191, 25.191-b12, Oracle Corporation, OS: Mac OS X, 10.13.6, x86_64"
      },
      {
        "Key": "product",
        "Value": "QpidJMS"
      },
      {
        "Key": "version",
        "Value": "0.38.0-SNAPSHOT"
      }
    ];

  }
  return {
    ObjectMeta: {
      Name: hostport,
      Uid: uuidv1() + "",
      Namespace: addressSpace.ObjectMeta.Namespace,
      CreationTimestamp: getRandomCreationDate(addressSpace.ObjectMeta.CreationTimestamp)
    },
    Spec: {
      AddressSpace: addressSpace.ObjectMeta.Name,
      Hostname: hostport,
      ContainerId: uuidv1() + "",
      Protocol: encrypted ? "amqps" : "amqp",
      Encrypted: encrypted,
      Properties: properties,
      Metrics: []
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
  addressspace_connection[as.ObjectMeta.Uid] = connections.filter((c) => c.Spec.AddressSpace === as.ObjectMeta.Name);
});

var addresses = [];

function createAddress(addr) {
  var namespace = availableNamespaces.find(n => n.ObjectMeta.Name === addr.ObjectMeta.Namespace);
  if (namespace === undefined) {
    var knownNamespaces = availableNamespaces.map(p => p.ObjectMeta.Name);
    throw `Unrecognised namespace '${addr.ObjectMeta.Namespace}', known ones are : ${knownNamespaces}`;
  }

  var addressSpacesInNamespace = addressSpaces.filter(as => as.ObjectMeta.Namespace === addr.ObjectMeta.Namespace);
  var addressSpace = addressSpacesInNamespace.find(as => as.ObjectMeta.Name === addr.Spec.AddressSpace);
  if (addressSpace === undefined) {
    var addressspacenames = addressSpacesInNamespace.map(p => p.ObjectMeta.Name);
    throw `Unrecognised address space '${addr.Spec.AddressSpace}', known ones are : ${addressspacenames}`;
  }

  var knownTypes = ['queue', 'topic', 'subscription', 'multicast', 'anycast'];
  if (knownTypes.find(t => t === addr.Spec.Type) === undefined) {
    throw `Unrecognised address type '${addr.Spec.Type}', known ones are : '${knownTypes}'`;
  }

  var plan = availableAddressPlans.find(p => p.ObjectMeta.Name === addr.Spec.Plan && addr.Spec.Type === p.Spec.AddressType);
  if (plan === undefined) {
    var knownPlansNames = availableAddressPlans.filter(p => addr.Spec.Type === p.Spec.AddressType).map(p => p.ObjectMeta.Name);
    throw `Unrecognised address plan '${addr.Spec.Plan}', known plans for type '${addr.Spec.Type}' are : ${knownPlansNames}`;
  }


  var prefix = addr.Spec.AddressSpace + ".";
  if (addr.Spec.Type === 'subscription') {
      var topics  = addresses.filter(a => a.ObjectMeta.Name.startsWith(prefix) && a.Spec.Type === "topic");
      if (!addr.Spec.Topic) {
        throw `Spec.Topic is mandatory for the subscription type`;
      } else if (topics.find(t => t.ObjectMeta.Name === addr.Spec.Topic) === undefined) {
        var topicNames  = topics.map(t => t.ObjectMeta.Name);
        throw `Unrecognised topic name '${addr.Spec.Topic}', known ones are : '${topicNames}'`;
      }
  } else {
      if (addr.Spec.Topic) {
        throw `Spec.Topic is not allowed for the address type '${addr.Spec.Type}'.`;
      }
  }

  var prefix = addr.Spec.AddressSpace + ".";
  if (!addr.ObjectMeta.Name.startsWith(prefix)) {
    throw `Address name must begin with '${prefix}`;
  }

  if (addresses.find(existing => addr.ObjectMeta.Name === existing.ObjectMeta.Name && addr.ObjectMeta.Namespace === existing.ObjectMeta.Namespace) !== undefined) {
    throw `Address with name  '${addr.ObjectMeta.Name} already exists in address space ${addr.Spec.AddressSpace}`;
  }

  var phase = "Active";
  var messages = [];
  if (addr.Status && addr.Status.Phase) {
    phase = addr.Status.Phase
  }
  if (phase !== "Active") {
    messages.push("Address " + addr.ObjectMeta.Name + " not found on qdrouterd")
  }
  var address = {
    ObjectMeta: {
      Name: addr.ObjectMeta.Name,
      Namespace: addr.ObjectMeta.Namespace,
      Uid: uuidv1(),
      CreationTimestamp: addr.ObjectMeta.CreationTimestamp ? addr.ObjectMeta.CreationTimestamp : getRandomCreationDate()
    },
    Spec: {
      Address: addr.Spec.Address,
      AddressSpace: addr.Spec.AddressSpace,
      Plan: plan,
      Type: addr.Spec.Type,
      Topic: addr.Spec.Topic
    },
    Status: {
      IsReady: "Active" === phase,
      Messages: messages,
      Phase: phase,
      PlanStatus: {
        Name: plan.ObjectMeta.Name,
        Partitions: 1
      }
    }
  };
  addresses.push(address);
  return address.ObjectMeta;
}

function patchAddress(objectmeta, jsonPatch, patchType) {
  var index = addresses.findIndex(existing => objectmeta.Name === existing.ObjectMeta.Name && objectmeta.Namespace === existing.ObjectMeta.Namespace);
  if (index < 0) {
    throw `Address with name  '${objectmeta.Name}' in namespace ${objectmeta.Namespace} does not exist`;
  }

  var knownPatchTypes = ["application/json-patch+json", "application/merge-patch+json", "application/strategic-merge-patch+json"];
  if (knownPatchTypes.find(p => p === patchType) === undefined) {
    throw `Unsupported patch type '$patchType'`
  } else if ( patchType !== 'application/json-patch+json') {
    throw `Unsupported patch type '$patchType', this mock currently supports only 'application/json-patch+json'`;
  }

  var patch = JSON.parse(jsonPatch);
  var current = addresses[index].Spec;
  var patched = applyPatch(JSON.parse(JSON.stringify(current)) , patch);
  if (patched.newDocument) {
    var replacement = patched.newDocument;
    if (replacement.Plan !== current.Plan) {
      var replacementPlan = typeof(replacement.Plan) === "string" ? replacement.Plan : replacement.Plan.ObjectMeta.Name;
      var spacePlan = availableAddressPlans.find(o => o.ObjectMeta.Name === replacementPlan);
      if (spacePlan === undefined) {
        var knownPlansNames = availableAddressPlans.map(p => p.ObjectMeta.Name);
        throw `Unrecognised address plan '${replacement.Spec.Plan}', known ones are : ${knownPlansNames}`;
      }
      replacement.Plan = spacePlan;
    }

    addresses[index].Spec = replacement;
    return true;
  } else {
    throw `Failed to patch address with name  '${objectmeta.Name}' in namespace ${objectmeta.Namespace}`
  }
}

function deleteAddress(metadata) {
  var index = addresses.findIndex(existing => metadata.Name === existing.ObjectMeta.Name && metadata.Namespace === existing.ObjectMeta.Namespace);
  if (index < 0) {
    throw `Address with name  '${metadata.Name}' in namespace ${metadata.Namespace} does not exist`;
  }
  addresses.splice(index, 1);
}

function purgeAddress(objectmeta) {
  var index = addresses.findIndex(existing => objectmeta.Name === existing.ObjectMeta.Name && objectmeta.Namespace === existing.ObjectMeta.Namespace);
  if (index < 0) {
    throw `Address with name  '${objectmeta.Name}' in namespace ${objectmeta.Namespace} does not exist`;
  }
}

function closeConnection(objectmeta) {

  var index = connections.findIndex(existing => objectmeta.Name === existing.ObjectMeta.Name && objectmeta.Namespace === existing.ObjectMeta.Namespace);
  if (index < 0) {
    var knownCons = connections.filter(c => c.ObjectMeta.Namespace === objectmeta.Namespace).map(c => c.ObjectMeta.Name);
    throw `Connection with name  '${objectmeta.Name}' in namespace ${objectmeta.Namespace} does not exist. Known connection names are: ${knownCons}`;
  }
  var targetCon = connections[index];

  var addressSpaceName = connections[index].Spec.AddressSpace;
  var as = addressSpaces.find(as => as.ObjectMeta.Name === addressSpaceName);

  var as_cons = addressspace_connection[as.ObjectMeta.Uid];
  var as_cons_index = as_cons.findIndex((c) => c === targetCon);
  as_cons.splice(as_cons_index, 1);

  connections.splice(index, 1);

}

["ganymede", "callisto", "io", "europa", "amalthea", "himalia", "thebe", "elara", "pasiphae", "metis", "carme", "sinope"].map(n =>
    (createAddress({
      ObjectMeta: {
        Name: addressSpaces[0].ObjectMeta.Name + "." + n,
        Namespace: addressSpaces[0].ObjectMeta.Namespace
      },
      Spec: {
        Address: n,
        AddressSpace: addressSpaces[0].ObjectMeta.Name,
        Plan: "standard-small-queue",
        Type: "queue"
      },
      Status: {
        Phase: n.startsWith("c") ? "Configuring" : (n.startsWith("p") ? "Pending" : "Active")
      }
    })));

function createTopicWithSub(addressSpace, n) {
    createAddress({
      ObjectMeta: {
        Name: addressSpace.ObjectMeta.Name + "." + n,
        Namespace: addressSpace.ObjectMeta.Namespace
      },
      Spec: {
        Address: n,
        AddressSpace: addressSpace.ObjectMeta.Name,
        Plan: "standard-small-topic",
        Type: "topic"
      },
      Status: {
        Phase: n.startsWith("c") ? "Configuring" : (n.startsWith("p") ? "Pending" : "Active")
      }
    });
    var subname = n + '-sub';
    createAddress({
      ObjectMeta: {
        Name: addressSpaces[0].ObjectMeta.Name + "." + subname,
        Namespace: addressSpace.ObjectMeta.Namespace
      },
      Spec: {
        Address: subname,
        AddressSpace: addressSpace.ObjectMeta.Name,
        Plan: "standard-small-subscription",
        Type: "subscription",
        Topic: addressSpace.ObjectMeta.Name + "." + n
      },
      Status: {
        Phase: n.startsWith("c") ? "Configuring" : (n.startsWith("p") ? "Pending" : "Active")
      }
    });
}

// Topic with a subscription
["themisto"].map(n => (createTopicWithSub(addressSpaces[0], n)));


["titan", "rhea", "iapetus", "dione", "tethys", "enceladus", "mimas"].map(n =>
    (createAddress({
      ObjectMeta: {
        Name: addressSpaces[1].ObjectMeta.Name + "." + n,
        Namespace: addressSpaces[1].ObjectMeta.Namespace
      },
      Spec: {
        Address: n,
        AddressSpace: addressSpaces[1].ObjectMeta.Name,
        Plan: "standard-small-queue",
        Type: "queue"
      }
    })));

["phobos", "deimous"].map(n =>
    (createAddress({
      ObjectMeta: {
        Name: addressSpaces[2].ObjectMeta.Name + "." + n,
        Namespace: addressSpaces[2].ObjectMeta.Namespace
      },
      Spec: {
        Address: n,
        AddressSpace: addressSpaces[2].ObjectMeta.Name,
        Plan: "brokered-queue",
        Type: "queue"
      }
    })));

function* makeAddrIter(namespace, addressspace) {
  var filter = addresses.filter(a => a.ObjectMeta.Namespace === namespace && a.ObjectMeta.Name.startsWith(addressspace + "."));
  var i = 0;
  while(filter.length) {
    var addr = filter[i++ % filter.length];
    yield addr;
  }
}

var addressItrs = {};
addressSpaces.forEach((as) => {
  addressItrs[as.ObjectMeta.Uid] = makeAddrIter(as.ObjectMeta.Namespace, as.ObjectMeta.Name);
});

var links = [];
connections.forEach(c => {
  var addressSpaceName = c.Spec.AddressSpace;
  var addressSpace = addressSpaces.find(as => as.ObjectMeta.Name === addressSpaceName);
  var uid = addressSpace.ObjectMeta.Uid;
  var addr = addressItrs[uid].next().value;

  for (var i = 0; i< addr.ObjectMeta.Name.length; i++) {
    links.push(
        {
          ObjectMeta: {
            Name: uuidv1(),
          },
          Spec: {
            Connection: c,
            Address: addr.ObjectMeta.Name,
            Role: i % 2 === 0 ? "sender" : "receiver",
          }
        });
  }
});

function buildFilterer(filter) {
  return filter ? parser.parse(filter) : {evaluate: () => true};
}


function init(input) {
  if (input.ObjectMeta) {
    input.ObjectMeta.CreationTimestamp = new Date();
  }
  return input;
}

function makeMockAddressMetrics() {
  return [
    {
      Name: "enmasse_messages_stored",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "messages"
    },
    {
      Name: "enmasse_senders",
      Type: "gauge",
      Value: Math.floor(Math.random() * 3),
      Units: "links"
    },
    {
      Name: "enmasse_receivers",
      Type: "gauge",
      Value: Math.floor(Math.random() * 3),
      Units: "links"
    },
    {
      Name: "enmasse_messages_in",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "msg/s"
    },
    {
      Name: "enmasse_messages_out",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "msg/s"
    },

  ];
}

function makeMockConnectionMetrics() {
  return [
    {
      Name: "enmasse_messages_in",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "msg/s"
    },
    {
      Name: "enmasse_messages_out",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "msg/s"
    },
    {
      Name: "enmasse_senders",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "total"
    },
    {
      Name: "enmasse_receivers",
      Type: "gauge",
      Value: Math.floor(Math.random() * 10),
      Units: "total"
    },
  ];
}

function makeMockLinkMetrics(is_addr_query, link) {
  if (is_addr_query) {

    return [
      {
        Name: link.Spec.Role === "sender" ? "enmasse_messages_in" : "enmasse_messages_out",
        Type: "gauge",
        Value: Math.floor(Math.random() * 10),
        Units: "msg/s"
      },
      {
        Name: "enmasse_messages_backlog",
        Type: "gauge",
        Value: Math.floor(Math.random() * 15),
        Units: "msg"
      },
    ];
  } else {

    var addressSpaceName = link.Spec.Connection.Spec.AddressSpace;
    var as = addressSpaces.find(as => as.ObjectMeta.Name === addressSpaceName);
    if (as.Spec.Type === "brokered") {
      return [
        {
          Name: "enmasse_deliveries",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        }
      ];
    } else {
      return [
        {
          Name: "enmasse_deliveries",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        },
        {
          Name: "enmasse_rejected",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        },
        {
          Name: "enmasse_released",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        },
        {
          Name: "enmasse_modified",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        },
        {
          Name: "enmasse_presettled",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        },
        {
          Name: "enmasse_undelivered",
          Type: "counter",
          Value: Math.floor(Math.random() * 10),
          Units: "deliveries"
        },
      ];

    }
  }
}

function makeAddressSpaceMetrics(as) {
  var cons = as.ObjectMeta.Uid in addressspace_connection ? addressspace_connection[as.ObjectMeta.Uid] : [];
  var addrs = addresses.filter((a) => as.ObjectMeta.Namespace === a.ObjectMeta.Namespace &&
      a.ObjectMeta.Name.startsWith(as.ObjectMeta.Name + "."));

  return [
    {
      Name: "enmasse_connections",
      Type: "gauge",
      Value: cons.length,
      Units: "connections"
    },
    {
      Name: "enmasse_addresses",
      Type: "gauge",
      Value: addrs.length,
      Units: "addresses"
    },
  ];
}

// A map of functions which return data for the schema.
const resolvers = {
  Mutation: {
    createAddressSpace: (parent, args) => {
      return createAddressSpace(init(args.input));
    },
    patchAddressSpace: (parent, args) => {
      return patchAddressSpace(args.input, args.jsonPatch, args.patchType);
    },
    deleteAddressSpace: (parent, args) => {
      deleteAddressSpace(args.input);
      return true;
    },
    createAddress: (parent, args) => {
      return createAddress(init(args.input));
    },
    patchAddress: (parent, args) => {
      return patchAddress(args.input, args.jsonPatch, args.patchType);
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
      return `apiVersion: enmasse.io/v1beta1
oc apply -f - << EOF
kind: AddressSpace
ObjectMeta:
  name: ${args.input.ObjectMeta.Name}
spec:
  type: ${args.input.Spec.Type}
  plan: ${args.input.Spec.Plan}
EOF
`;
    },

    addressCommand: (parent, args, context, info) => {
      return `apiVersion: enmasse.io/v1beta1
oc apply -f - << EOF
kind: Address
ObjectMeta:
  name: ${args.input.ObjectMeta.Name}
spec:
  address: ${args.input.Spec.Address}
  type: ${args.input.Spec.Type}
  plan: ${args.input.Spec.Plan}
EOF
`;
    },

    namespaces: () => availableNamespaces,

    authenticationServices: () => availableAuthenticationServices,
    addressSpaceSchema: () => availableAddressSpaceSchemas,
    addressSpaceSchema_v2: (parent, args, context, info) => {
      return availableAddressSpaceSchemas.filter(
        o =>
          args.addressSpaceType === undefined ||
          o.ObjectMeta.Name === args.addressSpaceType
      );
    },

    addressTypes: () => (['queue', 'topic', 'subscription', 'multicast', 'anycast']),
    addressTypes_v2: (parent, args, context, info) => {
      return availableAddressTypes
          .filter(o => args.addressSpaceType === undefined || o.Spec.AddressSpaceType === args.addressSpaceType)
          .sort(o => o.Spec.DisplayOrder);
    },
    addressSpaceTypes: () => (['standard', 'brokered']),
    addressSpaceTypes_v2: (parent, args, context, info) => {
      return availableAddressSpaceTypes
          .sort(o => o.Spec.DisplayOrder);
    },
    addressSpacePlans: (parent, args, context, info) => {
      return availableAddressSpacePlans
          .filter(o => args.addressSpaceType === undefined || o.Spec.AddressSpaceType === args.addressSpaceType)
          .sort(o => o.Spec.DisplayOrder);
    },
    addressPlans: (parent, args, context, info) => {
      var plans = availableAddressPlans;
      if (args.addressSpacePlan) {
        var spacePlan = availableAddressSpacePlans.find(o => o.ObjectMeta.Name === args.addressSpacePlan);
        if (spacePlan === undefined) {
          var knownPlansNames = availableAddressSpacePlans.map(p => p.ObjectMeta.Name);
          throw `Unrecognised address space plan '${args.addressSpacePlan}', known ones are : ${knownPlansNames}`;
        }
        plans = spacePlan.Spec.AddressPlans;
      }

      return plans.filter(p => (args.addressType === undefined || p.Spec.AddressType === args.addressType)).sort(o => o.Spec.DisplayOrder);
    },
    addressSpaces:(parent, args, context, info) => {

      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var copy = clone(addressSpaces);
      copy.forEach(as => {
        as.Metrics = makeAddressSpaceMetrics(as);
      });
      var as = copy.filter(as => filterer.evaluate(as)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, as.length);
      var page = as.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: as.length,
        AddressSpaces: page
      };
    },
    addresses:(parent, args, context, info) => {

      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);


      var copy = clone(addresses);
      copy.forEach(a => {
        a.Metrics = makeMockAddressMetrics();
      });

      var a = copy.filter(a => filterer.evaluate(a)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, a.length);
      var page = a.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: a.length,
        Addresses: page
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
        Total: cons.length,
        Connections: page
      };

    }
  },

  AddressSpace_consoleapi_enmasse_io_v1beta1: {
    Connections:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var as = parent;
      var cons = as.ObjectMeta.Uid in addressspace_connection ? addressspace_connection[as.ObjectMeta.Uid] : [];
      var copy = clone(cons);
      copy.forEach(c => {
        c.Metrics = makeMockConnectionMetrics();
      });

      copy = copy.filter(c => filterer.evaluate(c)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, copy.length);
      var page = copy.slice(paginationBounds.lower, paginationBounds.upper);
      return {Total: copy.length, Connections: page};
    },
    Addresses:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var as = parent;

      var copy = clone(addresses.filter(a => as.ObjectMeta.Namespace === a.ObjectMeta.Namespace && a.ObjectMeta.Name.startsWith(as.ObjectMeta.Name + ".")));
      copy.forEach(a => {
        a.Metrics = makeMockAddressMetrics();
      });

      var addrs = copy.filter(a => filterer.evaluate(a)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, addrs.length);
      var page = addrs.slice(paginationBounds.lower, paginationBounds.upper);
      return {Total: addrs.length,
        Addresses: page};
    },
  },
  Address_consoleapi_enmasse_io_v1beta1: {
    Links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);


      var addr = parent;
      var copy = clone(links.filter((l) => l.Spec.Connection.ObjectMeta.Namespace === addr.ObjectMeta.Namespace && addr.ObjectMeta.Name.startsWith(l.Spec.Connection.Spec.AddressSpace + ".")));
      copy.forEach(l => {
        l.Metrics = makeMockLinkMetrics(true, l);
      });
      var addrlinks = copy.filter(l => filterer.evaluate(l)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, addrlinks.length);
      var page = addrlinks.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: addrlinks.length,
        Links: page
      };
    },
  },
  Connection_consoleapi_enmasse_io_v1beta1: {
    Links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = orderer(args.orderBy);

      var con = parent;
      var copy = clone(links.filter((l) => _.isEqual(l.Spec.Connection.ObjectMeta, con.ObjectMeta)));
      copy.forEach(l => {
        l.Metrics = makeMockLinkMetrics(false, l);
      });

      var connlinks = copy.filter(l => filterer.evaluate(l)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, connlinks.length);
      var page = connlinks.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: connlinks.length,
        Links: page
      };
    },
  },
  ConnectionSpec_consoleapi_enmasse_io_v1beta1: {
    AddressSpace: (parent, args, context, info) => {
      var as = addressSpaces.find(as => as.ObjectMeta.Name ===  parent.AddressSpace);
      return as
    },
  },

  Link_consoleapi_enmasse_io_v1beta1: {
  },
  ObjectMeta_v1 : {
    CreationTimestamp: (parent, args, context, info) => {
      var meta = parent;
      return meta.CreationTimestamp;
    }
  }
};

const mocks = {
  Int: () => 6,
  Float: () => 22.1,
  String: () => undefined,
  User_v1: () => ({
    ObjectMeta: {
      Name: "vtereshkova"
    },
    Identities: ["vtereshkova"],
    FullName: "Valentina Tereshkova",
    Groups: ["admin"]
  })
};


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
