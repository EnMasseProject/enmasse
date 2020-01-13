const { gql } = require('apollo-server');

const typeDefs = gql`
    scalar Date

    type KeyValue {
        Key: String!
        Value: String!
    }

    enum AddressSpaceType {
        standard
        brokered
    }

    enum AddressType {
        queue
        topic
        subscription
        multicast
        anycast
    }

    enum AuthenticationServiceType {
        none
        standard
    }

    enum LinkRole {
        sender,
        receiver
    }

    enum MetricType {
        gauge
        counter
    }

    enum Protocol {
        amqp,
        amqps
    }

    type Metric_consoleapi_enmasse_io_v1beta1 {
        Name: String!
        Type: MetricType!
        Value: Float!
        Units: String!
    }

    type AddressSpaceType_consoleapi_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressTypeSpec_consoleapi_enmasse_io_v1beta1!
    }

    type AddressSpaceTypeSpec_consoleapi_enmasse_io_v1beta1 {
        AddressSpaceType: AddressSpaceType!
        DisplayName: String!
        LongDescription: String!
        ShortDescription: String!
        DisplayOrder: Int!
    }

    type AddressType_consoleapi_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressTypeSpec_consoleapi_enmasse_io_v1beta1!
    }

    type AddressTypeSpec_consoleapi_enmasse_io_v1beta1 {
        AddressSpaceType: AddressSpaceType!
        DisplayName: String!
        LongDescription: String!
        ShortDescription: String!
        DisplayOrder: Int!
    }

    type AuthenticationService_admin_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AuthenticationServiceSpec_admin_enmasse_io_v1beta1!
        Status: AuthenticationServiceStatus_admin_enmasse_io_v1beta1!
    }
    type AuthenticationServiceStatus_admin_enmasse_io_v1beta1 {
        host: String!
        port: Int!
    }
    type AuthenticationServiceSpec_admin_enmasse_io_v1beta1 {
        Type: AuthenticationServiceType!
    }
    type AddressSpaceSchema_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressSpaceSchemaSpec_enmasse_io_v1beta1!
    }
    type AddressSpaceSchemaSpec_enmasse_io_v1beta1 {
        AuthenticationServices: [String!]
        Description: String
    }

    type Connection_consoleapi_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: ConnectionSpec_consoleapi_enmasse_io_v1beta1!

        Metrics: [Metric_consoleapi_enmasse_io_v1beta1!]!,
        Links(first: Int, offset: Int, filter: String, orderBy: String): LinkQueryResult_consoleapi_enmasse_io_v1beta1!
    }

    type ConnectionSpec_consoleapi_enmasse_io_v1beta1 {
        AddressSpace: AddressSpace_consoleapi_enmasse_io_v1beta1!
        Hostname: String!
        ContainerId: String!
        Protocol: Protocol!
        Encrypted: Boolean!
        Properties: [KeyValue!]!
    }

    type Link_consoleapi_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: LinkSpec_consoleapi_enmasse_io_v1beta1!
        Metrics: [Metric_consoleapi_enmasse_io_v1beta1!]!,
    }

    type LinkSpec_consoleapi_enmasse_io_v1beta1 {
        Connection: Connection_consoleapi_enmasse_io_v1beta1!
        Address: String!
        Role: LinkRole!
        Metrics: [Metric_consoleapi_enmasse_io_v1beta1!]!
    }

    #
    #  Types used to facilitate the paginated model queries
    #

    type AddressSpaceQueryResult_consoleapi_enmasse_io_v1beta1 {
        Total: Int!
        AddressSpaces: [AddressSpace_consoleapi_enmasse_io_v1beta1!]!
    }

    type AddressQueryResult_consoleapi_enmasse_io_v1beta1 {
        Total: Int!
        Addresses: [Address_consoleapi_enmasse_io_v1beta1!]!
    }

    type ConnectionQueryResult_consoleapi_enmasse_io_v1beta1 {
        Total: Int!
        Connections: [Connection_consoleapi_enmasse_io_v1beta1!]!
    }

    type LinkQueryResult_consoleapi_enmasse_io_v1beta1 {
        Total: Int!
        Links: [Link_consoleapi_enmasse_io_v1beta1!]!
    }

    #
    # Mirrors of Kubernetes types.  These follow the names and structure of the underlying
    # Kubernetes object exactly.  We don't need to expose every field, just the ones that
    # are important to the GraphQL interface.
    #
    # It is also possible to map types into GraphQL types (enums, other types etc) as is
    # done below for the address.spec.plan and type fields.
    #

    type AddressSpace_consoleapi_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressSpaceSpec_enmasse_io_v1beta1!
        Status: AddressSpaceStatus_enmasse_io_v1beta1
        Connections(first: Int, offset: Int, filter: String, orderBy: String): ConnectionQueryResult_consoleapi_enmasse_io_v1beta1!
        Addresses(first: Int, offset: Int, filter: String, orderBy: String): AddressQueryResult_consoleapi_enmasse_io_v1beta1!
        Metrics: [Metric_consoleapi_enmasse_io_v1beta1!]
    }

    type AddressSpaceSpec_enmasse_io_v1beta1 {
        Plan:      AddressSpacePlan_admin_enmasse_io_v1beta2!
        Type:      AddressSpaceType!
    }

    type AddressSpaceStatus_enmasse_io_v1beta1 {
        IsReady: Boolean!
        Messages: [String!]
        Phase: String!
    }

    type AddressSpec_enmasse_io_v1beta1 {
        Address:      String!
        AddressSpace: String!
        Type:         AddressType!
        Plan:         AddressPlan_admin_enmasse_io_v1beta2!
        Topic:        String
    }

    type AddressStatus_enmasse_io_v1beta1 {
        IsReady: Boolean!
        Messages: [String!]
        Phase: String!
        PlanStatus: AddressPlanStatus_enmasse_io_v1beta1
    }

    type AddressPlanStatus_enmasse_io_v1beta1 {
        Name: String!
        Partitions: Int!
    }

    type Address_consoleapi_enmasse_io_v1beta1 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressSpec_enmasse_io_v1beta1!
        Status: AddressStatus_enmasse_io_v1beta1

        Links(first: Int, offset: Int, filter: String, orderBy: String ): LinkQueryResult_consoleapi_enmasse_io_v1beta1!
        Metrics: [Metric_consoleapi_enmasse_io_v1beta1!]
    }

    type AddressPlan_admin_enmasse_io_v1beta2 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressPlanSpec_admin_enmasse_io_v1beta2!
    }

    type AddressPlanSpec_admin_enmasse_io_v1beta2 {
        AddressType: AddressType!
        DisplayName: String!
        LongDescription: String!
        ShortDescription: String!
        DisplayOrder: Int!
    }

    type AddressSpacePlan_admin_enmasse_io_v1beta2 {
        ObjectMeta: ObjectMeta_v1!
        Spec: AddressSpacePlanSpec_admin_enmasse_io_v1beta2!
    }

    type AddressSpacePlanSpec_admin_enmasse_io_v1beta2 {
        AddressPlans: [AddressPlan_admin_enmasse_io_v1beta2!]!
        AddressSpaceType: AddressSpaceType,
        DisplayName: String!
        LongDescription: String!
        ShortDescription: String!
        DisplayOrder: Int!
    }


    type ObjectMeta_v1 {
        Annotations: [KeyValue!]!
        Name: String!
        Namespace: String!
        ResourceVersion: String!
        CreationTimestamp: Date!
        Uid: ID!
    }

    type User_v1 {
        ObjectMeta: ObjectMeta_v1!
        Identities: [String!]!
        Groups: [String!]!
        FullName: String!
    }

    type Namespace_v1 {
        ObjectMeta: ObjectMeta_v1!
        Status: NamespaceStatus_v1!
    }

    type NamespaceStatus_v1 {
        Phase: String!
    }

    type Query {
        hello: String

        "Returns the address spaces type defined by the system (DEPRECATED)"
        addressSpaceTypes: [AddressSpaceType!]!
        "Returns the address spaces type defined by the system optionally filtereing for a single address space type"
        addressSpaceTypes_v2: [AddressSpaceType_consoleapi_enmasse_io_v1beta1!]!

        "Returns the address types defined by the system (DEPRECATED)"
        addressTypes: [AddressType!]!
        "Returns the address types defined by the system optionally filtereing for a single address space type"
        addressTypes_v2(addressSpaceType: AddressSpaceType): [AddressType_consoleapi_enmasse_io_v1beta1!]!

        "Returns the address spaces plans defined by the system optionally filtereing for a single address space type"
        addressSpacePlans(addressSpaceType: AddressSpaceType): [AddressSpacePlan_admin_enmasse_io_v1beta2!]!

        "Returns the address plans defined by the system optionally filtering those for a matching address space plan and/or address type"
        addressPlans(addressSpacePlan: String, addressType: AddressType): [AddressPlan_admin_enmasse_io_v1beta2!]!

        "Returns the authenticationServices"
        authenticationServices: [AuthenticationService_admin_enmasse_io_v1beta1!]!
        "Returns the addressSpaceSchema"
        addressSpaceSchema: [AddressSpaceSchema_enmasse_io_v1beta1!]!
        "Returns the addressSpaceSchema optionally filtering those for a matching address space plan and/or address type"
        addressSpaceSchema_v2(
          addressSpaceType: AddressSpaceType
        ): [AddressSpaceSchema_enmasse_io_v1beta1!]!

        "Returns the current logged on user"
        whoami: User_v1!
        "Returns the namespaces visible to this user"
        namespaces : [Namespace_v1!]!

        "Returns the address spaces visible to this user,  optionally filtering"
        addressSpaces(first: Int, offset: Int, filter: String, orderBy: String): AddressSpaceQueryResult_consoleapi_enmasse_io_v1beta1

        "Returns the addresses visible to this user,  optionally filtering"
        addresses(first: Int, offset: Int, filter: String, orderBy: String): AddressQueryResult_consoleapi_enmasse_io_v1beta1

        "Returns the connections visible to this user,  optionally filtering"
        connections(first: Int, offset: Int, filter: String, orderBy: String): ConnectionQueryResult_consoleapi_enmasse_io_v1beta1

        "Returns the messaging certificate chain for the address space identifed by input, PEM format, suitable to be offered as a download to the user"
        messagingCertificateChain(input: ObjectMeta_v1_Input!): String!

        "Returns the command-line that, if executed, would create the given address space"
        addressSpaceCommand(input: AddressSpace_enmasse_io_v1beta1_Input!): String!

        "Returns the command-line command, if executed, would create the given address"
        addressCommand(input: Address_enmasse_io_v1beta1_Input!): String!
    }

    #
    # Inputs Types
    #

    input ObjectMeta_v1_Input {
        Name: String!
        Namespace: String!
        ResourceVersion: String
    }

    input AddressSpaceSpec_enmasse_io_v1beta1_Input {
        Type:         String!
        Plan:         String!
    }

    input AddressSpace_enmasse_io_v1beta1_Input {
        ObjectMeta: ObjectMeta_v1_Input
        Spec: AddressSpaceSpec_enmasse_io_v1beta1_Input
    }

    input AddressSpec_enmasse_io_v1beta1_Input {
        Address:      String!
        AddressSpace: String
        Type:         String!
        Plan:         String!
        Topic:        String
    }

    input Address_enmasse_io_v1beta1_Input {
        ObjectMeta: ObjectMeta_v1_Input
        Spec: AddressSpec_enmasse_io_v1beta1_Input
    }

    type Mutation {
        createAddressSpace(input: AddressSpace_enmasse_io_v1beta1_Input!): ObjectMeta_v1!
        patchAddressSpace(input: ObjectMeta_v1_Input!, jsonPatch: String!, patchType : String!): Boolean
        deleteAddressSpace(input: ObjectMeta_v1_Input!): Boolean

        createAddress(input: Address_enmasse_io_v1beta1_Input!): ObjectMeta_v1!
        patchAddress(input: ObjectMeta_v1_Input!, jsonPatch: String!, patchType : String!): Boolean
        deleteAddress(input: ObjectMeta_v1_Input!): Boolean
        purgeAddress(input: ObjectMeta_v1_Input!): Boolean

        closeConnection(input: ObjectMeta_v1_Input!): Boolean
    }
`;

module.exports = typeDefs;
