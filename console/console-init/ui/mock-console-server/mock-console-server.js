
const uuidv1 = require('uuid/v1');
const traverse = require('traverse');
const fs = require('fs');
const path = require('path');
const { ApolloServer, gql } = require('apollo-server');
const typeDefs = require('./schema');
const { applyPatch, compare } = require('fast-json-patch');
const parser = require('./filter_parser.js');
const jp = require('jsonpath');
const firstBy = require('thenby');

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
      "shortDescription": shortDescription
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

  var spacePlan = availableAddressSpacePlans.find(o => o.ObjectMeta.Name === as.Spec.Plan);
  if (spacePlan === undefined) {
    var knownPlansNames = availableAddressSpacePlans.map(p => p.ObjectMeta.Name);
    throw `Unrecognised address space plan '${as.Spec.Plan}', known ones are : ${knownPlansNames}`;
  }
  if (as.Spec.Type !== 'brokered' && as.Spec.Type !== 'standard') {
    throw `Unrecognised address space type '${(as.Spec.Type)}', known ones are : brokered, standard`;
  }

  if (addressSpaces.find(existing => as.ObjectMeta.Name === existing.ObjectMeta.Name && as.ObjectMeta.Namespace === existing.ObjectMeta.Namespace) !== undefined) {
    throw `Address space with name  '${as.ObjectMeta.Name} already exists in namespace ${as.ObjectMeta.Namespace}`;
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
      "isReady": true,
      "messages": [],
      "phase": "Active"
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
  return {
    ObjectMeta: {
      Name: hostport,
      Uid: uuidv1() + "",
      Namespace: addressSpace.ObjectMeta.Namespace,
      CreationTimestamp: getRandomCreationDate(addressSpace.ObjectMeta.CreationTimestamp)
    },
    Spec: {
      AddressSpace: addressSpace,
      Hostname: hostport,
      ContainerId: uuidv1() + "",
      Protocol: "amqp",
      Properties: [
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
        ],
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
  addressspace_connection[as.ObjectMeta.Uid] = connections.filter((c) => c.Spec.AddressSpace.ObjectMeta.Uid === as.ObjectMeta.Uid);
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

  var plan = availableAddressPlans.find(o => o.ObjectMeta.Name === addr.Spec.Plan);
  if (plan === undefined) {
    var knownPlansNames = availableAddressPlans.map(p => p.ObjectMeta.Name);
    throw `Unrecognised address plan '${addr.Spec.Plan}', known ones are : ${knownPlansNames}`;
  }

  var knownTypes = ['queue', 'topic', 'subscription', 'multicast', 'anycast'];
  if (knownTypes.find(t => t === addr.Spec.Type) === undefined) {
    throw `Unrecognised address type '${addr.Spec.Type}', known ones are : '${knownTypes}'`;
  }

  var prefix = addr.Spec.AddressSpace + ".";
  if (!addr.ObjectMeta.Name.startsWith(prefix)) {
    throw `Address name must begin with '${prefix}`;
  }

  if (addresses.find(existing => addr.ObjectMeta.Name === existing.ObjectMeta.Name && addr.ObjectMeta.Namespace === existing.ObjectMeta.Namespace) !== undefined) {
    throw `Address with name  '${addr.ObjectMeta.Name} already exists in address space ${addr.Spec.AddressSpace}`;
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
      Type: addr.Spec.Type
    },
    Status: {
      Phase: "Active",
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

  var as = connections[index].Spec.AddressSpace;
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
      }
    })));


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
  var addr = addressItrs[c.Spec.AddressSpace.ObjectMeta.Uid].next().value;
  links.push(
      {
        ObjectMeta: {
          Name: uuidv1(),
        },
        Spec: {
          Connection: c,
          Address: addr.ObjectMeta.Name,
          Role: "sender",
        }
      });
});

function buildFilterer(filter) {
  return filter ? parser.parse(filter) : {evaluate: () => true};
}

function buildOrderBy(sort_spec) {
  if (sort_spec) {
    return (r1, r2)  => {
      var by = firstBy.firstBy((a, b) => 0);

      sort_spec.split(/\s*,\s*/).forEach(spec => {
        var match = /^`(.+)`\s*(asc|desc)?$/i.exec(spec);
        var compmul = match.length > 2 && match[2] && match[2].toLowerCase() === "desc" ? -1 : 1;

        var path = match[1];
        var result1 = jp.query(r1, path, 1);
        var result2 = jp.query(r2, path, 1);

        var value1 = result1.length ? result1[0] : undefined;
        var value2 = result2.length ? result2[0] : undefined;

        by = by.thenBy((a,b) => ( value1 < value2 ) ? -1 * compmul : ( value1 > value2 ? compmul : 0 ));
      });

      return by(r1, r2);
    };
  } else {
    return (r1, r2) => (a, b) => 0;
  }
}

function init(input) {
  if (input.ObjectMeta) {
    input.ObjectMeta.CreationTimestamp = new Date();
  }
  return input;
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

    addressTypes: () => (['queue', 'topic', 'subscription', 'multicast', 'anycast']),
    addressSpaceTypes: () => (['standard', 'brokered']),
    addressSpacePlans: (parent, args, context, info) => {
      return availableAddressSpacePlans
          .filter(o => args.addressSpaceType === undefined || o.Spec.AddressSpaceType === args.addressSpaceType)
          .sort(o => o.Spec.DisplayOrder);
    },
    addressPlans: (parent, args, context, info) => {
      if (args.addressSpacePlan === undefined) {
        return availableAddressPlans.sort(o => o.Spec.DisplayOrder);
      } else {
        var spacePlan = availableAddressSpacePlans.find(o => o.ObjectMeta.Name === args.addressSpacePlan);
        if (spacePlan === undefined) {
          var knownPlansNames = availableAddressSpacePlans.map(p => p.ObjectMeta.Name);
          throw `Unrecognised address space plan '${args.addressSpacePlan}', known ones are : ${knownPlansNames}`;
        }
        return spacePlan.Spec.AddressPlans.sort(o => o.Spec.DisplayOrder);
      }
    },
    addressSpaces:(parent, args, context, info) => {

      var filterer = buildFilterer(args.filter);
      var orderBy = buildOrderBy(args.orderBy);
      var as = addressSpaces.filter(as => filterer.evaluate(as)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, as.length);
      var page = as.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: as.length,
        AddressSpaces: page
      };
    },
    addresses:(parent, args, context, info) => {

      var filterer = buildFilterer(args.filter);
      var orderBy = buildOrderBy(args.orderBy);
      var a = addresses.filter(a => filterer.evaluate(a)).sort(orderBy);
      var paginationBounds = calcLowerUpper(args.offset, args.first, a.length);
      var page = a.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: a.length,
        Addresses: page
      };
    },
    connections:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = buildOrderBy(args.orderBy);
      var cons = connections.filter(c => filterer.evaluate(c)).sort(orderBy);

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
      var orderBy = buildOrderBy(args.orderBy);

      var as = parent;
      var cons = as.ObjectMeta.Uid in addressspace_connection ? addressspace_connection[as.ObjectMeta.Uid] : [];
      cons = cons.filter(c => filterer.evaluate(c)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, cons.length);
      var page = cons.slice(paginationBounds.lower, paginationBounds.upper);
      return {Total: cons.length, Connections: page};
    },
    Addresses:(parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = buildOrderBy(args.orderBy);

      var as = parent;

      var addrs = addresses.filter(
          a => as.ObjectMeta.Namespace === a.ObjectMeta.Namespace && a.ObjectMeta.Name.startsWith(as.ObjectMeta.Name + "."))
          .filter(a => filterer.evaluate(a)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, addrs.length);
      var page = addrs.slice(paginationBounds.lower, paginationBounds.upper);
      return {Total: addrs.length,
        Addresses: page};
    },
    Metrics: (parent, args, context, info) => {
      var as = parent;
      var cons = as.ObjectMeta.Uid in addressspace_connection ? addressspace_connection[as.ObjectMeta.Uid] : [];
      var addrs = addresses.filter((a) => as.ObjectMeta.Namespace === a.ObjectMeta.Namespace &&
                                          a.ObjectMeta.Name.startsWith(as.ObjectMeta.Name + "."));

      return [
        {
          Name: "enmasse-connections",
          Type: "gauge",
          Value: cons.length,
          Units: "connections"
        },
        {
          Name: "enmasse-addresses",
          Type: "gauge",
          Value: addrs.length,
          Units: "addresses"
        },
      ];
    }

  },
  Address_consoleapi_enmasse_io_v1beta1: {
    Metrics: (parent, args, context, info) => {
      var as = parent;
      var cons = as.ObjectMeta.Uid in addressspace_connection ? addressspace_connection[as.ObjectMeta.Uid] : [];
      var addrs = addresses.filter((a) => as.ObjectMeta.Namespace === a.ObjectMeta.Namespace &&
                                          a.ObjectMeta.Name.startsWith(as.ObjectMeta.Name + "."));

      return [
        {
          Name: "enmasse_messages_stored",
          Type: "gauge",
          Value: Math.floor(Math.random() * 10),
          Units: "messages"
        },
        {
          Name: "enmasse-senders",
          Type: "gauge",
          Value: Math.floor(Math.random() * 3),
          Units: "links"
        },
        {
          Name: "enmasse-receivers",
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
    },
    Links: (parent, args, context, info) => {
      var filterer = buildFilterer(args.filter);
      var orderBy = buildOrderBy(args.orderBy);

      var addr = parent;
      var addrlinks = links.filter((l) => l.Spec.Connection.Spec.AddressSpace.ObjectMeta.Namespace === addr.ObjectMeta.Namespace &&   addr.ObjectMeta.Name.startsWith(l.Spec.Connection.Spec.AddressSpace.ObjectMeta.Name + "."))
          .filter(l => filterer.evaluate(l)).sort(orderBy);

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
      var orderBy = buildOrderBy(args.orderBy);

      var con = parent;
      var connlinks = links.filter((l) => l.Spec.Connection === con).filter(l => filterer.evaluate(l)).sort(orderBy);

      var paginationBounds = calcLowerUpper(args.offset, args.first, connlinks.length);
      var page = connlinks.slice(paginationBounds.lower, paginationBounds.upper);

      return {
        Total: connlinks.length,
        Links: page
      };
    },
    Metrics: (parent, args, context, info) => {
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
  },
  Link_consoleapi_enmasse_io_v1beta1: {
    Metrics: (parent, args, context, info) => {
      var nodes = traverse(info.path).nodes().filter(n => typeof(n) === "object");
      var is_addr_query = nodes.find(n =>  "key" in n && n["key"] === "Addresses") !== undefined;

      var link = parent;
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

        var as = link.Spec.Connection.Spec.AddressSpace;
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
  String: () => 'Hello',
  User_v1: () => ({
    Identities: ['fred'],
    Groups: ['admin']
  }),
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
