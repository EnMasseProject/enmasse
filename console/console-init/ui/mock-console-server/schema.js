/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const { gql } = require("apollo-server");

const typeDefs = gql`
  scalar Date

  type KeyValue {
    key: String!
    value: String!
  }

  enum AddressType {
    queue
    topic
    subscription
    multicast
    anycast
    deadLetter
  }

  enum AuthenticationServiceType {
    none
    standard
  }

  enum LinkRole {
    sender
    receiver
  }

  enum MetricType {
    gauge
    counter
  }

  enum Protocol {
    amqp
    amqps
  }

  enum MessagingEndpointType_enmasse_io_v1 {
    Cluster
    NodePort
    LoadBalancer
    Route
    Ingress
  }

  enum MessagingEndpointProtocol_enmasse_io_v1 {
    AMQP
    AMQPS
    AMQP_WS
    AMQP_WSS
  }

  enum MessagingEndpointPhase_enmasse_io_v1 {
    Configuring
    Active
    Terminating
  }

  type Metric_consoleapi_enmasse_io_v1 {
    name: String!
    type: MetricType!
    value: Float!
    units: String!
  }

  type Connection_consoleapi_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
    spec: ConnectionSpec_consoleapi_enmasse_io_v1!

    metrics: [Metric_consoleapi_enmasse_io_v1!]!
    links(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): LinkQueryResult_consoleapi_enmasse_io_v1!
  }

  type ConnectionSpec_consoleapi_enmasse_io_v1 {
    namespace: String!
    hostname: String!
    containerId: String!
    protocol: Protocol!
    encrypted: Boolean!
    properties: [KeyValue!]!
    principal: String!
    messagingProject(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): MessagingProjectQueryResult_consoleapi_enmasse_io_v1
  }

  type Link_consoleapi_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
    spec: LinkSpec_consoleapi_enmasse_io_v1!
    metrics: [Metric_consoleapi_enmasse_io_v1!]!
  }

  type LinkSpec_consoleapi_enmasse_io_v1 {
    connection: Connection_consoleapi_enmasse_io_v1!
    address: String!
    role: LinkRole!
  }

  #
  #  Types used to facilitate the paginated model queries
  #

  type MessagingProjectQueryResult_consoleapi_enmasse_io_v1 {
    total: Int!
    messagingProjects: [MessagingProject_consoleapi_enmasse_io_v1!]!
  }

  type AddressQueryResult_consoleapi_enmasse_io_v1 {
    total: Int!
    addresses: [Address_consoleapi_enmasse_io_v1!]!
  }

  type ConnectionQueryResult_consoleapi_enmasse_io_v1 {
    total: Int!
    connections: [Connection_consoleapi_enmasse_io_v1!]!
  }

  type LinkQueryResult_consoleapi_enmasse_io_v1 {
    total: Int!
    links: [Link_consoleapi_enmasse_io_v1!]!
  }

  type MessagingEndpointQueryResult_consoleapi_enmasse_io_v1 {
    total: Int!
    messagingEndpoints: [MessagingEndpoint_enmasse_io_v1!]!
  }
  #
  # Mirrors of Kubernetes types.  These follow the names and structure of the underlying
  # Kubernetes object exactly.  We don't need to expose every field, just the ones that
  # are important to the GraphQL interface.
  #
  # It is also possible to map types into GraphQL types (enums, other types etc) as is
  # done below for the address.spec.plan and type fields.
  #

  type MessagingProject_consoleapi_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
    spec: MessagingProjectSpec_enmasse_io_v1!
    status: MessagingProjectStatus_enmasse_io_v1
    connections(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): ConnectionQueryResult_consoleapi_enmasse_io_v1!
    addresses(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): AddressQueryResult_consoleapi_enmasse_io_v1!
    metrics: [Metric_consoleapi_enmasse_io_v1!]
  }

  type MessagingPlan_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
  }

  type MessagingAddressPlan_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
  }

  type MessagingProjectSpec_enmasse_io_v1 {
    capabilities: [MessagingProjectCapability]
  }

  type MessagingEndpoint_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
    spec: MessagingEndpointSpec_enmasse_io_v1!
    status: MessagingEndpointStatus_enmasse_io_v1
  }

  type MessagingEndpointSpec_enmasse_io_v1 {
    protocols: [MessagingEndpointProtocol_enmasse_io_v1!]!
  }

  type MessagingEndpointStatus_enmasse_io_v1 {
    phase: MessagingEndpointPhase_enmasse_io_v1!
    type: MessagingEndpointType_enmasse_io_v1!
    message: String
    host: String

    ports: [MessagingEndpointPort_enmasse_io_v1!]!
    internalPorts: [MessagingEndpointPort_enmasse_io_v1!]!
  }

  type MessagingEndpointPort_enmasse_io_v1 {
    name: String!
    protocol: MessagingEndpointProtocol_enmasse_io_v1!
    port: Int!
  }

  type MessagingProjectStatus_enmasse_io_v1 {
    message: String!
    phase: String!
  }

  type AddressSpec_enmasse_io_v1 {
    address: String
  }

  type AddressStatus_enmasse_io_v1 {
    message: String
    phase: String!
  }

  type Address_consoleapi_enmasse_io_v1 {
    metadata: ObjectMeta_v1!
    spec: AddressSpec_enmasse_io_v1!
    status: AddressStatus_enmasse_io_v1

    links(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): LinkQueryResult_consoleapi_enmasse_io_v1!
    metrics: [Metric_consoleapi_enmasse_io_v1!]
  }

  type Metadata_consoleapi_enmasse_io_v1 {
    annotations: [KeyValue!]!
    name: String!
    namespace: String!
    resourceVersion: String!
    creationTimestamp: String!
    uid: ID!
  }

  type ObjectMeta_v1 {
    annotations: [KeyValue!]!
    name: String!
    namespace: String!
    resourceVersion: String!
    creationTimestamp: String!
    uid: ID!
  }

  type User_v1 {
    metadata: ObjectMeta_v1!
    identities: [String!]!
    groups: [String!]!
    fullName: String!
  }

  type Namespace_v1 {
    metadata: ObjectMeta_v1!
    status: NamespaceStatus_v1!
  }

  type NamespaceStatus_v1 {
    phase: String!
  }

  type Query {
    "Returns the address types defined by the system"
    addressTypes: [AddressType!]!

    # "Returns the address spaces plans defined by the system optionally filtering for plans available for a given namespace"
    messagingPlans(namespace: String): [MessagingPlan_enmasse_io_v1!]!

    # "Returns the address plans defined by the system optionally filtering those for a matching namespaec"
    messagingAddressPlans(
      namespace: String
    ): [MessagingAddressPlan_enmasse_io_v1!]!

    "Returns the current logged on user"
    whoami: User_v1!
    "Returns the namespaces visible to this user"
    namespaces: [Namespace_v1!]!

    "Returns the messaging projects visible to this user,  optionally filtering"
    messagingProjects(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): MessagingProjectQueryResult_consoleapi_enmasse_io_v1

    "Returns the addresses visible to this user,  optionally filtering"
    addresses(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): AddressQueryResult_consoleapi_enmasse_io_v1

    "Returns the connections visible to this user,  optionally filtering"
    connections(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): ConnectionQueryResult_consoleapi_enmasse_io_v1

    "Returns the messaging certificate chain for the address space identifed by input, PEM format, suitable to be offered as a download to the user"
    messagingCertificateChain(input: ObjectMeta_v1_Input!): String!

    "Returns the command-line that, if executed, would create the given address space"
    messagingProjectCommand(
      input: MessagingProject_enmasse_io_v1_Input!
    ): String!

    "Returns the command-line command, if executed, would create the given address."
    addressCommand(input: Address_enmasse_io_v1_Input!): String!

    "Returns the messaging endpoints for the given address space"
    messagingEndpoints(
      first: Int
      offset: Int
      filter: String
      orderBy: String
    ): MessagingEndpointQueryResult_consoleapi_enmasse_io_v1
  }

  #
  # Inputs Types
  #

  input ObjectMeta_v1_Input {
    name: String
    namespace: String!
    resourceVersion: String
  }

  input MessagingProjectSpec_enmasse_io_v1_Input {
    capabilities: [MessagingProjectCapability]
  }

  enum MessagingProjectCapability {
    transactional
  }

  input MessagingProject_enmasse_io_v1_Input {
    metadata: ObjectMeta_v1_Input
    spec: MessagingProjectSpec_enmasse_io_v1_Input
  }

  input AddressSpec_enmasse_io_v1_Input {
    address: String
    type: AddressType
  }

  input Address_enmasse_io_v1_Input {
    metadata: ObjectMeta_v1_Input
    spec: AddressSpec_enmasse_io_v1_Input
  }

  type Mutation {
    createMessagingProject(
      input: MessagingProject_enmasse_io_v1_Input!
    ): ObjectMeta_v1!
    patchMessagingProject(
      input: ObjectMeta_v1_Input!
      jsonPatch: String!
      patchType: String!
    ): Boolean
    deleteMessagingProject(input: ObjectMeta_v1_Input!): Boolean @deprecated
    "deletes messagingprojects (s)"
    deleteMessagingProjects(input: [ObjectMeta_v1_Input!]!): Boolean

    createAddress(
      input: Address_enmasse_io_v1_Input!
      addressSpace: String
    ): ObjectMeta_v1!
    patchAddress(
      input: ObjectMeta_v1_Input!
      jsonPatch: String!
      patchType: String!
    ): Boolean
    deleteAddress(input: ObjectMeta_v1_Input!): Boolean @deprecated
    "deletes addresss(es)"
    deleteAddresses(input: [ObjectMeta_v1_Input!]!): Boolean
    "purges address(es)"
    purgeAddresses(input: [ObjectMeta_v1_Input!]!): Boolean

    closeConnections(input: [ObjectMeta_v1_Input!]!): Boolean
  }
`;

module.exports = typeDefs;
